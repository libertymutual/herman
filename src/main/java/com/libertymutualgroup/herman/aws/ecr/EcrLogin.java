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
package com.libertymutualgroup.herman.aws.ecr;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AuthorizationData;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;
import com.amazonaws.util.Base64;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EcrLogin {

    private HermanLogger logger;
    private AmazonECR client;

    public EcrLogin(HermanLogger logger, AWSCredentials sessionCredentials, ClientConfiguration config, Regions region) {
        client = AmazonECRClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
            .withClientConfiguration(config).withRegion(region).build();
        this.logger = logger;
    }

    public void login() {
        GetAuthorizationTokenResult result = client.getAuthorizationToken(new GetAuthorizationTokenRequest());

        AuthorizationData dockerLogin = result.getAuthorizationData().get(0);

        String rawLogin = new String(Base64.decode(dockerLogin.getAuthorizationToken()));

        rawLogin = rawLogin.replace("AWS:", "");

        String loginCommand = "docker login -u AWS -p " + rawLogin + " " + dockerLogin.getProxyEndpoint();

        Process p;
        try {
            p = Runtime.getRuntime().exec(loginCommand);
            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }
            logger.addLogEntry(buf.toString());
        } catch (IOException e) {
            throw new AwsExecException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException(e);
        }

    }

}
