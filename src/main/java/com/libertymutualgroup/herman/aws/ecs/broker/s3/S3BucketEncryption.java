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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.atlassian.bamboo.build.logger.BuildLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3BucketEncryption {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketEncryption.class);

    private final String bucketName;
    private final boolean bucketExists;
    private final AmazonS3 s3Client;
    private final BuildLogger buildLogger;

    public S3BucketEncryption(String bucketName, boolean bucketExists, AmazonS3 s3Client, BuildLogger buildLogger) {
        this.bucketName = bucketName;
        this.bucketExists = bucketExists;
        this.s3Client = s3Client;
        this.buildLogger = buildLogger;
    }

    public void ensureEncryption() {
        buildLogger.addBuildLogEntry("Ensuring default encryption enabled for bucket " + bucketName);

        if (bucketExists) {
            try {
                GetBucketEncryptionResult bucketEncryption = s3Client.getBucketEncryption(bucketName);
                if (bucketEncryption.getServerSideEncryptionConfiguration() != null) {
                    buildLogger.addBuildLogEntry("Bucket already has default encryption enabled -- Skipping");
                    return;
                }
            } catch (AmazonS3Exception e) {
                LOGGER.debug("Error getting bucket encryption: ", e);
                buildLogger.addBuildLogEntry("No default encryption exists - enabling");
            }
        }

        SetBucketEncryptionRequest request = new SetBucketEncryptionRequest()
            .withBucketName(bucketName)
            .withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
                .withRules(new ServerSideEncryptionRule()
                    .withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault()
                        .withSSEAlgorithm(SSEAlgorithm.AES256))));

        s3Client.setBucketEncryption(request);
    }
}
