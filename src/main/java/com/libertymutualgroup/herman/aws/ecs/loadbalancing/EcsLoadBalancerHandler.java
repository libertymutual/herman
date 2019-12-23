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
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.AccessLog;
import com.amazonaws.services.elasticloadbalancing.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.ApplySecurityGroupsToLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateAppCookieStickinessPolicyRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DuplicateLoadBalancerNameException;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.SetLoadBalancerPoliciesOfListenerRequest;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.EcsPortHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcsLoadBalancerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcsLoadBalancerHandler.class);

    private static final String HTTPS = "HTTPS";
    public static final String INTERNET_FACING = "internet-facing";
    private AmazonElasticLoadBalancing elbClient;
    private CertHandler certHandler;
    private HermanLogger buildLogger;
    private DnsRegistrar dnsRegistrar;
    private ECSPushTaskProperties taskProperties;

    public EcsLoadBalancerHandler(AmazonElasticLoadBalancing elbClient, CertHandler certHandler,
        DnsRegistrar dnsRegistrar, HermanLogger buildLogger, ECSPushTaskProperties taskProperties) {
        this.elbClient = elbClient;
        this.certHandler = certHandler;
        this.dnsRegistrar = dnsRegistrar;
        this.buildLogger = buildLogger;
        this.taskProperties = taskProperties;
    }

    public LoadBalancer createLoadBalancer(EcsClusterMetadata clusterMetadata, EcsPushDefinition definition) {

        String appName = definition.getAppName();

        if (appName.length() > 32) {
            throw new AwsExecException(String.format("Load Balancer name '(%s)' exceeds the 32 maximum character length set by AWS.", appName));
        }

        EcsPortHandler portHandler = new EcsPortHandler();

        String protocol = definition.getService().getProtocol();
        if (protocol == null) {
            protocol = HTTPS;
        }

        String urlPrefix = appName;
        if (definition.getService().getUrlPrefixOverride() != null) {
            urlPrefix = definition.getService().getUrlPrefixOverride();
        }

        String urlSuffix = definition.getService().getUrlSuffix();
        SSLCertificate sslCertificate = certHandler.deriveCert(protocol, urlSuffix, urlPrefix);

        ContainerDefinition webContainer = portHandler.findContainerWithExposedPort(definition, false);
        Integer randomPort = webContainer.getPortMappings().get(0).getHostPort();
        Integer containerPort = webContainer.getPortMappings().get(0).getContainerPort();
        String containerName = webContainer.getName();

        boolean isInternetFacingUrlScheme = certHandler.isInternetFacingUrlScheme(sslCertificate,
            definition.getService().getUrlSchemeOverride());
        String elbScheme;
        List<String> elbSubnets;
        if (isInternetFacingUrlScheme || INTERNET_FACING.equals(definition.getService().getElbSchemeOverride())) {
            elbScheme = INTERNET_FACING;
            elbSubnets = clusterMetadata.getPublicSubnets();
        } else {
            elbScheme = "internal";
            elbSubnets = clusterMetadata.getElbSubnets();
        }

        List<String> elbSecurityGroups = new ArrayList<>();
        if (INTERNET_FACING.equals(elbScheme) && HTTPS.equals(protocol)) {
            elbSecurityGroups.addAll(taskProperties.getExternalElbSecurityGroups());
        } else {
            elbSecurityGroups.addAll(clusterMetadata.getElbSecurityGroups());
        }

        List<Listener> listeners = generateListeners(definition.getService().getElbSourcePorts(), randomPort, protocol, sslCertificate);
        List<com.amazonaws.services.elasticloadbalancing.model.Tag> tags = getElbTagList(
            clusterMetadata.getClusterCftStackTags(), appName);
        CreateLoadBalancerRequest createLoadBalancerRequest = new CreateLoadBalancerRequest()
            .withSubnets(elbSubnets)
            .withListeners(listeners).withScheme(elbScheme)
            .withSecurityGroups(elbSecurityGroups)
            .withLoadBalancerName(appName)
            .withTags(tags);
        try {
            elbClient.createLoadBalancer(createLoadBalancerRequest);
        } catch (DuplicateLoadBalancerNameException e) {
            LOGGER.debug("Error creating ELB: " + appName, e);

            LoadBalancerDescription loadBalancerDescription = elbClient.describeLoadBalancers(
                    new DescribeLoadBalancersRequest().withLoadBalancerNames(appName))
                .getLoadBalancerDescriptions().get(0);
            if (!elbScheme.equals(loadBalancerDescription.getScheme())) {
                throw new AwsExecException(String.format("Expected scheme (%s) and actual scheme (%s) do not match. "
                    + "Load balancer %s must be re-created.", elbScheme, loadBalancerDescription.getScheme(), appName));
            }

            buildLogger.addLogEntry("Updating ELB: " + appName);
            elbClient.deleteLoadBalancerListeners(new DeleteLoadBalancerListenersRequest().withLoadBalancerName(appName)
                .withLoadBalancerPorts(CollectionUtils.isNotEmpty(definition.getService().getElbSourcePorts())
                    ? definition.getService().getElbSourcePorts()
                    : Arrays.asList(443)));

            elbClient.createLoadBalancerListeners(
                new CreateLoadBalancerListenersRequest().withListeners(listeners).withLoadBalancerName(appName));

            buildLogger.addLogEntry("... Resetting security groups: " + String.join(", ", elbSecurityGroups));
            elbClient.applySecurityGroupsToLoadBalancer(new ApplySecurityGroupsToLoadBalancerRequest()
                .withLoadBalancerName(appName).withSecurityGroups(elbSecurityGroups));

            buildLogger.addLogEntry("... Resetting subnets: " + String.join(", ", elbSubnets));
            elbClient.attachLoadBalancerToSubnets(
                new AttachLoadBalancerToSubnetsRequest().withSubnets(elbSubnets).withLoadBalancerName(appName));

            buildLogger.addLogEntry("... Resetting tags");
            elbClient.addTags(new AddTagsRequest().withLoadBalancerNames(appName).withTags(tags));
        } catch (Exception ex) {
            throw new RuntimeException("Error creating ELB: " + createLoadBalancerRequest, ex);
        }

        if (definition.getService().getAppStickinessCookie() != null) {
            elbClient.createAppCookieStickinessPolicy(new CreateAppCookieStickinessPolicyRequest()
                .withLoadBalancerName(appName).withPolicyName("StickyElbPolicy")
                .withCookieName(definition.getService().getAppStickinessCookie()));
            elbClient.setLoadBalancerPoliciesOfListener(new SetLoadBalancerPoliciesOfListenerRequest()
                .withLoadBalancerName(appName).withLoadBalancerPort(443).withPolicyNames("StickyElbPolicy"));
        }

        resetAccessLoggingConfig(appName);

        HealthCheck healthCheck = definition.getService().getHealthCheck();
        String healthCheckPath = healthCheck.getTarget();
        if ("TCP".equals(healthCheckPath)) {
            healthCheckPath = "TCP:" + randomPort;
        } else {
            healthCheckPath = "HTTPS:" + randomPort + healthCheckPath;
        }
        healthCheck.setTarget(healthCheckPath);

        if (healthCheck.getInterval() == null) {
            healthCheck.setInterval(30);
        }
        if (healthCheck.getHealthyThreshold() == null) {
            healthCheck.setHealthyThreshold(2);
        }
        if (healthCheck.getTimeout() == null) {
            healthCheck.setTimeout(10);
        }
        if (healthCheck.getUnhealthyThreshold() == null) {
            healthCheck.setUnhealthyThreshold(10);
        }

        elbClient.configureHealthCheck(
            new ConfigureHealthCheckRequest().withLoadBalancerName(appName).withHealthCheck(healthCheck));

        String registeredUrl = urlPrefix + "." + definition.getService().getUrlSuffix();
        dnsRegistrar.registerDns(appName, "classic", protocol, registeredUrl);

        brokerDDoSWAFConfiguration(appName, protocol, elbScheme);

        buildLogger.addLogEntry("... ELB updates complete");
        return new LoadBalancer().withContainerName(containerName).withContainerPort(containerPort)
            .withLoadBalancerName(appName);
    }

    private void resetAccessLoggingConfig(String appName) {
        AccessLog accessLog;
        if (taskProperties.getLogsBucket() != null) {
            buildLogger.addLogEntry(String.format("... Enabling access logging using the %s bucket", taskProperties.getLogsBucket()));
            accessLog = new AccessLog()
                .withEnabled(true)
                .withEmitInterval(60) // Default is 60
                .withS3BucketName(taskProperties.getLogsBucket());
        } else {
            buildLogger.addLogEntry("... Disabling access logging");
            accessLog = new AccessLog().withEnabled(false);
        }
        elbClient.modifyLoadBalancerAttributes(new ModifyLoadBalancerAttributesRequest()
            .withLoadBalancerName(appName)
            .withLoadBalancerAttributes(new LoadBalancerAttributes().withAccessLog(accessLog)));
    }

    private void brokerDDoSWAFConfiguration(String appName, String protocol, String elbScheme) {
        if (INTERNET_FACING.equals(elbScheme)
                && HTTPS.equals(protocol)) {
            buildLogger.addLogEntry("Skipping DDoS / WAF configuration updates - not implemented for ELBs");
        }
    }

    private List<Listener> generateListeners(List<Integer> elbSourcePorts, Integer randomPort, String protocol,
        SSLCertificate cert) {
        List<Listener> listenerList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(elbSourcePorts)) {
            for (Integer elbSourceport : elbSourcePorts) {
                listenerList.add(generateListener(randomPort, protocol, cert, elbSourceport));
            }
        } else {
            listenerList.add(generateListener(randomPort, protocol, cert, 443));
        }

        return listenerList;
    }

    private Listener generateListener(Integer randomPort, String protocol, SSLCertificate cert, Integer elbPort) {
        Listener listener = new Listener()
            .withLoadBalancerPort(elbPort)
            .withInstancePort(randomPort)
            .withProtocol(protocol)
            .withInstanceProtocol(protocol);
        if (cert != null) {
            listener.setSSLCertificateId(cert.getArn());
        }

        return listener;
    }

    private List<com.amazonaws.services.elasticloadbalancing.model.Tag> getElbTagList(List<Tag> tags, String name) {
        List<com.amazonaws.services.elasticloadbalancing.model.Tag> result = new ArrayList<>();
        for (Tag cftTag : tags) {
            if ("Name".equals(cftTag.getKey())) {
                result.add(new com.amazonaws.services.elasticloadbalancing.model.Tag()
                    .withKey(this.taskProperties.getClusterTagKey())
                    .withValue(cftTag.getValue()));
                result.add(new com.amazonaws.services.elasticloadbalancing.model.Tag().withKey("Name").withValue(name));
            } else {
                result.add(new com.amazonaws.services.elasticloadbalancing.model.Tag().withKey(cftTag.getKey())
                    .withValue(cftTag.getValue()));
            }
        }
        return result;
    }

}
