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
package com.libertymutualgroup.herman.aws.ecs.broker.newrelic;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.domain.HermanBrokerStatus;
import com.libertymutualgroup.herman.aws.ecs.logging.LoggingService;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.Optional;

import static com.libertymutualgroup.herman.util.HttpStatusUtil.isSuccessful;

public class NewRelicBroker {

    private HermanLogger buildLogger;
    private PropertyHandler propertyHandler;
    private FileUtil fileUtil;
    private NewRelicBrokerConfiguration brokerConfiguration;
    private AWSLambda lambdaClient;

    public NewRelicBroker(PropertyHandler propertyHandler, HermanLogger buildLogger, FileUtil fileUtil,
        NewRelicBrokerConfiguration brokerConfiguration, AWSLambda lambdaClient) {
        this.propertyHandler = propertyHandler;
        this.buildLogger = buildLogger;
        this.fileUtil = fileUtil;
        this.brokerConfiguration = brokerConfiguration;
        this.lambdaClient = lambdaClient;
    }

    public void brokerNewRelicApplicationDeployment(
        NewRelicConfiguration newRelicConfigurationDefinition,
        String policyName,
        String newRelicApplicationName,
        String newRelicLicenseKey) {

        buildLogger.addLogEntry("\n");
        buildLogger.addLogEntry("Brokering New Relic configuration");

        String payload;
        try {
            NewRelicBrokerRequest newRelicBrokerRequest = getNewRelicBrokerRequest(newRelicConfigurationDefinition,
                policyName, newRelicApplicationName, newRelicLicenseKey);
            payload = new ObjectMapper().writeValueAsString(newRelicBrokerRequest);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting NR Broker payload", ex);
        }

        InvokeRequest dnsBrokerInvokeRequest = new InvokeRequest()
            .withFunctionName(brokerConfiguration.getBrokerProperties().getNrLambda())
            .withInvocationType(InvocationType.RequestResponse)
            .withPayload(payload);
        buildLogger.addLogEntry(
            "... Invoke request sent to the broker: " + brokerConfiguration.getBrokerProperties().getNrLambda());

        InvokeResult invokeResult = this.lambdaClient.invoke(dnsBrokerInvokeRequest);


        if (isSuccessful(invokeResult.getStatusCode()) && StringUtils.isEmpty(invokeResult.getFunctionError())) {
            String nrBrokerResponseJson = new String(invokeResult.getPayload().array(), Charset.forName("UTF-8"));

            NewRelicBrokerResponse response;
            try {
                response = new ObjectMapper()
                    .readValue(nrBrokerResponseJson, NewRelicBrokerResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse NR broker response from: " + nrBrokerResponseJson, e);
            }

            response.getUpdates().stream().forEach(update -> {
                buildLogger
                    .addLogEntry(
                        String.format("... New Relic Broker: [%s] %s", update.getStatus(), update.getMessage()));

                if (HermanBrokerStatus.ERROR.equals(update.getStatus())) {
                    buildLogger.addLogEntry("... Error returned by the NR Broker given payload: " + payload);
                    throw new RuntimeException("New Relic broker error. See logs.");
                }
            });

            addNewRelicLinkToLogs(response.getApplicationId());
        } else {
            buildLogger.addLogEntry("... Error thrown by the NR Broker given payload: " + payload);
            String nrBrokerResponseJson = new String(invokeResult.getPayload().array(), Charset.forName("UTF-8"));
            throw new RuntimeException("Error invoking the New Relic Broker: " + nrBrokerResponseJson);
        }
    }

    NewRelicBrokerRequest getNewRelicBrokerRequest(NewRelicConfiguration newRelicConfigurationDefinition,
        String policyName, String newRelicApplicationName, String newrelicLicenseKey) {
        NewRelicConfiguration newRelicConfiguration = getNewRelicConfiguration(newRelicConfigurationDefinition);
        NewRelicApplicationDeploymentRequest newRelicApplicationDeployment = new NewRelicApplicationDeploymentRequest()
            .withRevision(propertyHandler.lookupVariable("bamboo.planRepository.revision"))
            .withVersion(propertyHandler.lookupVariable("bamboo.deploy.version"));

        return new NewRelicBrokerRequest()
            .withPolicyName(policyName)
            .withNewRelicApplicationName(newRelicApplicationName)
            .withConfiguration(newRelicConfiguration)
            .withDeployment(newRelicApplicationDeployment)
            .withNrLicenseKey(newrelicLicenseKey);
    }

    private NewRelicConfiguration getNewRelicConfiguration(NewRelicConfiguration newRelicConfigurationDefinition) {
        NewRelicConfiguration newRelicConfiguration;
        if (newRelicConfigurationDefinition != null) {
            String channels = propertyHandler
                .mapInProperties(fileUtil.findFile(newRelicConfigurationDefinition.getChannels(), false));
            String dbName = newRelicConfigurationDefinition.getDbName();

            String conditions = null;
            if (StringUtils.isNotBlank(newRelicConfigurationDefinition.getConditions())) {
                conditions = propertyHandler
                    .mapInProperties(fileUtil.findFile(newRelicConfigurationDefinition.getConditions(), false));
            }

            String rdsPluginsConditions = null;
            if (StringUtils.isNotBlank(newRelicConfigurationDefinition.getRdsPluginsConditions())) {
                rdsPluginsConditions = propertyHandler.mapInProperties(
                    fileUtil.findFile(newRelicConfigurationDefinition.getRdsPluginsConditions(), false));
            }

            String nrqlConditions = null;
            if (StringUtils.isNotBlank(newRelicConfigurationDefinition.getNrqlConditions())) {
                nrqlConditions = propertyHandler
                    .mapInProperties(fileUtil.findFile(newRelicConfigurationDefinition.getNrqlConditions(), false));
            }

            String infrastructureConditions = null;
            if (StringUtils.isNotBlank(newRelicConfigurationDefinition.getInfrastructureConditions())) {
                infrastructureConditions = propertyHandler
                    .mapInProperties(fileUtil.findFile(newRelicConfigurationDefinition.getInfrastructureConditions(), false));
            }

            String synthetics = null;
            if (StringUtils.isNotBlank(newRelicConfigurationDefinition.getSynthetics())) {
                synthetics = propertyHandler
                    .mapInProperties(fileUtil.findFile(newRelicConfigurationDefinition.getSynthetics(), false));
            }

            newRelicConfiguration = new NewRelicConfiguration()
                .withDbName(dbName)
                .withChannels(channels)
                .withConditions(conditions)
                .withRdsPluginsConditions(rdsPluginsConditions)
                .withNrqlConditions(nrqlConditions)
                .withInfrastructureConditions(infrastructureConditions)
                .withSynthetics(synthetics)
                .withApdex(newRelicConfigurationDefinition.getApdex());
        } else {
            newRelicConfiguration = null;
        }
        return newRelicConfiguration;
    }

    void addNewRelicLinkToLogs(String applicationId) {
        if (Optional.ofNullable(applicationId).isPresent()) {
            LoggingService loggingService = new LoggingService(buildLogger);
            String link = String.format("https://rpm.newrelic.com/accounts/%s/applications/%s",
                brokerConfiguration.getBrokerProperties().getAccountId(), applicationId);
            loggingService.logSection("New Relic UI", link);
        }
    }
}
