/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.aws.cft;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AlreadyExistsException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.cft.CftPushTaskProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CftPush { ;
    private static final Logger LOGGER = LoggerFactory.getLogger(CftPush.class);
    private static final String BUILD_NUMBER = "buildNumber";
    private static final String MAVEN_GROUP = "maven.groupId";
    private static final String MAVEN_ART = "maven.artifactId";
    private static final String MAVEN_VERS = "maven.version";
    private static final int RANDOM_PASSWORD_LENGTH = 20;
    private Properties props = new Properties();
    private Properties output = new Properties();
    private HermanLogger buildLogger;
    private CftPushContext taskContext;
    private AmazonCloudFormation cftClient;
    private AWSLambda lambdaClient;
    private StackUtils stackUtils;
    private Regions region;
    private PropertyHandler propertyHandler;
    private CftPushTaskProperties taskProperties;

    public CftPush(CftPushContext taskContext) {
        this.taskContext = taskContext;
        this.buildLogger = taskContext.getLogger();
        this.propertyHandler = taskContext.getPropertyHandler();
        this.region = taskContext.getRegion();

        cftClient = AmazonCloudFormationClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(taskContext.getSessionCredentials())).withClientConfiguration(taskContext.getAwsClientConfig())
            .withRegion(region).build();

        lambdaClient = AWSLambdaClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(taskContext.getSessionCredentials())).withClientConfiguration(taskContext.getAwsClientConfig())
            .withRegion(region).build();

        this.stackUtils = new StackUtils(this.cftClient, this.buildLogger);

        this.taskProperties = taskContext.getTaskProperties();
    }

    public void push(String stackName, String template) {
        this.push(stackName, template, null);
    }

    public void push(String stackName, String template, List<HermanTag> stackTags) {

        // Input data outside of CFT
        String env = taskContext.getEnvName();

        if (!StringUtils.isEmpty(this.taskProperties.getCftPushVariableBrokerLambda())) {
            buildLogger.addLogEntry(
                "Getting CFT variables from Lambda: " + this.taskProperties.getCftPushVariableBrokerLambda());
            introspectEnvironment();
        }
        injectBambooContext();
        importPropFiles(env);

        if (this.taskContext.getTemplateParameters() != null) {
            props.putAll(this.taskContext.getTemplateParameters());
        }

        if (!stackName.contains(region.getName())) {
            stackName = stackName + "-" + region.getName();
        }

        createStack(stackName, template, stackTags);

        buildLogger.addLogEntry("Stack triggered: " + stackName);
        this.stackUtils.waitForCompletion(stackName);
        outputStack(stackName);

        try (OutputStream fileOut = new FileOutputStream(taskContext.getRootPath() + File.separator + "stackoutput.properties")) {
            output.store(fileOut, null);
        } catch (IOException e) {
            throw new AwsExecException(e);
        }
    }

    private void importPropFiles(String env) {
        File stackOut = new File(taskContext.getRootPath() + File.separator + "stackoutput.properties");

        try (FileReader stackRead = new FileReader(stackOut)) {
            if (stackOut.exists()) {
                props.load(stackRead);
                buildLogger.addLogEntry("Loaded stackoutput.properties");
            }
        } catch (IOException e) {
            LOGGER.debug("No stackoutput.properties", e);
            buildLogger.addLogEntry("No stackoutput.properties");
        }

        String root = taskContext.getRootPath();
        File envProps = new File(root + File.separator + env + ".properties");
        try (FileReader envFile = new FileReader(envProps);) {
            // load second to allow env to override
            if (envProps.exists()) {
                props.load(envFile);
                buildLogger.addLogEntry("Loaded " + envProps.getName());
                buildLogger.addLogEntry("Props: " + props.toString());
            }
        } catch (IOException e) {
            LOGGER.debug("Property file not found for env: " + env, e);
            buildLogger.addLogEntry("No " + env + ".properties");
        }
    }

    private void injectBambooContext() {
        Properties bambooContext = this.propertyHandler.lookupProperties(BUILD_NUMBER, MAVEN_GROUP, MAVEN_ART,
            MAVEN_VERS);

        String randomPass = RandomStringUtils.randomAlphanumeric(RANDOM_PASSWORD_LENGTH);
        props.put("RandomPassword", randomPass);

        String build = "BUILD" + bambooContext.getProperty(BUILD_NUMBER);
        props.put("BuildId", build);

        String art = bambooContext.getProperty(MAVEN_ART);
        if (art != null) {
            props.put("ArtifactId", art);
        }

        String versionId = bambooContext.getProperty("maven.versionId");
        if (versionId != null) {
            props.put("Version", versionId);
        }

        String deployEnvironment = taskContext.getEnvName();
        if (deployEnvironment != null) {
            props.put("DeployEnvironment", deployEnvironment);
        }

    }

    private void createStack(String name, String template, List<HermanTag> stackTags) {
        List<Parameter> parameters = convertPropsToCftParams(template);

        String deployEnvironment = taskContext.getEnvName();

        List<HermanTag> defaultTags = new ArrayList<>();
        defaultTags.add(new HermanTag().withKey("Name").withValue(name));
        defaultTags.add(new HermanTag().withKey(this.taskProperties.getAppTagKey()).withValue(name));
        defaultTags.add(new HermanTag().withKey(this.taskProperties.getAppTagKey() + "_uid").withValue("app-e312c4299a"));
        defaultTags.add(new HermanTag().withKey(this.taskProperties.getAppTagKey() + "_env").withValue(deployEnvironment));
        defaultTags.add(new HermanTag().withKey(this.taskProperties.getSbuTagKey()).withValue(this.taskProperties.getSbu()));

        Properties bambooContext = this.propertyHandler.lookupProperties(BUILD_NUMBER, MAVEN_GROUP, MAVEN_ART,
            MAVEN_VERS);

        String artifactId = bambooContext.getProperty(MAVEN_ART);
        if (StringUtils.isNotEmpty(artifactId)) {
            defaultTags.add(new HermanTag().withKey(this.taskProperties.getCompany() + "_gav")
                .withValue(bambooContext.getProperty(MAVEN_GROUP) + ":"
                    + bambooContext.getProperty(MAVEN_ART) + ":"
                    + bambooContext.getProperty(MAVEN_VERS)));
        }

        List<HermanTag> mergedTags = TagUtil.mergeTags(defaultTags, stackTags);
        List<Tag> tags = TagUtil.hermanToCftTags(mergedTags);

        try {
            CreateStackRequest createStackRequest = new CreateStackRequest().withCapabilities("CAPABILITY_IAM")
                .withCapabilities("CAPABILITY_NAMED_IAM").withStackName(name).withTemplateBody(template)
                .withTags(tags).withParameters(parameters);

            cftClient.createStack(createStackRequest);
        } catch (AlreadyExistsException e) {
            LOGGER.debug("Stack already exists: " + name, e);

            UpdateStackRequest updateStackRequest = new UpdateStackRequest().withCapabilities("CAPABILITY_IAM")
                .withCapabilities("CAPABILITY_NAMED_IAM").withStackName(name).withTemplateBody(template)
                .withParameters(parameters).withTags(tags);
            try {
                cftClient.updateStack(updateStackRequest);
            } catch (AmazonCloudFormationException noUpdateException) {
                LOGGER.debug("Stack has no updates: " + name, noUpdateException);

                if (noUpdateException.getMessage().contains("No updates are to be performed")) {
                    buildLogger.addLogEntry("No CFT Updates to apply, skipping CFT Push...");
                } else {
                    buildLogger.addLogEntry(noUpdateException.toString());
                    throw new AwsExecException();
                }
            } catch (AmazonServiceException ase) {
                LOGGER.debug("UpdateStackRequest threw an exception for " + name, ase);

                buildLogger.addLogEntry(ase.toString());
                throw new AwsExecException();
            }
        }

    }


    private List<Parameter> convertPropsToCftParams(String template) {
        List<Parameter> parameters = new ArrayList<>();
        for (Object key: props.keySet()) {
            if (template.contains((String) key)) {
                parameters.add(new Parameter().withParameterKey((String) key)
                    .withParameterValue(props.getProperty((String) key)));
            }
        }
        return parameters;
    }

    private void introspectEnvironment() {
        InvokeRequest cftVariableBrokerReq = new InvokeRequest()
            .withFunctionName(this.taskProperties.getCftPushVariableBrokerLambda())
            .withInvocationType(InvocationType.RequestResponse)
            .withPayload("\"" + region.getName().toLowerCase() + "\"");

        String variableJson = new String(this.lambdaClient.invoke(cftVariableBrokerReq).getPayload().array(),
            Charset.forName("UTF-8"));
        Map<String, String> variables;
        try {
            variables = new ObjectMapper().readValue(variableJson, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            this.buildLogger.addLogEntry(e.getMessage());
            this.buildLogger.addLogEntry("Unable to parse variables from " + variableJson);
            throw new AwsExecException(e.getMessage(), e);
        }

        for (Map.Entry<String, String> entry: variables.entrySet()) {
            buildLogger.addLogEntry("Injecting " + entry.getKey() + " = " + entry.getValue());
            props.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @param stackName
     */
    private void outputStack(String stackName) {

        DescribeStackResourcesRequest req = new DescribeStackResourcesRequest();
        req.setStackName(stackName);

        DescribeStackResourcesResult res = cftClient.describeStackResources(req);

        for (StackResource r: res.getStackResources()) {
            buildLogger.addLogEntry(r.getPhysicalResourceId());
            buildLogger.addLogEntry(r.getResourceType());
            output.put("aws.stack." + r.getLogicalResourceId(), r.getPhysicalResourceId());
        }

        List<String> resources = new ArrayList<>();
        for (StackResource r: res.getStackResources()) {
            if ("AWS::ECS::TaskDefinition".equals(r.getResourceType())) {
                String id = r.getPhysicalResourceId();
                String task = id.split("/")[1];
                task = task.replace(":", "-");
                resources.add(task);

            }
        }

    }



}
