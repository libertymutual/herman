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
package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.ecs.broker.domain.HermanBrokerStatus;
import com.libertymutualgroup.herman.aws.ecs.broker.domain.HermanBrokerUpdate;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.List;

import static com.libertymutualgroup.herman.util.HttpStatusUtil.isSuccessful;

public class DnsRegistrar {

    private AWSLambda lambdaClient;
    private HermanLogger buildLogger;
    private String dnsLambda;

    public DnsRegistrar(AWSLambda lambdaClient, HermanLogger buildLogger, String dnsLambda) {
        this.lambdaClient = lambdaClient;
        this.buildLogger = buildLogger;
        this.dnsLambda = dnsLambda;
    }

    public void registerDns(String vanityUrl, String awsUrl, String appName, List<Tag> tags) {
        String payload = getPayload(vanityUrl, awsUrl, appName, tags);
        InvokeRequest dnsBrokerInvokeRequest = new InvokeRequest()
            .withFunctionName(dnsLambda)
            .withInvocationType(InvocationType.RequestResponse)
            .withPayload(payload);
        buildLogger.addLogEntry("... Invoke request sent to the DNS Broker");

        InvokeResult invokeResult = this.lambdaClient.invoke(dnsBrokerInvokeRequest);
        if (isSuccessful(invokeResult.getStatusCode()) && StringUtils
            .isEmpty(invokeResult.getFunctionError())) {
            String dnsBrokerUpdatesJson = new String(invokeResult.getPayload().array(), Charset.forName("UTF-8"));
            List<HermanBrokerUpdate> updates;
            try {
                updates = new ObjectMapper()
                    .readValue(dnsBrokerUpdatesJson, new TypeReference<List<HermanBrokerUpdate>>() {});
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse broker updates from: " + dnsBrokerUpdatesJson, e);
            }

            updates.stream().forEach(update -> {
                buildLogger
                    .addLogEntry(
                        String.format("... DNS Broker: [%s] %s", update.getStatus(), update.getMessage()));

                if (HermanBrokerStatus.ERROR.equals(update.getStatus())) {
                    throw new RuntimeException("DNS registration error");
                }
            });
        } else {
            buildLogger.addLogEntry("... Error thrown by the DNS Broker given payload: " + payload);
            throw new RuntimeException("Error invoking DNS Broker: " + invokeResult);
        }
    }

    private String getPayload(String vanityUrl, String awsUrl, String appName, List<Tag> tags) {
        try {
            DnsBrokerRequest dnsBrokerRequest = new DnsBrokerRequest()
                .withAppName(appName)
                .withAwsUrl(awsUrl)
                .withVanityUrl(vanityUrl)
                .withTags(tags);
            return new ObjectMapper().writeValueAsString(dnsBrokerRequest);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting DNS broker payload", ex);
        }
    }
}
