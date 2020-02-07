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
package com.libertymutualgroup.herman.aws.lambda;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.AddPermissionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.GetPolicyRequest;
import com.amazonaws.services.lambda.model.GetPolicyResult;
import com.amazonaws.services.lambda.model.RemovePermissionRequest;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.TagResourceRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PushType;
import com.libertymutualgroup.herman.aws.ecs.broker.cloudwatch.CloudWatchEventsBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoDBBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoDBMixIns;
import com.libertymutualgroup.herman.aws.ecs.broker.iam.IAMBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisStream;
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.sns.SnsBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.sns.SnsTopic;
import com.libertymutualgroup.herman.aws.ecs.broker.sqs.SqsBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.sqs.SqsQueue;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;

public class LambdaBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaBroker.class);
    private static final String LAMBDA_TEMPLATE_JSON = "lambda_template.json";
    private static final String LAMBDA_TEMPLATE_YML = "lambda_template.yml";
    private static final String LAMBDA_EXECUTION_PERMISSION = "lambda-execution-permission.json";

    private LambdaPushContext context;
    private HermanLogger buildLogger;

    private FileUtil fileUtil;
    private LambdaInjectConfiguration configuration;

    private AWSLambda lambdaClient;
    private AWSKMS kmsClient;
    private AmazonIdentityManagement iamClient;
    private AmazonEC2 ec2Client;
    private CommonTaskProperties taskProperties;
    private AWSCredentials credentials;
    private Regions region;
    private AmazonKinesis kinesisClient;
    private AmazonSQS sqsClient;
    private AmazonSNS snsClient;
    private AmazonDynamoDB dynamoDbClient;
    private AmazonCloudWatchEvents cloudWatchEventsClient;

    private ObjectMapper mapper = new ObjectMapper();

    public LambdaBroker(LambdaPushContext context, HermanLogger buildLogger, Regions region) {
        this.context = context;
        this.buildLogger = buildLogger;
        this.fileUtil = new FileUtil(this.context.getRootPath(), this.buildLogger);
        this.configuration = getLambdaInjectConfiguration(this.context, fileUtil);
        this.taskProperties = context.getTaskProperties();
        this.region = region;

        credentials = this.context.getSessionCredentials();
        ClientConfiguration config = BambooCredentialsHandler.getConfiguration();

        this.lambdaClient = AWSLambdaClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.iamClient = AmazonIdentityManagementClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.kmsClient = AWSKMSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.ec2Client = AmazonEC2ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.sqsClient = AmazonSQSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.snsClient = AmazonSNSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.dynamoDbClient = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.kinesisClient = AmazonKinesisClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();

        this.cloudWatchEventsClient = AmazonCloudWatchEventsClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(config)
            .withRegion(region)
            .build();
    }

    public void brokerLambda() throws IOException {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withTagKey(this.taskProperties.getSbuTagKey()).withTagValue(this.taskProperties.getSbu()));
        tags.add(new Tag().withTagKey(this.taskProperties.getOrgTagKey()).withTagValue(this.taskProperties.getOrg()));
        tags.add(
            new Tag().withTagKey(this.taskProperties.getAppTagKey()).withTagValue(this.configuration.getAppName()));

        Map<String, String> tagMap = tags.stream().collect(Collectors.toMap(Tag::getTagKey, Tag::getTagValue));
        if (this.configuration.getTags() != null) {
            tagMap.putAll(TagUtil.hermanToMap(this.configuration.getTags()));
        }
        // Create/Update custom security group if needed
        String customSecurityGroupId = getCustomSecurityGroupId(tagMap);

        String functionArn;
        try {
            GetFunctionConfigurationResult describeResult = lambdaClient.getFunctionConfiguration(
                new GetFunctionConfigurationRequest().withFunctionName(this.configuration.getFunctionName())
            );
            functionArn = describeResult.getFunctionArn();
        } catch (ResourceNotFoundException ex) {
            LOGGER.debug("Function not found: " + this.configuration.getFunctionName(), ex);
            functionArn = null;
        }

        this.buildLogger.addLogEntry("Brokering execution role with name: " + this.configuration.getFunctionName());

        String iamPolicyFileName = Optional.ofNullable(this.configuration.getIamPolicy()).orElse("iam-policy.json");
        String defaultExecutionRole = IOUtils
            .toString(this.getClass().getResourceAsStream("/iam/lambda-default-execution-policy.json"));
        String policy = this.fileUtil.findFile(iamPolicyFileName, true);
        if (policy == null) {
            policy = defaultExecutionRole;
        }

        IAMBroker iamBroker = new IAMBroker(this.buildLogger);
        Role executionRole = iamBroker
            .brokerAppRole(this.iamClient, this.configuration, policy, this.context.getBambooPropertyHandler(),
                PushType.LAMBDA, this.configuration.getTags());
        this.context.getBambooPropertyHandler().addProperty("app.iam", executionRole.getArn());

        FunctionCode functionCode;
        try {
            FileInputStream zipInputStream = fileUtil.findfileAsInputStream(this.configuration.getZipFileName());
            FileChannel zipInputChannel = zipInputStream.getChannel();
            ByteBuffer code = ByteBuffer.allocate(toIntExact(zipInputChannel.size()));
            zipInputChannel.read(code);
            code.rewind();
            functionCode = new FunctionCode().withZipFile(code);
            zipInputChannel.close();
        } catch (IOException ex) {
            buildLogger.addLogEntry("Failed to read zip file: " + this.configuration.getZipFileName());
            throw new AwsExecException(ex);
        }

        VpcConfig vpcConfig = getVpcConfig(customSecurityGroupId);
        Environment environment = getEnvironment();
        String kmsKeyArn = brokerKms(tags);
        if (functionArn == null && functionCode != null) {
            buildLogger.addLogEntry("Pushing new Lambda");

            CreateFunctionRequest createRequest = new CreateFunctionRequest()
                .withCode(functionCode)
                .withFunctionName(this.configuration.getFunctionName())
                .withHandler(this.configuration.getHandler())
                .withRole(executionRole.getArn())
                .withRuntime(this.configuration.getRuntime())
                .withTags(tagMap)
                .withTimeout(this.configuration.getTimeout())
                .withVpcConfig(vpcConfig)
                .withMemorySize(this.configuration.getMemorySize())
                .withEnvironment(environment)
                .withKMSKeyArn(kmsKeyArn);

            CreateFunctionResult result = lambdaClient.createFunction(createRequest);
            buildLogger.addLogEntry("Lambda created: " + result.getFunctionName());
        } else if (functionArn != null && functionCode != null) {
            buildLogger.addLogEntry("Lambda exists, attempting update...");

            UpdateFunctionCodeRequest updateFunctionCode = new UpdateFunctionCodeRequest()
                .withFunctionName(this.configuration.getFunctionName())
                .withZipFile(functionCode.getZipFile());

            UpdateFunctionConfigurationRequest updateFunctionConfiguration = new UpdateFunctionConfigurationRequest()
                .withFunctionName(this.configuration.getFunctionName())
                .withHandler(this.configuration.getHandler())
                .withRole(executionRole.getArn())
                .withRuntime(this.configuration.getRuntime())
                .withTimeout(this.configuration.getTimeout())
                .withVpcConfig(vpcConfig)
                .withMemorySize(this.configuration.getMemorySize())
                .withEnvironment(environment)
                .withKMSKeyArn(kmsKeyArn);

            TagResourceRequest tagRequest = new TagResourceRequest()
                .withResource(functionArn)
                .withTags(tagMap);

            buildLogger.addLogEntry("... Updating with configuration: " + updateFunctionConfiguration);
            lambdaClient.updateFunctionCode(updateFunctionCode);
            UpdateFunctionConfigurationResult configurationResult = lambdaClient
                .updateFunctionConfiguration(updateFunctionConfiguration);
            lambdaClient.tagResource(tagRequest);

            buildLogger.addLogEntry("Lambda updated: " + configurationResult.getFunctionName());
        }

        try {
            buildLogger.addLogEntry("... Resetting execution permissions");
            List<String> sidsToRemove = getExistingExecutionPermissionSids();
            if (!sidsToRemove.isEmpty()) {
                for (String sid: sidsToRemove) {
                    RemovePermissionRequest removeExistingPerms = new RemovePermissionRequest()
                        .withFunctionName(this.configuration.getFunctionName())
                        .withStatementId(sid);
                    lambdaClient.removePermission(removeExistingPerms);
                }
            }
        } catch (ResourceNotFoundException ex) {
            LOGGER.debug("Function not found: " + this.configuration.getFunctionName(), ex);
            buildLogger.addLogEntry("Unable to reset permissions, skipping...");
        }

        List<LambdaPermission> permissions = getExecutionPermission();
        if (!permissions.isEmpty()) {
            buildLogger.addLogEntry("Adding new execution permission");

            for (int i = 0; i < permissions.size(); i++) {
                LambdaPermission permission = permissions.get(i);
                if (permission != null) {
                    String sid = Optional.ofNullable(permission.getSid())
                        .orElse(this.configuration.getFunctionName() + "-InvokePermission-" + i);
                    AddPermissionRequest permissionRequest = new AddPermissionRequest()
                        .withFunctionName(this.configuration.getFunctionName())
                        .withAction(permission.getAction())
                        .withPrincipal(permission.getPrincipal())
                        .withEventSourceToken(permission.getEventSourceToken())
                        .withStatementId(sid)
                        .withQualifier(permission.getQualifier()).withSourceArn(permission.getSourceArn());

                    lambdaClient.addPermission(permissionRequest);
                }
            }
        }

        buildLogger.addLogEntry("Lambda pushed");
        GetFunctionResult output = lambdaClient
            .getFunction(new GetFunctionRequest().withFunctionName(this.configuration.getFunctionName()));
        if (this.configuration.getUseKms()) {
            buildLogger.addLogEntry("Pushed lambda with kms key " + output.getConfiguration().getKMSKeyArn());
        }
        buildLogger.addLogEntry(output.getConfiguration().toString());

        brokerSns(this.configuration);
        brokerSqs(this.configuration);
        brokerKinesisStream(this.configuration);
        brokerDynamoDB(this.configuration);
        brokerScheduledRule(this.configuration, output);
    }

    private Environment getEnvironment() {
        HashMap<String, String> environmentMap = new HashMap<>();
        if (this.configuration.getEnvironment() != null) {
            for (KeyValuePair envVar: this.configuration.getEnvironment()) {
                environmentMap.put(envVar.getName(), envVar.getValue());
            }
        }
        return new Environment().withVariables(environmentMap);
    }

    private VpcConfig getVpcConfig(String customSecurityGroupId) {
        VpcConfig vpcConfig = new VpcConfig();
        if (this.configuration.getSubnetIds() != null) {
            vpcConfig.setSubnetIds(this.configuration.getSubnetIds());
        }
        if (customSecurityGroupId != null) {
            vpcConfig.setSecurityGroupIds(Arrays.asList(customSecurityGroupId));
        } else if (this.configuration.getSecurityGroupIds() != null) {
            vpcConfig.setSecurityGroupIds(this.configuration.getSecurityGroupIds());
        }
        this.buildLogger.addLogEntry("VPC Configuration: " + vpcConfig);
        return vpcConfig;
    }

    private String getCustomSecurityGroupId(Map<String, String> tagMap) {
        // Create a custom security group if needed
        String customSecurityGroupId = null;
        if (this.configuration.getCustomSecurityGroup() != null) {
            try {
                DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2Client
                    .describeSecurityGroups(new DescribeSecurityGroupsRequest()
                        .withFilters(
                            new Filter("vpc-id").withValues(this.configuration.getCustomSecurityGroup().getVpcId()),
                            new Filter("group-name").withValues(this.configuration.getFunctionName())));
                if (describeSecurityGroupsResult.getSecurityGroups().size() == 1) {
                    customSecurityGroupId = describeSecurityGroupsResult.getSecurityGroups().get(0).getGroupId();
                    this.buildLogger.addLogEntry("Security group found: " + customSecurityGroupId);
                } else {
                    this.buildLogger.addLogEntry(
                        "Creating security group with name " + this.configuration.getFunctionName());

                    CreateSecurityGroupResult createSecurityGroupResult = ec2Client.createSecurityGroup(
                        new CreateSecurityGroupRequest().withGroupName(this.configuration.getFunctionName())
                            .withDescription(
                                "Security group for the " + this.configuration.getFunctionName() + " Lambda function")
                            .withVpcId(this.configuration.getCustomSecurityGroup().getVpcId()));

                    customSecurityGroupId = createSecurityGroupResult.getGroupId();
                    this.buildLogger.addLogEntry("... Security group created. ID = " + customSecurityGroupId);
                }
            } catch (AmazonEC2Exception ex) {
                throw new AwsExecException("Error getting security group", ex);
            }

            this.buildLogger.addLogEntry("... Updating tags");
            ec2Client.createTags(new CreateTagsRequest()
                .withResources(customSecurityGroupId)
                .withTags(tagMap.entrySet().stream()
                    .map(entry -> new com.amazonaws.services.ec2.model.Tag().withKey(entry.getKey())
                        .withValue(entry.getValue()))
                    .collect(Collectors.toList())));
        }
        return customSecurityGroupId;
    }

    private List<String> getExistingExecutionPermissionSids() {
        final GetPolicyResult executionPolicyResult = lambdaClient
            .getPolicy(new GetPolicyRequest().withFunctionName(this.configuration.getFunctionName()));
        if (executionPolicyResult == null) {
            buildLogger.addLogEntry("Unable to find existing execution policy");
            return Collections.emptyList();
        }
        try {
            final JsonNode executionStatements = this.mapper.readTree(executionPolicyResult.getPolicy())
                .get("Statement");
            final TypeReference<List<JsonNode>> listRef = new TypeReference<List<JsonNode>>() {};
            List<JsonNode> statements = mapper.readValue(executionStatements.toString(), listRef);
            return statements.stream().map(it -> it.get("Sid").textValue()).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.debug("Unable to parse existing execution policy", e);
            buildLogger.addLogEntry("Unable to parse existing execution policy");
            return Collections.emptyList();
        }
    }

    private List<LambdaPermission> getExecutionPermission() {
        try {
            if (this.fileUtil.fileExists(LAMBDA_EXECUTION_PERMISSION)) {
                this.buildLogger
                    .addLogEntry(String.format("... Using %s for execution permissions", LAMBDA_EXECUTION_PERMISSION));
                String template = fileUtil.findFile(LAMBDA_EXECUTION_PERMISSION, false);
                String mappedPermissionString = this.context.getBambooPropertyHandler().mapInProperties(template);
                final TypeReference<List<LambdaPermission>> listRef = new TypeReference<List<LambdaPermission>>() {};
                try {
                    return this.mapper.readValue(mappedPermissionString, listRef);
                } catch (JsonMappingException jsonEx) {
                    buildLogger.addErrorLogEntry(
                        "DEPRECATION WARNING: Singleton permissions are deprecated, please pass execution permissions as an array.");
                    LambdaPermission singletonPermission = this.mapper
                        .readValue(mappedPermissionString, LambdaPermission.class);
                    return Collections.singletonList(singletonPermission);
                } catch (Exception ex) {
                    buildLogger.addErrorLogEntry("Error parsing permissions", ex);
                    throw new AwsExecException(ex);
                }
            }
        } catch (IOException ex) {
            buildLogger.addErrorLogEntry("Error getting execution permissions", ex);
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private String brokerKms(List<Tag> tags) {
        KmsBroker kmsBroker = new KmsBroker(this.context.getLogger(), this.context.getBambooPropertyHandler(),
            this.fileUtil, this.context.getTaskProperties(), credentials, null, this.region);
        String keyArn = "";
        if (this.configuration.getUseKms()) {
            String keyId = kmsBroker.brokerKey(kmsClient, this.configuration, tags);
            keyArn = kmsBroker.getExistingKeyArnFromId(kmsClient, keyId);
        } else {
            kmsBroker.deleteKey(kmsClient, this.configuration);
        }

        return keyArn;
    }

    private LambdaInjectConfiguration getLambdaInjectConfiguration(LambdaPushContext context, FileUtil fileUtil) {
        LambdaInjectConfiguration definition;
        String template;

        boolean isJson;

        try {
            if (new File(context.getRootPath() + File.separator + LAMBDA_TEMPLATE_JSON).exists()) {
                buildLogger.addLogEntry("... Using " + LAMBDA_TEMPLATE_JSON);
                template = fileUtil.findFile(LAMBDA_TEMPLATE_JSON, false);
                isJson = true;
            } else if (new File(context.getRootPath() + File.separator + LAMBDA_TEMPLATE_YML).exists()) {
                buildLogger.addLogEntry("... Using " + LAMBDA_TEMPLATE_YML);
                template = fileUtil.findFile(LAMBDA_TEMPLATE_YML, false);
                isJson = false;
            } else {
                throw new AwsExecException("No Lambda template provided!");
            }

            ObjectMapper mapper = isJson ? new ObjectMapper() : new ObjectMapper(new YAMLFactory());
            mapper.addMixIn(KeySchemaElement.class, DynamoDBMixIns.class);
            mapper.addMixIn(StreamSpecification.class, DynamoDBMixIns.class);
            mapper.addMixIn(AttributeDefinition.class, DynamoDBMixIns.class);
            mapper.addMixIn(Projection.class, DynamoDBMixIns.class);

            definition = mapper.readValue(this.context.getBambooPropertyHandler().mapInProperties(template),
                LambdaInjectConfiguration.class);
        } catch (FileNotFoundException e1) {
            buildLogger.addErrorLogEntry("No template found: " + LAMBDA_TEMPLATE_JSON, e1);
            throw new AwsExecException("No template found at " + LAMBDA_TEMPLATE_JSON);
        } catch (IOException e1) {
            throw new AwsExecException(e1);
        }

        return definition;
    }

    private void brokerSqs(LambdaInjectConfiguration definition) {
        if (definition.getQueues() != null) {
            SqsBroker sqsBroker = new SqsBroker(this.buildLogger, this.context.getBambooPropertyHandler());
            this.buildLogger.addLogEntry("Brokering SQS queues...");
            for (SqsQueue queue : definition.getQueues()) {
                if (queue.getPolicyName() != null) {
                    String policy = fileUtil.findFile(queue.getPolicyName(), false);
                    sqsBroker.brokerQueue(sqsClient, queue, policy, definition.getTags());
                } else {
                    sqsBroker.brokerQueue(sqsClient, queue, null, definition.getTags());
                }
            }
        }
    }

    private void brokerSns(LambdaInjectConfiguration definition) {
        if (definition.getTopics() != null) {
            SnsBroker snsBroker = new SnsBroker(this.buildLogger, this.context.getBambooPropertyHandler());
            this.buildLogger.addLogEntry("Brokering SNS topics...");
            for (SnsTopic topic : definition.getTopics()) {
                if (topic.getPolicyName() != null) {
                    String policy = fileUtil.findFile(topic.getPolicyName(), false);
                    snsBroker.brokerTopic(snsClient, topic, policy);
                } else {
                    snsBroker.brokerTopic(snsClient, topic, null);
                }
            }
        }

    }

    private void brokerKinesisStream(LambdaInjectConfiguration definition) {
        KinesisBroker kinesisBroker = new KinesisBroker(this.buildLogger, kinesisClient, definition, taskProperties);

        // delete any streams tied to this app that are no longer specified in the PushDefinition
        kinesisBroker.checkStreamsToBeDeleted();

        if (definition.getStreams() != null) {
            this.buildLogger.addLogEntry("Brokering Kinesis streams...");
            for (KinesisStream stream : definition.getStreams()) {
                kinesisBroker.brokerStream(stream);
            }
        }
    }

    private void brokerDynamoDB(LambdaInjectConfiguration definition) {
        if (definition.getDynamoDBTables() != null) {
            DynamoDBBroker dynamoDBBroker = new DynamoDBBroker(this.buildLogger, definition);
            this.buildLogger.addLogEntry("Brokering DynamoDB tables");
            dynamoDBBroker.createDynamoDBTables(dynamoDbClient);
        }
    }

    private void brokerScheduledRule(LambdaInjectConfiguration configuration, GetFunctionResult output) {
        CloudWatchEventsBroker cloudWatchEventsBroker = new CloudWatchEventsBroker(this.buildLogger, this.cloudWatchEventsClient);
        cloudWatchEventsBroker.brokerScheduledRule(configuration, output);
    }
}
