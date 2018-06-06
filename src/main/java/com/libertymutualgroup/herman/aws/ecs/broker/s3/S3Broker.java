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
package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.TagSet;
import com.amazonaws.util.IOUtils;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Broker.class);
    private static final String S3_TEMPLATE_JSON = "s3_template.json";
    private static final String S3_TEMPLATE_YML = "s3_template.yml";

    private BuildLogger buildLogger;
    private PropertyHandler handler;
    private CommonTaskProperties taskProperties;
    private S3CreateContext context;

    public S3Broker(S3CreateContext context) {
        this.buildLogger = context.getLogger();
        this.handler = context.getBambooPropertyHandler();
        this.taskProperties = context.getTaskProperties();
        this.context = context;
    }

    public BucketMeta brokerFromConfigurationFile() {
        S3InjectConfiguration configuration = getS3Configuration();
        AmazonS3 client = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(CredentialsHandler.getConfiguration()).withRegion(context.getRegion()).build();

        TagSet tags = new TagSet();
        tags.setTag(taskProperties.getSbuTagKey(), configuration.getSbu());
        tags.setTag(taskProperties.getOrgTagKey(), configuration.getOrg());
        tags.setTag(taskProperties.getAppTagKey(), configuration.getAppName());
        String policy = null;

        if (configuration.getPolicyName() != null) {
            FileUtil fileUtil = new FileUtil(context.getRootPath(), buildLogger);
            policy = fileUtil.findFile(configuration.getPolicyName(), false);
        }

        brokerBucket(client, configuration, tags, policy);
        buildLogger.addBuildLogEntry("Setting bamboo.s3.brokered.name = " + configuration.getAppName());
        buildLogger.addBuildLogEntry("Setting bamboo.s3.brokered.region = " + client.getRegionName());
        BucketMeta result = new BucketMeta();
        result.setName(configuration.getAppName());
        result.setRegion(client.getRegionName());
        return result;
    }

    public void brokerBucketFromEcsPush(AmazonS3 client, S3Bucket bucket, String bucketPolicy,
        EcsClusterMetadata clusterMetadata, EcsPushDefinition definition) {
        TagSet tags = new TagSet();
        tags.setTag(taskProperties.getSbuTagKey(), clusterMetadata.getNewrelicSbuTag());
        tags.setTag(taskProperties.getOrgTagKey(), clusterMetadata.getNewrelicOrgTag());
        tags.setTag(taskProperties.getAppTagKey(), definition.getAppName());
        tags.setTag(taskProperties.getClusterTagKey(), clusterMetadata.getClusterId());
        S3InjectConfiguration configuration = new S3InjectConfiguration();
        configuration.setAppName(bucket.getName());
        configuration.setSbu(clusterMetadata.getNewrelicSbuTag());
        configuration.setOrg(clusterMetadata.getNewrelicOrgTag());

        brokerBucket(client, configuration, tags, bucketPolicy);
    }

    private void brokerBucket(AmazonS3 client, S3InjectConfiguration configuration, TagSet tags, String bucketPolicy) {
        String bucketName = configuration.getAppName();
        buildLogger.addBuildLogEntry("Deploying S3 Bucket to " + client.getRegionName());
        buildLogger.addBuildLogEntry("Checking for existing bucket: " + bucketName);
        boolean exists = client.doesBucketExistV2(bucketName);
        if (!exists) {
            buildLogger.addBuildLogEntry("Bucket not found, creating...");
            client.createBucket(new CreateBucketRequest(bucketName));
        } else {
            try {
                client.headBucket(new HeadBucketRequest(bucketName)).getBucketRegion();
            } catch (AmazonS3Exception ex) {
                Pattern regionPattern = Pattern.compile("The bucket is in this region: ([a-z-]+[\\d]).");
                Matcher regionMatcher = regionPattern.matcher(ex.getMessage());
                if (regionMatcher.find()) {
                    buildLogger.addBuildLogEntry(
                        "Bucket found in different region: " + regionMatcher.group(1) + "! Skipping update...");
                    return;
                }
                throw ex;
            }
            buildLogger.addBuildLogEntry("Existing bucket found, updating...");
        }

        setBucketPolicy(client, bucketPolicy, bucketName);

        if (configuration.getWebsite()) {
            client.setBucketWebsiteConfiguration(bucketName,
                new BucketWebsiteConfiguration(configuration.getIndexFile(), configuration.getErrorFile()));
            BucketWebsiteConfiguration websiteConfiguration = client.getBucketWebsiteConfiguration(bucketName);
            client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
            String bucketWebsiteUrl =
                "http://" + bucketName + ".s3-website-" + client.getRegionName() + ".amazonaws.com";
            String websiteConfigurationString = "Configured " + bucketName + " as website using index document: " +
                websiteConfiguration.getIndexDocumentSuffix() + " and error document: " + websiteConfiguration
                .getErrorDocument()
                + " at URL: " + bucketWebsiteUrl;

            buildLogger.addBuildLogEntry(websiteConfigurationString);
        } else {
            S3BucketEncryption bucketEncryption = new S3BucketEncryption(bucketName, exists, client, buildLogger);
            try {
                bucketEncryption.ensureEncryption();
            } catch (Exception ex) {
                buildLogger
                    .addBuildLogEntry(String.format("Error enabling bucket default encryption for %s", bucketName));
                throw new AwsExecException("Error enabling bucket default encryption", ex);
            }
        }

        client.setBucketTaggingConfiguration(bucketName, new BucketTaggingConfiguration().withTagSets(tags));
    }

    private void setBucketPolicy(AmazonS3 client, String bucketPolicy, String bucketName) {
        if (bucketPolicy != null) {
            String fullPolicy = handler.mapInProperties(bucketPolicy);

            try {
                client.setBucketPolicy(new SetBucketPolicyRequest(bucketName, fullPolicy));
            } catch (Exception ex) {
                buildLogger.addBuildLogEntry(
                    String.format("Error setting bucket policy for %s: Policy = %s", bucketName, fullPolicy));
                throw new AwsExecException("Error setting bucket policy", ex);
            }
        } else {
            BucketPolicy currentPolicy = client.getBucketPolicy(bucketName);
            if (currentPolicy.getPolicyText() != null) {
                buildLogger.addBuildLogEntry("No bucket policy specified, deleting current bucket policy");
                client.deleteBucketPolicy(bucketName);
            } else {
                buildLogger.addBuildLogEntry("No bucket policy specified and no bucket policy previously existed");
            }
        }
    }

    private S3InjectConfiguration getS3Configuration() {
        S3InjectConfiguration definition;

        boolean isJson;
        String template;
        try {
            if (new File(context.getRootPath() + File.separator + S3_TEMPLATE_JSON).exists()) {
                try (InputStream streamToParse = new FileInputStream(
                    new File(context.getRootPath() + File.separator + S3_TEMPLATE_JSON))) {
                    buildLogger.addBuildLogEntry("Using " + S3_TEMPLATE_JSON);
                    template = IOUtils.toString(streamToParse);
                    isJson = true;
                }
            } else if (new File(context.getRootPath() + File.separator + S3_TEMPLATE_YML).exists()) {
                try (InputStream streamToParse = new FileInputStream(
                    new File(context.getRootPath() + File.separator + S3_TEMPLATE_YML))) {
                    buildLogger.addBuildLogEntry("Using " + S3_TEMPLATE_YML);
                    template = IOUtils.toString(streamToParse);
                    isJson = false;
                }
            } else {
                throw new AwsExecException("No S3 template provided!");
            }

            ObjectMapper mapper = isJson ? new ObjectMapper() : new ObjectMapper(new YAMLFactory());
            definition = mapper.readValue(handler.mapInProperties(template), S3InjectConfiguration.class);
        } catch (FileNotFoundException e1) {
            LOGGER.debug("No template found: " + S3_TEMPLATE_JSON, e1);
            throw new AwsExecException("No template found at " + S3_TEMPLATE_JSON);
        } catch (IOException e1) {
            throw new AwsExecException(e1);
        }

        return definition;
    }

}
