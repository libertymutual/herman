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
package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.OptionGroup;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.Tag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.ecs.EcsPush;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.DateUtil;
import com.libertymutualgroup.herman.util.FileUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RdsBroker {

    public static final String AURORA_ENGINE = "aurora";
    public static final String POSTGRES_ENGINE = "postgres";
    public static final String MYSQL_ENGINE = "mysql";
    public static final String CIPHER_PREFIX = "{cipher}";
    private static final int MYSQL_MAXLENGTH = 32;
    static int pollingIntervalMs = 10000;
    private AmazonRDS client;
    private AWSKMS kmsClient;
    private String targetKeyId;
    private EcsPushDefinition definition;
    private EcsClusterMetadata clusterMetadata;
    private HermanLogger logger;
    private PropertyHandler propertyHandler;
    private EcsPushContext pushContext;
    private EcsPushFactory pushFactory;
    private FileUtil fileUtil;

    public RdsBroker(EcsPushContext pushContext, AmazonRDS client, AWSKMS kmsClient, String targetKeyId,
        EcsPushDefinition definition,
        EcsClusterMetadata clusterMetadata, EcsPushFactory pushFactory, FileUtil fileUtil) {
        this.logger = pushContext.getLogger();
        this.propertyHandler = pushContext.getPropertyHandler();
        this.client = client;
        this.kmsClient = kmsClient;
        this.targetKeyId = targetKeyId;
        this.definition = definition;
        this.clusterMetadata = clusterMetadata;
        this.pushContext = pushContext;
        this.pushFactory = pushFactory;
        this.fileUtil = fileUtil;
    }

    public RdsInstance brokerDb() {
        RdsInstance rds = definition.getDatabase();
        rds.setDefaults();
        String instanceId = rds.getDBInstanceIdentifier() != null ? rds.getDBInstanceIdentifier()
            : definition.getAppName();
        String masterUserPassword = this.generateRandomPassword();
        Boolean staticPassword = rds.getAppEncryptedPassword() != null || rds.getAdminEncryptedPassword() != null;
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withKey(this.pushContext.getTaskProperties().getSbuTagKey())
            .withValue(clusterMetadata.getNewrelicSbuTag()));
        tags.add(new Tag().withKey(this.pushContext.getTaskProperties().getOrgTagKey())
            .withValue(clusterMetadata.getNewrelicOrgTag()));
        tags.add(
            new Tag().withKey(this.pushContext.getTaskProperties().getAppTagKey()).withValue(definition.getAppName()));
        tags.add(new Tag().withKey(this.pushContext.getTaskProperties().getClusterTagKey())
            .withValue(clusterMetadata.getClusterId()));
        if (definition.getNotificationWebhook() != null) {
            tags.add(new Tag().withKey("NotificationWebhook").withValue(definition.getNotificationWebhook()));
        }

        String encryptedPassword;
        RdsClient rdsClient;

        if (rds.getEngine().contains(AURORA_ENGINE)) {
            rdsClient = new AuroraClient(client, rds, clusterMetadata, tags, logger);
        } else if (rds.getEngine().contains("oracle")) {
            rdsClient = new OracleClient(client, rds, clusterMetadata, tags, logger);
        } else {
            rdsClient = new StandardRdsClient(client, rds, clusterMetadata, tags, logger);
        }

        Boolean newDb = !rdsClient.dbExists(instanceId);

        if (!newDb) {
            logger.addLogEntry("Finding existing RDS instance");
            logger.addLogEntry("... RDS instance found: " + instanceId);

            if (rds.getPreDeployBackup()) {
                rdsClient.waitForAvailableStatus(instanceId);

                String snapshotId = instanceId + "-snapshot-" + DateUtil.getDateAsString(new DateTime());
                logger.addLogEntry("Creating snapshot " + snapshotId);
                rdsClient.createSnapshot(instanceId, snapshotId);
            }

            rdsClient.waitForAvailableStatus(instanceId);
            if (rds.getFullUpdate()) {
                logger.addLogEntry("Performing full update on " + instanceId);

                rdsClient.runFullUpdate(instanceId, masterUserPassword);

            } else if (!rds.getIAMDatabaseAuthenticationEnabled() && !staticPassword) {
                logger.addLogEntry("Updating master password for " + instanceId);
                rdsClient.updateMasterPassword(instanceId, masterUserPassword);
            }
        } else {
            logger.addLogEntry("Creating new RDS instance, this will take about ten minutes.");
            rdsClient.createNewDb(instanceId, masterUserPassword);
        }

        rdsClient.waitForAvailableStatus(instanceId);
        rds.setEndpoint(rdsClient.getEndpoint(instanceId));
        rds.setDbiResourceId(rdsClient.getDbiResourceId(instanceId));

        if (newDb || rds.getFullUpdate() || (!rds.getIAMDatabaseAuthenticationEnabled() && !staticPassword)) {
            encryptedPassword = rds.getEncryptedPassword() != null ? rds.getEncryptedPassword()
                : this.encrypt(kmsClient, targetKeyId, masterUserPassword);
            logger.addLogEntry("Encrypted password: " + encryptedPassword);

            rds.setEncryptedPassword(encryptedPassword);

            rds = this.brokerCredentials(rds, encryptedPassword, kmsClient);
        }

        if (rds.getOptionGroupFile() != null) {
            try {
                String optionGroupJson = fileUtil.findFile(rds.getOptionGroupFile(), false);
                ObjectMapper objectMapper = new ObjectMapper();
                OptionGroup optionGroup = objectMapper.readValue(optionGroupJson, new TypeReference<OptionGroup>() {});
                rdsClient.setOptionGroup(instanceId, optionGroup);
            } catch (Exception ex) {
                throw new RuntimeException("Error attempting to set DB Options", ex);
            }
        }

        if (rds.getParameterGroupFile() != null) {
            try {
                String parameterGroupJson = fileUtil.findFile(rds.getParameterGroupFile(), false);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.addMixIn(Parameter.class, RdsBrokerMixIns.ParameterMixIn.class);
                List<Parameter> parameters = objectMapper
                    .readValue(parameterGroupJson, new TypeReference<List<Parameter>>() {});
                rdsClient.setDBParameterGroup(instanceId, parameters);
            } catch (Exception ex) {
                throw new RuntimeException("Error attempting to set DB Parameter Groups", ex);
            }
        }

        rds = injectCipherNotation(rds);

        return rds;
    }

    private RdsInstance brokerCredentials(RdsInstance instance, String encryptedMasterPassword, AWSKMS awskmsClient) {

        if (instance.getEngine().equalsIgnoreCase(POSTGRES_ENGINE)
            || instance.getEngine().equalsIgnoreCase(MYSQL_ENGINE)
            || instance.getEngine().contains(AURORA_ENGINE)) {
            logger.addLogEntry("Brokering credentials for " + instance.getEngine() + " instance "
                + instance.getEndpoint().getAddress());

            String appUsername = instance.getAppUsername() != null ? instance.getAppUsername()
                : getUsername(instance, "app");

            String appPassword = instance.getAppEncryptedPassword() != null ? instance.getAppEncryptedPassword()
                : this.encrypt(awskmsClient, targetKeyId, this.generateRandomPassword());

            logger.addLogEntry("... DB instance type is " + instance.getEngine().toLowerCase());
            logger.addLogEntry("... app username: " + appUsername);
            logger.addLogEntry("... app encrypted password: " + appPassword);
            logger.addLogEntry("... app password encrypted using KMS key: " + targetKeyId);

            String adminUsername = instance.getAdminUsername() != null ? instance.getAdminUsername()
                : getUsername(instance, "admin");

            String adminPassword = instance.getAdminEncryptedPassword() != null ? instance.getAdminEncryptedPassword()
                : this.encrypt(awskmsClient, targetKeyId, this.generateRandomPassword());

            logger.addLogEntry("... admin username: " + adminUsername);
            logger.addLogEntry("... admin encrypted password: " + adminPassword);
            logger.addLogEntry("... admin password encrypted using KMS key: " + targetKeyId);

            propertyHandler.addProperty("ecs.cluster", definition.getCluster());
            propertyHandler.addProperty("DB_ENGINE", this.getCredentialBrokerProfile(instance));
            propertyHandler.addProperty("DB_ADMIN_PASSWORD", adminPassword);
            propertyHandler.addProperty("DB_HOST", instance.getEndpoint().getAddress());
            propertyHandler.addProperty("DB_PORT", instance.getEndpoint().getPort().toString());
            propertyHandler.addProperty("DB_NAME", instance.getDBName());
            propertyHandler.addProperty("DB_USERNAME", instance.getMasterUsername());
            propertyHandler.addProperty("DB_PASSWORD", encryptedMasterPassword);
            propertyHandler.addProperty("DB_APP_USERNAME", appUsername);
            propertyHandler.addProperty("DB_APP_PASSWORD", appPassword);
            propertyHandler.addProperty("DB_ADMIN_USERNAME", adminUsername);
            propertyHandler.addProperty("DB_ADMIN_PASSWORD", adminPassword);
            propertyHandler.addProperty("classpathTemplate", "/brokerTemplates/rds/credential-broker.yml");

            instance.setAppUsername(appUsername);
            instance.setAdminUsername(adminUsername);
            instance.setAppEncryptedPassword(appPassword);
            instance.setAdminEncryptedPassword(adminPassword);

            logger.addLogEntry("\n");
            logger.addLogEntry("Running RDS credential broker to set updated IDs and passwords");
            EcsPush push = pushFactory.createPush(pushContext);
            push.push();
            logger.addLogEntry("RDS credential broker task completed");
            logger.addLogEntry("\n");
        }

        return instance;
    }

    private RdsInstance injectCipherNotation(RdsInstance instance) {
        String appPassword = instance.getAppEncryptedPassword();
        String adminPassword = instance.getAdminEncryptedPassword();
        String rootPassword = instance.getEncryptedPassword();

        String appPasswordVariableName = instance.getInjectNames().getAppEncryptedPassword();
        String adminPasswordVariableName = instance.getInjectNames().getAdminEncryptedPassword();
        String rootPasswordVariableName = instance.getInjectNames().getEncryptedPassword();

        Set<String> encryptablePasswordNames = new HashSet<>(Arrays.asList("spring.datasource.password",
            "spring_datasource_password", "spring.datasource.tomcat.password",
            "spring_datasource_tomcat_password", "liquibase.password", "liquibase_password", "flyway.password",
            "flyway_password"));

        if (instance.getCredPrefix() != null) {
            appPassword = instance.getCredPrefix() + appPassword;
            adminPassword = instance.getCredPrefix() + adminPassword;
            rootPassword = instance.getCredPrefix() + rootPassword;
        } else {
            if (encryptablePasswordNames.contains(appPasswordVariableName.toLowerCase())) {
                appPassword = CIPHER_PREFIX + appPassword;
            }

            if (encryptablePasswordNames.contains(adminPasswordVariableName.toLowerCase())) {
                adminPassword = CIPHER_PREFIX + adminPassword;
            }

            if (encryptablePasswordNames.contains(rootPasswordVariableName.toLowerCase())) {
                adminPassword = CIPHER_PREFIX + rootPassword;
            }
        }

        instance.setAppEncryptedPassword(appPassword);
        instance.setAdminEncryptedPassword(adminPassword);
        instance.setEncryptedPassword(rootPassword);

        return instance;
    }

    private String getUsername(RdsInstance instance, String prefix) {
        String username;

        if (instance.getEngine().toLowerCase().contains(MYSQL_ENGINE) || instance.getEngine()
            .equalsIgnoreCase(AURORA_ENGINE)) {
            username = prefix.concat(RandomStringUtils.randomAlphanumeric(MYSQL_MAXLENGTH - prefix.length()));
        } else {
            username = prefix.concat(UUID.randomUUID().toString().replace("-", ""));
        }

        return username;
    }

    private String encrypt(AWSKMS kmsClient, String keyId, String clearText) {
        Assert.hasText(keyId, "kmsKeyId must not be blank");
        if (clearText == null || clearText.isEmpty()) {
            return "";
        } else {
            final EncryptRequest encryptRequest =
                new EncryptRequest().withKeyId(keyId) //
                    .withPlaintext(ByteBuffer.wrap(clearText.getBytes()));

            final ByteBuffer encryptedBytes = kmsClient.encrypt(encryptRequest).getCiphertextBlob();

            return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Base64.getEncoder().encode(encryptedBytes.array())))
                .toString();
        }
    }

    private String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    private String getCredentialBrokerProfile(RdsInstance instance) {
        if ((instance.getEngine().equalsIgnoreCase(MYSQL_ENGINE)
            || (instance.getEngine().equalsIgnoreCase(AURORA_ENGINE))
            || ("aurora-mysql".equalsIgnoreCase(instance.getEngine())))
            && instance.getIAMDatabaseAuthenticationEnabled()) {
            return "mysqliam";
        }

        if ("aurora-postgresql".equalsIgnoreCase(instance.getEngine())) {
            return POSTGRES_ENGINE;
        }

        if ((instance.getEngine().equalsIgnoreCase(AURORA_ENGINE)
            || "aurora-mysql".equalsIgnoreCase(instance.getEngine()))
            && !instance.getIAMDatabaseAuthenticationEnabled()) {
            return MYSQL_ENGINE; // mysql profile for aurora without IAM auth
        }

        return instance.getEngine().toLowerCase();
    }
}
