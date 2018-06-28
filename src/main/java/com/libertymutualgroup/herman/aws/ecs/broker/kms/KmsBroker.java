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
package com.libertymutualgroup.herman.aws.ecs.broker.kms;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.CancelKeyDeletionRequest;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.DeleteAliasRequest;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.EnableKeyRequest;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.amazonaws.services.kms.model.ListResourceTagsRequest;
import com.amazonaws.services.kms.model.ListResourceTagsResult;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.amazonaws.services.kms.model.ScheduleKeyDeletionRequest;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.kms.model.TagResourceRequest;
import com.amazonaws.util.IOUtils;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KmsBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(KmsBroker.class);

    public static final String KMS_POLICY_JSON = "kms-policy.json";
    private static final String PREFIX = "alias/herman/";
    private HermanLogger logger;
    private PropertyHandler handler;
    private FileUtil fileUtil;
    private CommonTaskProperties taskProperties;

    public KmsBroker(HermanLogger logger, PropertyHandler handler, FileUtil fileUtil,
        CommonTaskProperties taskProperties) {
        this.logger = logger;
        this.handler = handler;
        this.fileUtil = fileUtil;
        this.taskProperties = taskProperties;
    }

    public boolean isActive(EcsPushDefinition definition) {
        return Boolean.TRUE.toString().equals(definition.getUseKms()) || definition.getDatabase() != null;
    }

    public String brokerKey(AWSKMS client, KmsAppDefinition definition, List<Tag> tags) {
        logger.addLogEntry("Brokering KMS key");
        String keyName = getKeyName(definition);
        String appKeyAlias = PREFIX + keyName;

        String keyId = getExistingKeyId(client, appKeyAlias);

        if (!Optional.ofNullable(keyId).isPresent()) {
            keyId = getExistingDeletedKey(client, definition, appKeyAlias);
        }

        if (!Optional.ofNullable(keyId).isPresent()) {
            logger.addLogEntry("... Creating new KMS key: " + appKeyAlias);
            CreateKeyResult key = client
                .createKey(
                    new CreateKeyRequest().withDescription("Herman key for " + definition.getAppName()).withTags(tags));
            CreateAliasRequest aliasReq = new CreateAliasRequest().withAliasName(appKeyAlias)
                .withTargetKeyId(key.getKeyMetadata().getArn());
            client.createAlias(aliasReq);
            keyId = key.getKeyMetadata().getKeyId();
        } else {
            DescribeKeyResult key = client.describeKey(new DescribeKeyRequest().withKeyId(keyId));
            if (key.getKeyMetadata().getDeletionDate() != null) {
                logger.addLogEntry("... Revoking the delete!");
                client.cancelKeyDeletion(new CancelKeyDeletionRequest().withKeyId(keyId));
                client.enableKey(new EnableKeyRequest().withKeyId(keyId));
            }

            // update keys in case of cluster move
            client.tagResource(new TagResourceRequest().withKeyId(keyId).withTags(tags));
        }

        // Update key policy
        String policy = handler.mapInProperties(getPolicy());
        try {
            client.putKeyPolicy(new PutKeyPolicyRequest()
                .withPolicyName("default")
                .withKeyId(keyId)
                .withPolicy(policy));
            logger.addLogEntry("... KMS key policy updated");
        } catch (Exception ex) {
            throw new RuntimeException(
                String.format("Error updating key policy for key ID %s. Policy: %s", keyId, policy), ex);
        }

        return keyId;
    }

    public String getExistingKeyArnFromId(AWSKMS client, String keyId) {
        DescribeKeyRequest describeRequest = new DescribeKeyRequest()
            .withKeyId(keyId);
        DescribeKeyResult keyResult = client.describeKey(describeRequest);
        if (keyResult != null && keyResult.getKeyMetadata() != null) {
            return keyResult.getKeyMetadata().getArn();
        }
        return null;
    }

    private String getExistingKeyId(AWSKMS client, String appKeyAlias) {
        String keyId = null;
        String listAliasesNextMarker = null;
        do {
            ListAliasesResult aliases = client.listAliases(new ListAliasesRequest().withMarker(listAliasesNextMarker));
            listAliasesNextMarker = aliases.getNextMarker();

            Optional<AliasListEntry> aliasOptional = aliases.getAliases().stream()
                .filter(alias -> appKeyAlias.equals(alias.getAliasName()))
                .findAny();
            if (aliasOptional.isPresent()) {
                keyId = aliasOptional.get().getTargetKeyId();
                logger.addLogEntry("... KMS key found: " + appKeyAlias + ":" + keyId);
            }
        } while (Optional.ofNullable(listAliasesNextMarker).isPresent() && !Optional.ofNullable(keyId).isPresent());
        return keyId;
    }

    private String getExistingDeletedKey(AWSKMS client, KmsAppDefinition definition, String appKeyAlias) {
        String keyId = null;
        String listKeysNextMarker = null;
        do {
            ListKeysResult listKeysResult = client.listKeys(new ListKeysRequest().withMarker(listKeysNextMarker));
            listKeysNextMarker = listKeysResult.getNextMarker();

            for (KeyListEntry key : listKeysResult.getKeys()) {
                List<Tag> tags = Collections.emptyList();
                try {
                    ListResourceTagsResult keyTags = client
                        .listResourceTags(new ListResourceTagsRequest().withKeyId(key.getKeyId()));
                    tags = keyTags.getTags();
                } catch (AWSKMSException ex) {
                    LOGGER.debug("Error getting tags for " + key.getKeyId(), ex);
                }
                for (Tag t : tags) {
                    if (this.taskProperties.getAppTagKey().equals(t.getTagKey()) && t.getTagValue()
                        .equals(definition.getAppName())) {
                        DescribeKeyResult hiddenKey = client
                            .describeKey(new DescribeKeyRequest().withKeyId(key.getKeyArn()));
                        if (hiddenKey.getKeyMetadata().getDeletionDate() != null) {
                            logger.addLogEntry(
                                "... KMS Key found: " + appKeyAlias + ":" + key.getKeyId() + " - Revoking the delete");
                            client.cancelKeyDeletion(new CancelKeyDeletionRequest().withKeyId(key.getKeyArn()));
                            client.enableKey(new EnableKeyRequest().withKeyId(key.getKeyArn()));
                            keyId = key.getKeyId();
                            CreateAliasRequest aliasReq = new CreateAliasRequest().withAliasName(appKeyAlias)
                                .withTargetKeyId(keyId);
                            client.createAlias(aliasReq);
                        }
                    }
                }

                if (Optional.ofNullable(keyId).isPresent()) {
                    break;
                }
            }

        } while (Optional.ofNullable(listKeysNextMarker).isPresent() && !Optional.ofNullable(keyId).isPresent());

        return keyId;
    }

    private String getKeyName(KmsAppDefinition definition) {
        String keyName;
        if (Optional.ofNullable(definition.getKmsKeyName()).isPresent()) {
            keyName = definition.getKmsKeyName();
        } else {
            keyName = definition.getAppName();
        }
        return keyName;
    }

    private String getPolicy() {
        String customPolicy = fileUtil.findFile(KMS_POLICY_JSON, true);

        String policy;
        if (customPolicy != null) {
            logger.addLogEntry("... Using custom KMS policy");
            policy = customPolicy;
        } else {
            logger.addLogEntry("... Using default KMS policy");
            InputStream policyStream = getClass().getResourceAsStream("/iam/" + KMS_POLICY_JSON);
            try {
                policy = IOUtils.toString(policyStream);
            } catch (IOException e) {
                throw new AwsExecException(e);
            }
        }
        return policy;
    }

    public void deleteKey(AWSKMS client, KmsAppDefinition definition) {
        String keyName = getKeyName(definition);
        String appKeyAlias = PREFIX + keyName;

        String keyId = getExistingKeyId(client, appKeyAlias);

        if (Optional.ofNullable(keyId).isPresent()) {
            logger.addLogEntry("Key exists but yml cleared - deleting: " + appKeyAlias);
            client.deleteAlias(new DeleteAliasRequest().withAliasName(appKeyAlias));
            client.scheduleKeyDeletion(new ScheduleKeyDeletionRequest().withKeyId(keyId));
        }
    }

}
