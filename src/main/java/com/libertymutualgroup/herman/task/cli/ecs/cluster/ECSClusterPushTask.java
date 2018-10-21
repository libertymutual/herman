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
package com.libertymutualgroup.herman.task.cli.ecs.cluster;

import com.amazonaws.auth.AWSCredentials;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPush;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushPropertyFactory;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

public class ECSClusterPushTask {
    private HermanLogger logger;

    public ECSClusterPushTask(HermanLogger logger) {
        this.logger = logger;
    }

    public void runTask(ECSClusterPushTaskConfiguration configuration) {
        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();
        final PropertyHandler propertyHandler = new PropertyHandlerUtil().getCliPropertyHandler(
            sessionCredentials,
            logger,
            configuration.getEnvironmentName(),
            configuration.getRootPath(),
            configuration.getCustomVariables());
        final ECSPushTaskProperties taskProperties = ECSPushPropertyFactory.getTaskProperties(
            sessionCredentials,
            logger,
            configuration.getCustomConfigurationBucket(),
            configuration.getRegion(),
            propertyHandler);

        EcsPushContext context = new EcsPushContext()
            .withLogger(logger)
            .withPropertyHandler(propertyHandler)
            .withEnvName(configuration.getEnvironmentName())
            .withSessionCredentials(CredentialsHandler.getCredentials())
            .withAwsClientConfig(CredentialsHandler.getConfiguration())
            .withRegion(configuration.getRegion())
            .withTimeout(configuration.getTimeout())
            .withRootPath(configuration.getRootPath())
            .withTaskProperties(taskProperties)
            .withCustomConfigurationBucket(configuration.getCustomConfigurationBucket());
        EcsPush push = new EcsPush(context);
        push.push();

        logger.addLogEntry("Done!");
    }
}
