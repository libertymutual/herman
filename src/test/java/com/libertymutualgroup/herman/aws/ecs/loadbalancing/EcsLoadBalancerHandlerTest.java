package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.*;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EcsLoadBalancerHandlerTest {

    HermanLogger logger = new SysoutLogger();

    @Mock
    AmazonElasticLoadBalancing elbClient;

    @Mock
    CertHandler certHandler;

    @Mock
    DnsRegistrar dnsRegistrar;

    EcsLoadBalancerHandler handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ECSPushTaskProperties taskProperties = new ECSPushTaskProperties()
            .withCompany("lm")
            .withOrg("LMB")
            .withSbu("CI");

        handler = new EcsLoadBalancerHandler(elbClient, certHandler, dnsRegistrar, logger, taskProperties);
        when(certHandler.deriveCert("HTTPS", "np-lmb.lmig.com", "my-app-dev"))
            .thenReturn(new SSLCertificate().withArn("arn:somecert"));
    }

    @Test
    public void elbCreate() throws IOException {
        // GIVEN
        EcsClusterMetadata meta = generateMetadata();
        EcsPushDefinition pushDef = loadTemplate("template.yml");

        LoadBalancerDescription balancerDescription = new LoadBalancerDescription()
            .withDNSName("https://some.aws.url")
            .withCanonicalHostedZoneNameID("123")
            .withScheme("internal");
        DescribeLoadBalancersResult elbResult = new DescribeLoadBalancersResult()
            .withLoadBalancerDescriptions(balancerDescription);
        when(elbClient
            .describeLoadBalancers(new DescribeLoadBalancersRequest().withLoadBalancerNames(pushDef.getAppName())))
            .thenReturn(elbResult);

        // WHEN
        handler.createLoadBalancer(meta, pushDef);

        // THEN
        // DNS Registered
        verify(dnsRegistrar).registerDns(
            pushDef.getAppName(),
            "classic",
            "HTTPS",
            pushDef.getService().getUrlPrefixOverride() + "." + pushDef.getService().getUrlSuffix());
        // Create ELB
        List<Tag> elbTags = new ArrayList<>();
        elbTags.add(new Tag().withKey("lm_cluster").withValue("some-cluster"));
        elbTags.add(new Tag().withKey("Name").withValue(pushDef.getAppName()));
        elbTags.add(new Tag().withKey("Org").withValue("some-org"));
        Integer hostPort = pushDef.getContainerDefinitions().get(0).getPortMappings().get(0).getHostPort();
        Listener listener = new Listener().withInstancePort(hostPort).withLoadBalancerPort(443).withProtocol("HTTPS")
            .withInstanceProtocol("HTTPS").withSSLCertificateId("arn:somecert");
        verify(elbClient).createLoadBalancer(new CreateLoadBalancerRequest().withSubnets(meta.getElbSubnets())
            .withScheme("internal").withSecurityGroups(meta.getElbSecurityGroups()).withListeners(listener)
            .withLoadBalancerName(pushDef.getAppName()).withTags(elbTags));

        ConfigureHealthCheckRequest req = new ConfigureHealthCheckRequest().withLoadBalancerName(pushDef.getAppName())
            .withHealthCheck(pushDef.getService().getHealthCheck());
        verify(elbClient).configureHealthCheck(req);
    }

    @Test(expected=AwsExecException.class)
    public void shouldThrowExceptionWhenElbNameExceedsCharacterLimit() throws AwsExecException {
        // GIVEN
        EcsClusterMetadata meta = generateMetadata();
        EcsPushDefinition pushDef = new EcsPushDefinition();

        pushDef.setAppName("load-balancer-app-name-that-exceeds-max-length");

        // WHEN
        when(handler.createLoadBalancer(meta, pushDef)).thenThrow(new AwsExecException());

        // THEN (exception thrown above)
    }

    @Test
    public void elbUpdate() throws IOException {
        // GIVEN
        EcsClusterMetadata meta = generateMetadata();
        EcsPushDefinition pushDef = loadTemplate("template.yml");

        when(elbClient.createLoadBalancer(any())).thenThrow(new DuplicateLoadBalancerNameException("exists"));
        LoadBalancerDescription balancerDescription = new LoadBalancerDescription()
            .withDNSName("https://some.aws.url")
            .withCanonicalHostedZoneNameID("123")
            .withScheme("internal");
        DescribeLoadBalancersResult elbResult = new DescribeLoadBalancersResult()
            .withLoadBalancerDescriptions(balancerDescription);
        when(elbClient
            .describeLoadBalancers(new DescribeLoadBalancersRequest().withLoadBalancerNames(pushDef.getAppName())))
            .thenReturn(elbResult);

        // WHEN
        handler.createLoadBalancer(meta, pushDef);

        // THEN
        // DNS Registered
        verify(dnsRegistrar).registerDns(
            pushDef.getAppName(),
            "classic",
            "HTTPS",
            pushDef.getService().getUrlPrefixOverride() + "." + pushDef.getService().getUrlSuffix());

        // Update ELB
        verify(elbClient).deleteLoadBalancerListeners(any());

        Integer hostPort = pushDef.getContainerDefinitions().get(0).getPortMappings().get(0).getHostPort();
        Listener listener = new Listener().withInstancePort(hostPort).withLoadBalancerPort(443).withProtocol("HTTPS")
            .withInstanceProtocol("HTTPS").withSSLCertificateId("arn:somecert");
        verify(elbClient).createLoadBalancerListeners(new CreateLoadBalancerListenersRequest().withListeners(listener)
            .withLoadBalancerName(pushDef.getAppName()));
        verify(elbClient).applySecurityGroupsToLoadBalancer(new ApplySecurityGroupsToLoadBalancerRequest()
            .withLoadBalancerName(pushDef.getAppName()).withSecurityGroups(meta.getElbSecurityGroups()));

        List<Tag> elbTags = new ArrayList<>();
        elbTags.add(new Tag().withKey("lm_cluster").withValue("some-cluster"));
        elbTags.add(new Tag().withKey("Name").withValue(pushDef.getAppName()));
        elbTags.add(new Tag().withKey("Org").withValue("some-org"));
        verify(elbClient).addTags(new AddTagsRequest().withLoadBalancerNames(pushDef.getAppName()).withTags(elbTags));

        verify(elbClient).attachLoadBalancerToSubnets(new AttachLoadBalancerToSubnetsRequest()
            .withSubnets(meta.getElbSubnets()).withLoadBalancerName(pushDef.getAppName()));

        // Health Check
        ConfigureHealthCheckRequest req = new ConfigureHealthCheckRequest().withLoadBalancerName(pushDef.getAppName())
            .withHealthCheck(pushDef.getService().getHealthCheck());
        verify(elbClient).configureHealthCheck(req);
    }

    private EcsPushDefinition loadTemplate(String templateName) throws IOException {
        URL template = this.getClass().getResource("/sampleTemplates/" + templateName);
        String temp = FileUtils.readFileToString(new File(template.getFile()));

        PropertyHandler ph = mock(TaskContextPropertyHandler.class);
        when(ph.mapInProperties(any())).thenReturn(temp);

        EcsDefinitionParser parser = new EcsDefinitionParser(ph);
        EcsPushDefinition pushDef = parser.parse(temp, false);
        return pushDef;
    }

    private EcsClusterMetadata generateMetadata() {
        EcsClusterMetadata meta = mock(EcsClusterMetadata.class);
        List<com.amazonaws.services.cloudformation.model.Tag> tags = new ArrayList<>();
        com.amazonaws.services.cloudformation.model.Tag nameTag = new com.amazonaws.services.cloudformation.model.Tag()
            .withKey("Name").withValue("some-cluster");
        com.amazonaws.services.cloudformation.model.Tag otherTag = new com.amazonaws.services.cloudformation.model.Tag()
            .withKey("Org").withValue("some-org");
        tags.add(nameTag);
        tags.add(otherTag);
        when(meta.getClusterCftStackTags()).thenReturn(tags);
        List<String> elbSecGrps = new ArrayList<>();
        elbSecGrps.add("sg123456");
        List<String> elbSubnets = new ArrayList<>();
        elbSubnets.add("subnet12345");
        when(meta.getElbSecurityGroups()).thenReturn(elbSecGrps);
        when(meta.getElbSubnets()).thenReturn(elbSubnets);
        when(meta.getVpcId()).thenReturn("vpc12345");
        return meta;
    }
}
