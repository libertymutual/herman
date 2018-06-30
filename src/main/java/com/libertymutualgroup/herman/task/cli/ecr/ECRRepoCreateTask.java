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
package com.libertymutualgroup.herman.task.cli.ecr;

import com.amazonaws.auth.AWSCredentials;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecr.EcrCreate;
import com.libertymutualgroup.herman.logging.HermanLogger;

public class ECRRepoCreateTask {
    private HermanLogger logger;

    public ECRRepoCreateTask(HermanLogger logger) {
        this.logger = logger;
    }

    public void runTask(ECRRepoTaskConfiguration configuration) {
        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();

        EcrCreate create = new EcrCreate(logger, sessionCredentials, CredentialsHandler.getConfiguration(), configuration.getRegion());
        create.createRepo(configuration.getRepoName());
    }
}
