package com.libertymutualgroup.herman.util;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigurationUtil {

    private static final String CONFIG_FILE = "properties.yml";
    private static final String ECR_POLICY_FILE = "ecr-policy.json";
    private static final String KMS_POLICY_FILE = "kms-policy.json";
    private static final String VERSION_PROPERTY_FILE = "version.properties";

    public static String getHermanConfigurationAsString(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region) {
        return getHermanConfigurationAsString(sessionCredentials, hermanLogger, null, region);
    }

    public static String getHermanConfigurationAsString(AWSCredentials sessionCredentials, HermanLogger hermanLogger, String customConfigurationBucket, Regions region) {
        try {
            String hermanConfigBucket = getConfigurationBucketName(sessionCredentials, customConfigurationBucket, region);
            hermanLogger.addLogEntry(String.format("... Using task config from S3 bucket %s: %s", hermanConfigBucket, CONFIG_FILE));

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withRegion(region)
                    .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
            S3Object fullObject = s3Client.getObject(new GetObjectRequest(hermanConfigBucket, CONFIG_FILE));
            return IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();

            String account = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
            String userId = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getUserId();
            String errorMessage = String.format("ERROR: This AWS Account is not configured for Herman! Please ensure that the appropriate configuration bucket and files are in place, or that you're using the correct AWS Account.\nRequested configuration file: %s\nAWS Account ID: %s\nAWS User ID: %s", CONFIG_FILE, account, userId);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    public static String getECRPolicyAsString(AWSCredentials sessionCredentials, HermanLogger hermanLogger, String customConfigurationBucket, Regions region) {
        try {
            String configBucket = getConfigurationBucketName(sessionCredentials, customConfigurationBucket, region);
            hermanLogger.addLogEntry(String.format("... Using ECR policy file from S3 bucket %s: %s", configBucket, ECR_POLICY_FILE));

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withRegion(region)
                    .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
            S3Object fullObject = s3Client.getObject(new GetObjectRequest(configBucket, ECR_POLICY_FILE));
            return IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new RuntimeException("Error getting ECR policy file from " + ECR_POLICY_FILE, ex);
        }
    }

    public static String getKMSPolicyAsString(AWSCredentials sessionCredentials, HermanLogger hermanLogger, String customConfigurationBucket, Regions region) {
        try {
            String configBucket = getConfigurationBucketName(sessionCredentials, customConfigurationBucket, region);
            hermanLogger.addLogEntry(String.format("... Using KMS policy file from S3 bucket %s: %s", configBucket, KMS_POLICY_FILE));

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withRegion(region)
                    .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
            S3Object fullObject = s3Client.getObject(new GetObjectRequest(configBucket, KMS_POLICY_FILE));
            return IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new RuntimeException("Error getting KMS policy file from " + KMS_POLICY_FILE, ex);
        }
    }

    private static String getConfigurationBucketName(AWSCredentials sessionCredentials, String customConfigurationBucket, Regions region)
            throws IOException {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
        String account = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();

        InputStream versionPropertiesInputStream = ConfigurationUtil.class.getClassLoader().getResourceAsStream(VERSION_PROPERTY_FILE);
        String versionPropertiesString = IOUtils.toString(versionPropertiesInputStream);
        final Properties versionProperties = new Properties();
        versionProperties.load(new StringReader(versionPropertiesString));

        String hermanConfigBucket;
        if (customConfigurationBucket != null) {
            hermanConfigBucket = customConfigurationBucket;
        } else {
            hermanConfigBucket = String.format("herman-configuration-%s-%s-%s",
                    account,
                    region.getName(),
                    versionProperties.getProperty("version").toLowerCase());
        }
        return hermanConfigBucket;
    }

    public <T> T getConfigProperties(AWSCredentials sessionCredentials, HermanLogger logger, Regions region, Class<T> propertiesClass) {
        String propertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, logger, region);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(propertiesYml, propertiesClass);
        } catch (Exception e) {
            logger.addErrorLogEntry("Error getting properties from config bucket. Continuing...", e);
        }
        return null;
    }
}
