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
package com.libertymutualgroup.herman.aws.ecs.broker.ddoswaf;

import static com.libertymutualgroup.herman.util.HttpStatusUtil.isSuccessful;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.logging.HermanLogger;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class DdosWafBroker {

    private HermanLogger buildLogger;
    private DdosWafBrokerProperties brokerConfiguration;
    private AWSLambda lambdaClient;

    public DdosWafBroker(HermanLogger buildLogger, DdosWafBrokerProperties brokerConfiguration, AWSLambda lambdaClient) {
        this.buildLogger = buildLogger;
        this.brokerConfiguration = brokerConfiguration;
        this.lambdaClient = lambdaClient;
    }

    public void brokerDDoSWAFConfiguration(String appName, String elbArn) {
        buildLogger.addLogEntry("Updating DDoS / WAF configuration");

        String payload;
        try {
           DdosWafBrokerRequest request = new DdosWafBrokerRequest()
               .withAppName(appName)
               .withElbResourceArn(elbArn)
               .withRuleActions(brokerConfiguration.getRuleActions());
            payload = new ObjectMapper().writeValueAsString(request);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting DDoS / WAF Broker payload", ex);
        }

        InvokeRequest brokerRequest = new InvokeRequest()
            .withFunctionName(brokerConfiguration.getDdosWafLambda())
            .withInvocationType(InvocationType.RequestResponse)
            .withPayload(payload);
        buildLogger.addLogEntry(
            "... Invoke request sent to the broker: " + brokerConfiguration.getDdosWafLambda());

        InvokeResult invokeResult = this.lambdaClient.invoke(brokerRequest);

        if (isSuccessful(invokeResult.getStatusCode()) && StringUtils.isEmpty(invokeResult.getFunctionError())) {
            String brokerUpdatesJson = new String(invokeResult.getPayload().array(), Charset.forName("UTF-8"));
            List<String> updates;
            try {
                updates = new ObjectMapper()
                    .readValue(brokerUpdatesJson, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse broker updates from: " + brokerUpdatesJson, e);
            }

            updates.stream().forEach(update ->
                buildLogger
                    .addLogEntry(
                        String.format("... DDoS / WAF Broker: %s", update))
            );
        } else {
            buildLogger.addLogEntry("... Error thrown by the DDoS / WAF Broker given payload: " + brokerRequest);
            String brokerResponseJson = new String(invokeResult.getPayload().array(), Charset.forName("UTF-8"));
            throw new RuntimeException("Error invoking DDoS / WAF Broker: " + brokerResponseJson);
        }
    }
}
