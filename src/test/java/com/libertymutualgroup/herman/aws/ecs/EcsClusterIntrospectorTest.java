package com.libertymutualgroup.herman.aws.ecs;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterIntrospector;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class EcsClusterIntrospectorTest {

    HermanLogger logger = new SysoutLogger();

    @Mock
    AmazonCloudFormation cftClient;
    @Mock
    AmazonEC2 ec2Client;

    EcsClusterIntrospector introspector;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        introspector = new EcsClusterIntrospector(cftClient, ec2Client, logger);
    }

    @Test
    public void testInspect() {
        //GIVEN
        String stackName = "some-cluster-stack";

        StackSummary stackSummary = new StackSummary()
            .withStackId(stackName)
            .withStackName(stackName);
        List<StackSummary> listStacksSummaries = Arrays.asList(stackSummary);
        ListStacksResult listStacksResult = new ListStacksResult()
            .withStackSummaries(listStacksSummaries)
            .withNextToken(null);

        when(cftClient.listStacks()).thenReturn(listStacksResult);

        DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
        List<StackResource> resources = describeStackResourcesResult.getStackResources();
        resources.add(
            new StackResource().withLogicalResourceId("someELBSecurityGroup").withPhysicalResourceId("sg12345"));
        resources.add(
            new StackResource().withLogicalResourceId("someAppSecurityGroup").withPhysicalResourceId("sg23456"));
        resources.add(
            new StackResource().withLogicalResourceId("RDSSecurityGroup").withPhysicalResourceId("sg34567"));
        resources.add(new StackResource().withLogicalResourceId("ECSCluster")
            .withPhysicalResourceId("dev-plat-cluster-WOURKSLUY"));
        resources.add(
            new StackResource().withLogicalResourceId("EncryptionKey").withPhysicalResourceId("123-123-123-123"));
        resources.add(new StackResource().withLogicalResourceId("DBSubnetGroup").withPhysicalResourceId("subnet12345"));
        resources.add(new StackResource().withLogicalResourceId("InstanceRole").withPhysicalResourceId("plat-role"));

        when(cftClient.describeStackResources(new DescribeStackResourcesRequest().withStackName(stackName)))
            .thenReturn(describeStackResourcesResult);

        Tag tag = new Tag().withKey("Name").withValue("some-cluster");
        Parameter orgParam = new Parameter().withParameterKey("NrOrgTag").withParameterValue("Platform");
        Parameter sbuParam = new Parameter().withParameterKey("NrSbuTag").withParameterValue("LMB");

        Stack stack = new Stack().withTags(tag).withParameters(orgParam, sbuParam);
        DescribeStacksResult describeStacksResult = new DescribeStacksResult().withStacks(stack);
        when(cftClient.describeStacks(new DescribeStacksRequest().withStackName(stackName)))
            .thenReturn(describeStacksResult);

        com.amazonaws.services.ec2.model.Tag vpcTag = new com.amazonaws.services.ec2.model.Tag().withKey("Name")
            .withValue("nonprod-vpc");
        Vpc vpc = new Vpc().withTags(vpcTag).withVpcId("vpc12345");
        when(ec2Client.describeVpcs()).thenReturn(new DescribeVpcsResult().withVpcs(vpc));

        com.amazonaws.services.ec2.model.Tag subnetTag = new com.amazonaws.services.ec2.model.Tag().withKey("Name")
            .withValue("private-elb-subnet");
        Subnet subnet = new Subnet().withTags(subnetTag).withVpcId("vpc12345");
        when(ec2Client.describeSubnets()).thenReturn(new DescribeSubnetsResult().withSubnets(subnet));

        //WHEN
        EcsClusterMetadata meta = introspector.introspect(stackName, Regions.US_EAST_1);

        //THEN
        assertEquals("sg23456", meta.getAppSecurityGroup());
        assertEquals(1, meta.getClusterCftStackTags().size());
        assertEquals("plat-role", meta.getClusterEcsRole());
        assertEquals("dev-plat-cluster-WOURKSLUY", meta.getClusterId());
        assertEquals("subnet12345", meta.getDbSubnetGroup());
        assertEquals("123-123-123-123", meta.getEncryptionKey());
        assertEquals("Platform", meta.getNewrelicOrgTag());
        assertEquals("LMB", meta.getNewrelicSbuTag());
        assertEquals("sg34567", meta.getRdsSecurityGroup());
        assertEquals("vpc12345", meta.getVpcId());
        assertEquals(1, meta.getElbSecurityGroups().size());
        assertEquals(1, meta.getElbSubnets().size());
    }

    @Test
    public void testInspectSharedStack() {
        //GIVEN
        String stackName = "some-cluster-stack-us-east-1";
        String sharedStackName = "some-cluster-stack-us-east-1-shared";

        DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
        List<StackResource> resources = describeStackResourcesResult.getStackResources();
        resources.add(
            new StackResource().withLogicalResourceId("someELBSecurityGroup").withPhysicalResourceId("sg12345"));
        resources.add(
            new StackResource().withLogicalResourceId("someAppSecurityGroup").withPhysicalResourceId("sg23456"));
        resources.add(
            new StackResource().withLogicalResourceId("RDSSecurityGroup").withPhysicalResourceId("sg34567"));
        resources.add(new StackResource().withLogicalResourceId("ECSCluster")
            .withPhysicalResourceId("dev-plat-cluster-WOURKSLUY"));
        resources.add(
            new StackResource().withLogicalResourceId("EncryptionKey").withPhysicalResourceId("123-123-123-123"));
        resources.add(new StackResource().withLogicalResourceId("DBSubnetGroup").withPhysicalResourceId("subnet12345"));
        resources.add(new StackResource().withLogicalResourceId("InstanceRole").withPhysicalResourceId("plat-role"));

        when(cftClient.describeStackResources(new DescribeStackResourcesRequest().withStackName(stackName))).thenThrow(new AmazonCloudFormationException(""));
        when(cftClient.describeStackResources(new DescribeStackResourcesRequest().withStackName(sharedStackName)))
            .thenReturn(describeStackResourcesResult);

        Tag tag = new Tag().withKey("Name").withValue("some-cluster");
        Parameter orgParam = new Parameter().withParameterKey("NrOrgTag").withParameterValue("Platform");
        Parameter sbuParam = new Parameter().withParameterKey("NrSbuTag").withParameterValue("LMB");

        Stack stack = new Stack().withTags(tag).withParameters(orgParam, sbuParam);
        DescribeStacksResult describeStacksResult = new DescribeStacksResult().withStacks(stack);
        when(cftClient.describeStacks(new DescribeStacksRequest().withStackName(sharedStackName)))
            .thenReturn(describeStacksResult);

        com.amazonaws.services.ec2.model.Tag vpcTag = new com.amazonaws.services.ec2.model.Tag().withKey("Name")
            .withValue("nonprod-vpc");
        Vpc vpc = new Vpc().withTags(vpcTag).withVpcId("vpc12345");
        when(ec2Client.describeVpcs()).thenReturn(new DescribeVpcsResult().withVpcs(vpc));

        com.amazonaws.services.ec2.model.Tag subnetTag = new com.amazonaws.services.ec2.model.Tag().withKey("Name")
            .withValue("private-elb-subnet");
        Subnet subnet = new Subnet().withTags(subnetTag).withVpcId("vpc12345");
        when(ec2Client.describeSubnets()).thenReturn(new DescribeSubnetsResult().withSubnets(subnet));

        //WHEN
        EcsClusterMetadata meta = introspector.introspect(stackName, Regions.US_EAST_1);

        //THEN
        assertEquals("sg23456", meta.getAppSecurityGroup());
        assertEquals(1, meta.getClusterCftStackTags().size());
        assertEquals("plat-role", meta.getClusterEcsRole());
        assertEquals("dev-plat-cluster-WOURKSLUY", meta.getClusterId());
        assertEquals("subnet12345", meta.getDbSubnetGroup());
        assertEquals("123-123-123-123", meta.getEncryptionKey());
        assertEquals("Platform", meta.getNewrelicOrgTag());
        assertEquals("LMB", meta.getNewrelicSbuTag());
        assertEquals("sg34567", meta.getRdsSecurityGroup());
        assertEquals("vpc12345", meta.getVpcId());
        assertEquals(1, meta.getElbSecurityGroups().size());
        assertEquals(1, meta.getElbSubnets().size());
    }

    @Test(expected = AwsExecException.class)
    public void testIntrospectAwsExceptionOnNonExistentStack() {
        introspector.introspect("not-a-real-stack", Regions.US_EAST_1);
    }

}
