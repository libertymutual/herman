package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Certificate;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerState;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerStateEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.SetSecurityGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.SetSubnetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EcsLoadBalancerV2HandlerTest {

    HermanLogger logger = new SysoutLogger();

    @Mock
    AmazonElasticLoadBalancing elbClient;
    @Mock
    CertHandler certHandler;
    @Mock
    DnsRegistrar dnsRegistrar;

    EcsLoadBalancerV2Handler handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ECSPushTaskProperties taskProperties = new ECSPushTaskProperties()
            .withCompany("lm")
            .withOrg("LMB")
            .withSbu("CI");

        handler = new EcsLoadBalancerV2Handler(elbClient, certHandler, dnsRegistrar, logger, taskProperties);

        when(certHandler.deriveCert("HTTPS", "np-lmb.lmig.com", "my-app-dev"))
            .thenReturn(new SSLCertificate().withArn("arn:somecert"));

    }

    @Test
    public void executeElbUpdate() throws IOException {
        //GIVEN
        EcsClusterMetadata meta = generateMetadata();
        EcsPushDefinition pushDef = loadTemplate("template.yml");

        LoadBalancer balancer = new LoadBalancer().withDNSName("https://some.aws.url")
            .withLoadBalancerArn("arn:balancer")
            .withCanonicalHostedZoneId("123")
            .withScheme("internal");
        DescribeLoadBalancersResult balancersResult = new DescribeLoadBalancersResult().withLoadBalancers(balancer);
        when(elbClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class))).thenReturn(balancersResult);

        TargetGroup targetGroup = new TargetGroup().withTargetGroupArn("arn:target");
        DescribeTargetGroupsResult groupsResult = new DescribeTargetGroupsResult().withTargetGroups(targetGroup);
        when(elbClient.describeTargetGroups(any(DescribeTargetGroupsRequest.class))).thenReturn(groupsResult);

        Listener listener = new Listener().withListenerArn("arn:listener");
        DescribeListenersResult listenersResult = new DescribeListenersResult().withListeners(listener);
        when(elbClient.describeListeners(any(DescribeListenersRequest.class))).thenReturn(listenersResult);

        //WHEN
        handler.createLoadBalancer(meta, pushDef);

        //THEN
        // DNS Registered
        verify(dnsRegistrar).registerDns(
            pushDef.getAppName(),
            "application",
            "HTTPS",
            pushDef.getService().getUrlPrefixOverride() + "." + pushDef.getService().getUrlSuffix());

        // Update Target Group 
        ModifyTargetGroupRequest modifyTargetGroupRequest = new ModifyTargetGroupRequest()
            .withTargetGroupArn(targetGroup.getTargetGroupArn())
            .withHealthCheckIntervalSeconds(pushDef.getService().getHealthCheck().getInterval())
            .withHealthCheckPath(pushDef.getService().getHealthCheck().getTarget())
            .withHealthCheckPort("traffic-port")
            .withHealthCheckTimeoutSeconds(pushDef.getService().getHealthCheck().getTimeout())
            .withHealthyThresholdCount(pushDef.getService().getHealthCheck().getHealthyThreshold())
            .withUnhealthyThresholdCount(pushDef.getService().getHealthCheck().getUnhealthyThreshold());
        verify(elbClient).modifyTargetGroup(modifyTargetGroupRequest);

        // Update Listener
        ModifyListenerRequest modifyListenerRequest = new ModifyListenerRequest()
            .withListenerArn(listener.getListenerArn())
            .withCertificates(new Certificate().withCertificateArn("arn:somecert"));
        verify(elbClient).modifyListener(modifyListenerRequest);

        //Update LB
        SetSecurityGroupsRequest groupsRequest = new SetSecurityGroupsRequest()
            .withLoadBalancerArn(balancer.getLoadBalancerArn()).withSecurityGroups(meta.getElbSecurityGroups());
        verify(elbClient).setSecurityGroups(groupsRequest);

        SetSubnetsRequest setSubnetsRequest = new SetSubnetsRequest().withLoadBalancerArn(balancer.getLoadBalancerArn())
            .withSubnets(meta.getElbSubnets());
        verify(elbClient).setSubnets(setSubnetsRequest);

        List<Tag> elbTags = new ArrayList<>();
        elbTags.add(new Tag().withKey("lm_cluster").withValue("some-cluster"));
        elbTags.add(new Tag().withKey("Name").withValue(pushDef.getAppName()));
        elbTags.add(new Tag().withKey("Org").withValue("some-org"));
        verify(elbClient)
            .addTags(new AddTagsRequest().withResourceArns(balancer.getLoadBalancerArn()).withTags(elbTags));
    }


    @Test
    public void executeElbCreate() throws IOException {
        //GIVEN
        EcsClusterMetadata meta = generateMetadata();

        EcsPushDefinition pushDef = loadTemplate("template.yml");

        DescribeLoadBalancersResult balancersResult = new DescribeLoadBalancersResult()
            .withLoadBalancers(new ArrayList<>());
        when(elbClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class))).thenReturn(balancersResult);

        TargetGroup targetGroup = new TargetGroup().withTargetGroupArn("arn:target");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult().withTargetGroups(targetGroup);
        when(elbClient.createTargetGroup(any(CreateTargetGroupRequest.class))).thenReturn(createTargetGroupResult);

        LoadBalancer balancer = new LoadBalancer().withDNSName("https://some.aws.url")
            .withLoadBalancerArn("arn:balancer")
            .withCanonicalHostedZoneId("123")
            .withScheme("internal");
        CreateLoadBalancerResult balancerResult = new CreateLoadBalancerResult().withLoadBalancers(balancer);
        when(elbClient.createLoadBalancer(any(CreateLoadBalancerRequest.class))).thenReturn(balancerResult);

        DescribeLoadBalancersResult createdResult = new DescribeLoadBalancersResult().withLoadBalancers(balancer);
        balancer.setState(new LoadBalancerState().withCode(LoadBalancerStateEnum.Active));
        DescribeLoadBalancersRequest pollingRequest = new com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest()
            .withLoadBalancerArns("arn:balancer");
        when(elbClient.describeLoadBalancers(pollingRequest)).thenReturn(createdResult);

        //WHEN
        handler.createLoadBalancer(meta, pushDef);

        //THEN
        // DNS Registered
        verify(dnsRegistrar).registerDns(
            pushDef.getAppName(),
            "application",
            "HTTPS",
            pushDef.getService().getUrlPrefixOverride() + "." + pushDef.getService().getUrlSuffix());

        CreateTargetGroupRequest ctgr = new CreateTargetGroupRequest().withName(pushDef.getAppName())
            .withHealthCheckIntervalSeconds(pushDef.getService().getHealthCheck().getInterval())
            .withHealthCheckPath(pushDef.getService().getHealthCheck().getTarget())
            .withHealthCheckPort("traffic-port")
            .withPort(443)
            .withHealthCheckTimeoutSeconds(pushDef.getService().getHealthCheck().getTimeout())
            .withHealthyThresholdCount(pushDef.getService().getHealthCheck().getHealthyThreshold())
            .withUnhealthyThresholdCount(pushDef.getService().getHealthCheck().getUnhealthyThreshold())
            .withProtocol(ProtocolEnum.HTTPS)
            .withVpcId(meta.getVpcId());
        verify(elbClient).createTargetGroup(ctgr);

        List<Tag> elbTags = new ArrayList<>();
        elbTags.add(new Tag().withKey("lm_cluster").withValue("some-cluster"));
        elbTags.add(new Tag().withKey("Name").withValue(pushDef.getAppName()));
        elbTags.add(new Tag().withKey("Org").withValue("some-org"));
        CreateLoadBalancerRequest createLoadBalancerRequest = new CreateLoadBalancerRequest()
            .withSubnets(meta.getElbSubnets())
            .withScheme("internal")
            .withSecurityGroups(meta.getElbSecurityGroups())
            .withName(pushDef.getAppName()).withTags(elbTags);
        verify(elbClient).createLoadBalancer(createLoadBalancerRequest);

        CreateListenerRequest createListenerRequest = new CreateListenerRequest()
            .withLoadBalancerArn(balancer.getLoadBalancerArn())
            .withCertificates(new Certificate().withCertificateArn("arn:somecert")).withProtocol(ProtocolEnum.HTTPS)
            .withPort(443).withDefaultActions(
                new Action().withTargetGroupArn(targetGroup.getTargetGroupArn()).withType(ActionTypeEnum.Forward));
        verify(elbClient).createListener(createListenerRequest);
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
