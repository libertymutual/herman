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

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceNotActiveException;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ServicePurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicePurger.class);
    private static final String INTERRUPTED_WHILE_POLLING = "Interrupted while polling";
    private AmazonECS ecsClient;
    private HermanLogger buildLogger;

    public ServicePurger(AmazonECS ecsClient, HermanLogger buildLogger) {
        this.ecsClient = ecsClient;
        this.buildLogger = buildLogger;
    }

    public void purgeOtherClusters(String targetCluster, String serviceName) {
        buildLogger.addLogEntry("Checking other clusters for app");
        List<String> clusters = ecsClient.listClusters().getClusterArns();
        for (String clusterArn : clusters) {
            if (!clusterArn.contains(targetCluster)) {
                DescribeServicesResult svcsResult = ecsClient
                    .describeServices(new DescribeServicesRequest().withCluster(clusterArn).withServices(serviceName));
                if (!svcsResult.getServices().isEmpty()) {
                    ServicePurger purger = new ServicePurger(ecsClient, buildLogger);
                    buildLogger.addLogEntry("Moving app to " + targetCluster);
                    buildLogger.addLogEntry("Clearing service from " + clusterArn);
                    purger.shutdownAndDeleteService(clusterArn, serviceName);
                }
            }
        }
    }


    private void shutdownAndDeleteService(String cluster, String service) {
        try {
            ecsClient.updateService(
                new UpdateServiceRequest().withService(service).withCluster(cluster).withDesiredCount(0));
            waitForShutdown(cluster, service);
            buildLogger.addLogEntry("... Deleting app");
            ecsClient.deleteService(new DeleteServiceRequest().withCluster(cluster).withService(service));
            waitForServiceDelete(cluster, service);
        } catch (ServiceNotActiveException e) {
            LOGGER.debug("Error updating service: " + service, e);
            buildLogger.addLogEntry("... Service not active, nothing to shutdown");
        }
    }


    private void waitForShutdown(String cluster, String service) {
        for (int i = 0; i < 60; i++) {
            Service s = ecsClient
                .describeServices(new DescribeServicesRequest().withCluster(cluster).withServices(service))
                .getServices().get(0);
            if (s.getRunningCount() == 0) {
                return;
            } else {
                try {
                    buildLogger
                        .addLogEntry("... Waiting for shutdown to convert, still running: " + s.getRunningCount());
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    buildLogger.addLogEntry(INTERRUPTED_WHILE_POLLING);
                    throw new AwsExecException(INTERRUPTED_WHILE_POLLING);
                }
            }

        }
    }

    private void waitForServiceDelete(String cluster, String service) {
        buildLogger.addLogEntry("... Waiting for service delete");
        for (int i = 0; i < 60; i++) {
            Service s = ecsClient
                .describeServices(new DescribeServicesRequest().withCluster(cluster).withServices(service))
                .getServices().get(0);
            buildLogger.addLogEntry("... Service state: " + s.getStatus());
            if (!("DRAINING".equalsIgnoreCase(s.getStatus()) || "ACTIVE".equalsIgnoreCase(s.getStatus()))) {
                return;
            } else {
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    buildLogger.addLogEntry(INTERRUPTED_WHILE_POLLING);
                    throw new AwsExecException(INTERRUPTED_WHILE_POLLING);
                }
            }

        }
    }

}
