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
package com.libertymutualgroup.herman.task.cli.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Broker;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateContext;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

import static com.libertymutualgroup.herman.task.s3.S3CreatePropertyFactory.getTaskProperties;

public class S3CreateTask {

    private HermanLogger logger;

    public S3CreateTask(HermanLogger hermanLogger) { this.logger = hermanLogger;  }

    public void runTask(S3CreateTaskConfiguration configuration) {
        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();
        final PropertyHandler propertyHandler = PropertyHandlerUtil.getCliPropertyHandler(
                sessionCredentials,
                this.logger,
                configuration.getEnvironmentName(),
                configuration.getRootPath(),
                configuration.getCustomVariables()
        );

        S3CreateContext s3CreateContext = initializeContext(propertyHandler, logger, configuration, sessionCredentials);

        S3Broker s3Broker = initializeBroker(s3CreateContext);
        s3Broker.brokerFromConfigurationFile();

        logger.addLogEntry("Done!");
    }

    public S3CreateContext initializeContext(
            PropertyHandler propertyHandler,
            HermanLogger logger,
            S3CreateTaskConfiguration configuration,
            AWSCredentials sessionCredentials
    ){
        S3CreateContext s3CreateContext = new S3CreateContext()
                .withPropertyHandler(propertyHandler)
                .withLogger(logger)
                .withRegion(configuration.getRegion())
                .withRootPath(configuration.getRootPath())
                .withSessionCredentials(sessionCredentials)
                .withTaskProperties(getTaskProperties(sessionCredentials, logger, configuration.getRegion()))
                .withFileUtil(new FileUtil(configuration.getRootPath(), logger));
        return s3CreateContext;
    }

    public S3Broker initializeBroker(S3CreateContext context){
        S3Broker broker = new S3Broker(context);
        return broker;
    }


}
