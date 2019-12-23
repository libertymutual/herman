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
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLoggingConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketEncryptionRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.LambdaConfiguration;
import com.amazonaws.services.s3.model.NotificationConfiguration;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.TopicConfiguration;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsBroker;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.s3.S3CreateTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class S3Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Broker.class);
    private static final String S3_TEMPLATE_JSON = "s3_template.json";
    private static final String S3_TEMPLATE_YML = "s3_template.yml";

    private HermanLogger buildLogger;
    private PropertyHandler handler;
    private S3CreateTaskProperties taskProperties;
    private S3CreateContext context;

    public S3Broker(S3CreateContext context) {
        this.buildLogger = context.getLogger();
        this.handler = context.getPropertyHandler();
        this.taskProperties = context.getTaskProperties();
        this.context = context;
    }

    public BucketMeta brokerFromConfigurationFile() {
        S3InjectConfiguration configuration = getS3Configuration();
        configuration.setEncryptionOption(configuration.getEncryptionOption() == null ?
            taskProperties.getS3().getDefaultEncryption() : configuration.getEncryptionOption());

        ArrayList<HermanTag> tags = new ArrayList<>();
        if (taskProperties != null) {
            tags.add(new HermanTag(taskProperties.getSbuTagKey(), configuration.getSbu()));
            tags.add(new HermanTag(taskProperties.getOrgTagKey(), configuration.getOrg()));
            tags.add(new HermanTag(taskProperties.getAppTagKey(), configuration.getAppName()));
        }
        if (configuration.getTags() != null) {
            tags = new ArrayList<>(TagUtil.mergeTags(tags, configuration.getTags()));
        }

        // Broker KMS key if required
        if (S3EncryptionOption.KMS.equals(configuration.getEncryptionOption())
                && Boolean.TRUE.equals(configuration.getCreateBucketKey())) {
            configuration.setKmsKeyArn(brokerKms(configuration, TagUtil.hermanToMap(tags)));
        }

        // Setup up policy and tags for the bucket
        String policy = null;
        if (configuration.getPolicyName() != null) {
            FileUtil fileUtil = new FileUtil(context.getRootPath(), buildLogger);
            policy = fileUtil.findFile(configuration.getPolicyName(), false);
        }

        AmazonS3 client = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).withRegion(context.getRegion()).build();
        brokerBucket(client, configuration, tags, policy);
        updateNotificationConfiguration(configuration, client);

        buildLogger.addLogEntry("Setting bamboo.s3.brokered.name = " + configuration.getAppName());
        buildLogger.addLogEntry("Setting bamboo.s3.brokered.region = " + client.getRegionName());
        BucketMeta result = new BucketMeta();
        result.setName(configuration.getAppName());
        result.setRegion(client.getRegionName());
        return result;
    }

    private String brokerKms(S3InjectConfiguration configuration, Map<String, String> tagMap) {
        AWSKMS kmsClient = AWSKMSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).withRegion(context.getRegion()).build();

        KmsBroker kmsBroker = new KmsBroker(this.buildLogger, this.handler, this.context.getFileUtil(), this.taskProperties,
            this.context.getSessionCredentials(), null, this.context.getRegion());
        String keyArn = "";
        if (configuration.getCreateBucketKey()) {
            List<Tag> tags = tagMap.entrySet().stream().map(it ->
                    new Tag().withTagKey(it.getKey()).withTagValue(it.getValue()))
                .collect(Collectors.toList());
            String keyId = kmsBroker.brokerKey(kmsClient, configuration, tags);
            keyArn = kmsBroker.getExistingKeyArnFromId(kmsClient, keyId);
        } else {
            kmsBroker.deleteKey(kmsClient, configuration);
        }

        return keyArn;
    }

    public void brokerBucketFromEcsPush(AmazonS3 s3Client, AWSKMS kmsClient, S3Bucket bucket, String bucketPolicy, String kmsKeyId,
            EcsClusterMetadata clusterMetadata, EcsPushDefinition definition) {
        ArrayList<HermanTag> tags = new ArrayList<>();
        if (taskProperties != null) {
            tags.add(new HermanTag(taskProperties.getSbuTagKey(), clusterMetadata.getNewrelicSbuTag()));
            tags.add(new HermanTag(taskProperties.getOrgTagKey(), clusterMetadata.getNewrelicOrgTag()));
            tags.add(new HermanTag(taskProperties.getAppTagKey(), definition.getAppName()));
            tags.add(new HermanTag(taskProperties.getClusterTagKey(), clusterMetadata.getClusterId()));
        }
        if (definition.getTags() != null) {
            tags = new ArrayList<>(TagUtil.mergeTags(tags, definition.getTags()));
        }

        S3InjectConfiguration configuration = new S3InjectConfiguration();
        configuration.setAppName(bucket.getName());
        configuration.setSbu(clusterMetadata.getNewrelicSbuTag());
        configuration.setOrg(clusterMetadata.getNewrelicOrgTag());
        configuration.setEncryptionOption(bucket.getEncryptionOption() == null ?
            taskProperties.getS3().getDefaultEncryption() : bucket.getEncryptionOption());
        configuration.setSnsNotifications(bucket.getSnsNotifications());
        configuration.setLambdaNotifications(bucket.getLambdaNotifications());

        if (S3EncryptionOption.KMS.equals(configuration.getEncryptionOption()) && kmsKeyId != null) {
            String kmsKeyArn = kmsClient.describeKey(new DescribeKeyRequest().withKeyId(kmsKeyId)).getKeyMetadata().getArn();
            buildLogger.addLogEntry("KMS key arn retrieved: " + kmsKeyArn);
            configuration.setKmsKeyArn(kmsKeyArn);
        } else {
            new AwsExecException("KMS is set as the encryption option. An app KMS key ID is required.");
        }

        brokerBucket(s3Client, configuration, tags, bucketPolicy);
    }

    private void updateNotificationConfiguration(S3InjectConfiguration configuration, AmazonS3 client) {
        Map<String, NotificationConfiguration> configurationMap = new HashMap<>();
        buildLogger.addLogEntry("Updating S3 Events Configuration");
        if (configuration.getLambdaNotifications() != null) {
            buildLogger.addLogEntry("Setting Lambda notification configurations: " + configuration.getLambdaNotifications());
            configuration.getLambdaNotifications().forEach(it -> configurationMap.put(
                it.getName(),
                new LambdaConfiguration(it.getArn(), it.getEvents())));
        }
        if (configuration.getSnsNotifications() != null) {
            buildLogger.addLogEntry("Setting SNS notification configurations: " + configuration.getSnsNotifications());
            configuration.getSnsNotifications().forEach(it -> configurationMap.put(
                it.getName(),
                new TopicConfiguration(it.getArn(), it.getEvents())
            ));
        }

        client.setBucketNotificationConfiguration(new SetBucketNotificationConfigurationRequest(
            configuration.getAppName(),
            new BucketNotificationConfiguration().withNotificationConfiguration(configurationMap)));
    }

    private void brokerBucket(AmazonS3 client, S3InjectConfiguration configuration, List<HermanTag> tags, String bucketPolicy) {
        String bucketName = configuration.getAppName();
        buildLogger.addLogEntry("Deploying S3 Bucket to " + client.getRegionName());
        buildLogger.addLogEntry("Checking for existing bucket: " + bucketName);
        boolean exists = client.doesBucketExistV2(bucketName);
        if (!exists) {
            buildLogger.addLogEntry("Bucket not found, creating...");
            client.createBucket(new CreateBucketRequest(bucketName));
        } else {
            try {
                client.headBucket(new HeadBucketRequest(bucketName)).getBucketRegion();
            } catch (AmazonS3Exception ex) {
                Pattern regionPattern = Pattern.compile("The bucket is in this region: ([a-z-]+[\\d]).");
                Matcher regionMatcher = regionPattern.matcher(ex.getMessage());
                if (regionMatcher.find()) {
                    buildLogger.addLogEntry(
                        "Bucket found in different region: " + regionMatcher.group(1) + "! Skipping update...");
                    return;
                }
                throw ex;
            }
            buildLogger.addLogEntry("Existing bucket found, updating...");
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

            buildLogger.addLogEntry(websiteConfigurationString);
        }

        if (S3EncryptionOption.NONE.equals(configuration.getEncryptionOption())) {
            client.deleteBucketEncryption(new DeleteBucketEncryptionRequest().withBucketName(bucketName));
            buildLogger.addLogEntry("Encryption is disabled for bucket " + bucketName);

        } else if (S3EncryptionOption.KMS.equals(configuration.getEncryptionOption())) {
            buildLogger.addLogEntry(String.format("Enabling %s encryption for the bucket", configuration.getEncryptionOption()));
            if (configuration.getKmsKeyArn() == null) {
                throw new AwsExecException("KMS key arn is required if encryption option is set to KMS. Other options are AES256 and NONE.");
            }

            SetBucketEncryptionRequest request = new SetBucketEncryptionRequest()
                .withBucketName(bucketName)
                .withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
                    .withRules(new ServerSideEncryptionRule()
                        .withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault()
                            .withSSEAlgorithm(SSEAlgorithm.KMS)
                            .withKMSMasterKeyID(configuration.getKmsKeyArn()))));
            client.setBucketEncryption(request);
            buildLogger.addLogEntry(String.format("... KMS key: %s", configuration.getKmsKeyArn()));


        } else if (S3EncryptionOption.AES256.equals(configuration.getEncryptionOption())) {
            buildLogger.addLogEntry(String.format("Enabling %s encryption for the bucket", configuration.getEncryptionOption()));
            SetBucketEncryptionRequest request = new SetBucketEncryptionRequest()
                .withBucketName(bucketName)
                .withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
                    .withRules(new ServerSideEncryptionRule()
                        .withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault()
                            .withSSEAlgorithm(SSEAlgorithm.AES256))));
            client.setBucketEncryption(request);
        }

        if (tags != null) {
            client.setBucketTaggingConfiguration(bucketName, new BucketTaggingConfiguration().withTagSets(TagUtil.hermanToTagSet(tags)));
        }

        if (taskProperties != null && taskProperties.getLogsBucket() != null) {
            this.setBucketLoggingConfiguration(client, configuration.getAppName(), taskProperties.getLogsBucket());
        } else if (taskProperties != null && taskProperties.getS3().getDefaultLoggingBucket() != null) {
            this.setBucketLoggingConfiguration(client, configuration.getAppName(),taskProperties.getS3().getDefaultLoggingBucket());
        }
        else {
            buildLogger.addLogEntry("Disabling S3 access logging");
            client.setBucketLoggingConfiguration(new SetBucketLoggingConfigurationRequest(
                configuration.getAppName(),
                new BucketLoggingConfiguration()));
        }
        updateNotificationConfiguration(configuration, client);
    }

    private void setBucketLoggingConfiguration(AmazonS3 client, String appName, String loggingBucket) {
        buildLogger.addLogEntry(String.format("Enabling S3 access logging using logs bucket %s", loggingBucket));
        String logFilePrefix = String.format("AWSLogs/%s/s3-access/%s/%s",
            handler.lookupVariable("account.id"),
            handler.lookupVariable("aws.region"),
            appName);
        client.setBucketLoggingConfiguration(new SetBucketLoggingConfigurationRequest(
            appName,
            new BucketLoggingConfiguration(loggingBucket, logFilePrefix)));
    }

    private void setBucketPolicy(AmazonS3 client, String bucketPolicy, String bucketName) {
        if (bucketPolicy != null) {
            String fullPolicy = handler.mapInProperties(bucketPolicy);

            try {
                client.setBucketPolicy(new SetBucketPolicyRequest(bucketName, fullPolicy));
            } catch (Exception ex) {
                buildLogger.addLogEntry(
                    String.format("Error setting bucket policy for %s: Policy = %s", bucketName, fullPolicy));
                throw new AwsExecException("Error setting bucket policy", ex);
            }
        } else {
            BucketPolicy currentPolicy = client.getBucketPolicy(bucketName);
            if (currentPolicy.getPolicyText() != null) {
                buildLogger.addLogEntry("No bucket policy specified, deleting current bucket policy");
                client.deleteBucketPolicy(bucketName);
            } else {
                buildLogger.addLogEntry("No bucket policy specified and no bucket policy previously existed");
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
                    buildLogger.addLogEntry("Using " + S3_TEMPLATE_JSON);
                    template = IOUtils.toString(streamToParse);
                    isJson = true;
                }
            } else if (new File(context.getRootPath() + File.separator + S3_TEMPLATE_YML).exists()) {
                try (InputStream streamToParse = new FileInputStream(
                    new File(context.getRootPath() + File.separator + S3_TEMPLATE_YML))) {
                    buildLogger.addLogEntry("Using " + S3_TEMPLATE_YML);
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
