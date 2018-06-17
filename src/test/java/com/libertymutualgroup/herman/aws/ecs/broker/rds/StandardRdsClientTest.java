package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateOptionGroupRequest;
import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBParameterGroupNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeOptionGroupsRequest;
import com.amazonaws.services.rds.model.DescribeOptionGroupsResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.ModifyOptionGroupRequest;
import com.amazonaws.services.rds.model.OptionGroup;
import com.amazonaws.services.rds.model.OptionGroupMembership;
import com.amazonaws.services.rds.model.OptionGroupNotFoundException;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.PendingModifiedValues;
import com.amazonaws.services.rds.model.Tag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

public class StandardRdsClientTest {

    @Mock
    AmazonRDS client;
    @Mock
    EcsClusterMetadata clusterMetadata;
    @Mock
    HermanLogger logger;
    @Mock
    FileUtil fileUtil;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DescribeDBInstancesResult result = new DescribeDBInstancesResult();
        result.setDBInstances(Arrays.asList(RdsCommonTestObjects.initDbInstance()));
        Mockito.when(client.describeDBInstances(Mockito.any())).thenReturn(result);
    }

    private StandardRdsClient initClient(EcsPushDefinition definition, List<Tag> tags) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        RdsBroker.pollingIntervalMs = 0;
        return new StandardRdsClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
    }

    @Test
    public void setOptionGroup_GroupExists_Modify() throws Exception {
        // GIVEN
        final String optionGroupFile = "/rds/option-group-options.json";
        final String dbId = "lmb-app-ui-dev";
        final String engine = "oracle";
        final String optionGroupName = String.format("%s-%s-%s", engine, "1-2-3", dbId);

        RdsInstance rds = new RdsInstance();
        rds.setEngine(engine);
        rds.setOptionGroupFile(optionGroupFile);
        rds.setDBInstanceIdentifier(dbId);
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);

        List<Tag> tags = new ArrayList<>();
        StandardRdsClient rdsClient = initClient(definition, tags);

        DBInstance rdsResult = new DBInstance();
        rdsResult.setDBInstanceIdentifier(dbId);
        rdsResult.setEngine(engine);
        rdsResult
            .setOptionGroupMemberships(Arrays.asList(new OptionGroupMembership().withOptionGroupName(optionGroupName)));
        Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbId)))
            .thenReturn(new DescribeDBInstancesResult().withDBInstances(rdsResult));

        // Set mock option group options
        URL template = this.getClass().getResource(optionGroupFile);
        String optionGroupOptions = FileUtils.readFileToString(new File(template.getFile()));
        Mockito.when(fileUtil.findFile(rds.getOptionGroupFile(), false)).thenReturn(optionGroupOptions);

        // Set mock existing option group
        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();
        OptionGroup options = objectMapper.readValue(optionGroupOptions, new TypeReference<OptionGroup>() {});
        OptionGroup optionGroup = objectMapper.readValue(optionGroupOptions, OptionGroup.class);
        Mockito
            .when(client.describeOptionGroups(new DescribeOptionGroupsRequest().withOptionGroupName(optionGroupName)))
            .thenReturn(new DescribeOptionGroupsResult().withOptionGroupsList(optionGroup));

        // WHEN
        rdsClient.setOptionGroup(rds.getDBInstanceIdentifier(), options);

        // THEN
        Mockito.verify(client, times(1)).describeOptionGroups(any(DescribeOptionGroupsRequest.class));

        ModifyOptionGroupRequest modifyOptionGroupRequest = new ModifyOptionGroupRequest()
            .withApplyImmediately(true)
            .withOptionsToInclude(
                optionGroup.getOptions().stream().map(rdsClient::getOptionConfiguration).collect(Collectors.toList()))
            .withOptionsToRemove(Collections.EMPTY_LIST)
            .withOptionGroupName(optionGroupName);
        Mockito.verify(client, times(1)).modifyOptionGroup(modifyOptionGroupRequest);

        Mockito.verify(client, times(0)).createOptionGroup(any(CreateOptionGroupRequest.class));
        Mockito.verify(client, times(0)).modifyDBInstance(any(ModifyDBInstanceRequest.class));
    }

    @Test
    public void setOptionGroup_GroupExists_Remove() throws Exception {
        // GIVEN
        final String optionGroupFile = "/rds/option-group-options.json";
        final String dbId = "lmb-app-ui-dev";
        final String engine = "oracle";
        final String optionGroupName = String.format("%s-%s-%s", engine, "1-2-3", dbId);

        RdsInstance rds = new RdsInstance();
        rds.setEngine(engine);
        rds.setOptionGroupFile(optionGroupFile);
        rds.setDBInstanceIdentifier(dbId);
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        List<Tag> tags = new ArrayList<>();

        RdsClient rdsClient = initClient(definition, tags);

        DBInstance rdsResult = new DBInstance();
        rdsResult.setDBInstanceIdentifier(dbId);
        rdsResult.setEngine(engine);
        rdsResult
            .setOptionGroupMemberships(Arrays.asList(new OptionGroupMembership().withOptionGroupName(optionGroupName)));
        Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbId)))
            .thenReturn(new DescribeDBInstancesResult().withDBInstances(rdsResult));

        URL template = this.getClass().getResource(optionGroupFile);
        String optionGroupOptions = FileUtils.readFileToString(new File(template.getFile()));
        ObjectMapper objectMapper = new ObjectMapper();
        OptionGroup oldOptionGroup = objectMapper.readValue(optionGroupOptions, OptionGroup.class);
        OptionGroup newOptionGroup = objectMapper.readValue(optionGroupOptions, OptionGroup.class)
            .withOptions(Collections.EMPTY_LIST);

        // Set mock option group options
        Mockito.when(fileUtil.findFile(rds.getOptionGroupFile(), false))
            .thenReturn(objectMapper.writeValueAsString(newOptionGroup));

        // Set mock existing option group
        Mockito
            .when(client.describeOptionGroups(new DescribeOptionGroupsRequest().withOptionGroupName(optionGroupName)))
            .thenReturn(new DescribeOptionGroupsResult().withOptionGroupsList(oldOptionGroup));

        // WHEN
        rdsClient.setOptionGroup(rds.getDBInstanceIdentifier(), newOptionGroup);

        // THEN
        Mockito.verify(client, times(1)).describeOptionGroups(any(DescribeOptionGroupsRequest.class));

        ModifyOptionGroupRequest modifyOptionGroupRequest = new ModifyOptionGroupRequest()
            .withApplyImmediately(true)
            .withOptionsToInclude(Collections.EMPTY_LIST)
            .withOptionsToRemove(Arrays.asList("SSL", "SQLT"))
            .withOptionGroupName(optionGroupName);
        Mockito.verify(client, times(1)).modifyOptionGroup(modifyOptionGroupRequest);

        Mockito.verify(client, times(0)).createOptionGroup(any(CreateOptionGroupRequest.class));
        Mockito.verify(client, times(0)).modifyDBInstance(any(ModifyDBInstanceRequest.class));
    }

    @Test
    public void setOptionGroup_CreateGroup() throws Exception {
        // GIVEN
        final String optionGroupFile = "/rds/option-group-options.json";
        final String dbId = "lmb-app-ui-dev";
        final String engine = "oracle";
        final String optionGroupName = String.format("%s-%s-%s", engine, "1-2-3", dbId);

        RdsInstance rds = new RdsInstance();
        rds.setEngine(engine);
        rds.setOptionGroupFile(optionGroupFile);
        rds.setDBInstanceIdentifier(dbId);

        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        List<Tag> tags = new ArrayList<>();

        StandardRdsClient rdsClient = initClient(definition, tags);

        DBInstance rdsResult = new DBInstance();
        rdsResult.setDBInstanceIdentifier(dbId);
        rdsResult.setEngine(engine);
        Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbId)))
            .thenReturn(new DescribeDBInstancesResult().withDBInstances(rdsResult));

        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();

        // Set mock option group options
        URL template = this.getClass().getResource(optionGroupFile);
        String optionGroupOptions = FileUtils.readFileToString(new File(template.getFile()));
        OptionGroup options = objectMapper.readValue(optionGroupOptions, new TypeReference<OptionGroup>() {});
        Mockito.when(fileUtil.findFile(rds.getOptionGroupFile(), false)).thenReturn(optionGroupOptions);

        // Set mock existing option group
        Mockito.when(client.describeOptionGroups(any(DescribeOptionGroupsRequest.class)))
            .thenThrow(new OptionGroupNotFoundException("test"));

        // WHEN
        rdsClient.setOptionGroup(rds.getDBInstanceIdentifier(), options);

        // THEN
        Mockito.verify(client, times(1)).describeOptionGroups(any(DescribeOptionGroupsRequest.class));

        OptionGroup optionGroup = objectMapper.readValue(optionGroupOptions, OptionGroup.class);
        ModifyOptionGroupRequest modifyOptionGroupRequest = new ModifyOptionGroupRequest()
            .withApplyImmediately(true)
            .withOptionsToInclude(
                optionGroup.getOptions().stream().map(rdsClient::getOptionConfiguration).collect(Collectors.toList()))
            .withOptionsToRemove(Collections.EMPTY_LIST)
            .withOptionGroupName(optionGroupName);
        Mockito.verify(client, times(1)).modifyOptionGroup(modifyOptionGroupRequest);

        Mockito.verify(client, times(1)).createOptionGroup(any(CreateOptionGroupRequest.class));
        Mockito.verify(client, times(1)).modifyDBInstance(any(ModifyDBInstanceRequest.class));
    }

    @Test
    public void setDBParameterGroup_CreateGroup() throws Exception {
        // GIVEN
        final String paramGroupFile = "/rds/parameter-group-parameters.json";
        final String dbId = "lmb-app-ui-dev";

        RdsInstance rds = new RdsInstance();
        rds.setEngine("oracle");
        rds.setEngineVersion("12.4.5");
        rds.setParameterGroupFile(paramGroupFile);
        rds.setDBInstanceIdentifier(dbId);

        List<Tag> tags = Collections.EMPTY_LIST;
        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();

        DBInstance rdsResult = new DBInstance();
        rdsResult.setDBInstanceIdentifier(dbId);
        rdsResult.setDBParameterGroups(Collections.EMPTY_LIST);
        rdsResult.setDBInstanceStatus("available");
        rdsResult.setPendingModifiedValues(new PendingModifiedValues());
        Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbId)))
            .thenReturn(new DescribeDBInstancesResult().withDBInstances(rdsResult));

        // Set up Param group
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult()
            .withDBEngineVersions(new DBEngineVersion().withDBParameterGroupFamily("oracle-12"));
        Mockito.when(client.describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class)))
            .thenReturn(describeDBEngineVersionsResult);
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbParameterGroupName = String
            .format("%s-%s", dbEngineVersion.getDBParameterGroupFamily(), rdsResult.getDBInstanceIdentifier());

        // Set mock option group options
        URL template = this.getClass().getResource(paramGroupFile);
        String paramGroupJson = FileUtils.readFileToString(new File(template.getFile()));
        Mockito.when(fileUtil.findFile(rds.getParameterGroupFile(), false)).thenReturn(paramGroupJson);

        List<Parameter> parameters = objectMapper.readValue(paramGroupJson, new TypeReference<List<Parameter>>() {});

        // Throw DescribeDBParameterGroupsRequest when getting group - Trigger create
        Mockito.when(client.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class)))
            .thenThrow(DBParameterGroupNotFoundException.class);

        // Set mock DB Engine Version
        Mockito.when(client.createDBParameterGroup(any(CreateDBParameterGroupRequest.class)))
            .thenReturn(new DBParameterGroup().withDBParameterGroupName(rds.getDBInstanceIdentifier()));
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        RdsClient rdsClient = initClient(definition, tags);

        // WHEN
        rdsClient.setDBParameterGroup(rds.getDBInstanceIdentifier(), parameters);

        // THEN
        Mockito.verify(client, times(1)).describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class));
        Mockito.verify(client, times(1)).describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class));
        Mockito.verify(client, times(1)).createDBParameterGroup(new CreateDBParameterGroupRequest()
            .withDBParameterGroupFamily("oracle-12")
            .withDBParameterGroupName(dbParameterGroupName)
            .withDescription(String.format("%s %s Parameter Group", rds.getDBInstanceIdentifier(),
                dbEngineVersion.getDBParameterGroupFamily()))
            .withTags(tags));
        Mockito.verify(client, times(1)).modifyDBInstance(any(ModifyDBInstanceRequest.class));
    }

    @Test
    public void setDBParameterGroup_UpdateGroup() throws Exception {
        // GIVEN
        final String paramGroupFile = "/rds/parameter-group-parameters.json";
        final String dbId = "lmb-app-ui-dev";

        RdsInstance rds = new RdsInstance();
        rds.setEngine("oracle");
        rds.setEngineVersion("12.4.5");
        rds.setParameterGroupFile(paramGroupFile);
        rds.setDBInstanceIdentifier(dbId);

        List<Tag> tags = Collections.EMPTY_LIST;
        DBInstance rdsResult = new DBInstance();
        rdsResult.setDBParameterGroups(Collections.EMPTY_LIST);
        rdsResult.setDBInstanceIdentifier(dbId);
        rdsResult.setDBInstanceStatus("available");
        rdsResult.setPendingModifiedValues(new PendingModifiedValues());
        Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbId)))
            .thenReturn(new DescribeDBInstancesResult().withDBInstances(rdsResult));

        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();

        // Set up Param group
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult()
            .withDBEngineVersions(new DBEngineVersion().withDBParameterGroupFamily("oracle-12"));
        Mockito.when(client.describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class)))
            .thenReturn(describeDBEngineVersionsResult);
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbParameterGroupName = String
            .format("%s-%s", dbEngineVersion.getDBParameterGroupFamily(), rdsResult.getDBInstanceIdentifier());

        // Set mock option group options
        URL template = this.getClass().getResource(paramGroupFile);
        String paramGroupJson = FileUtils.readFileToString(new File(template.getFile()));
        Mockito.when(fileUtil.findFile(rds.getParameterGroupFile(), false)).thenReturn(paramGroupJson);

        List<Parameter> parameters = objectMapper.readValue(paramGroupJson, new TypeReference<List<Parameter>>() {});

        Mockito.when(client.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenReturn(
            new DescribeDBParameterGroupsResult()
                .withDBParameterGroups(new DBParameterGroup().withDBParameterGroupName(dbParameterGroupName)));

        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        RdsClient rdsClient = initClient(definition, tags);

        // WHEN
        rdsClient.setDBParameterGroup(rds.getDBInstanceIdentifier(), parameters);

        // THEN
        Mockito.verify(client, times(1)).describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class));
        Mockito.verify(client, times(1)).describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class));
        Mockito.verify(client, times(0)).createDBParameterGroup(any(CreateDBParameterGroupRequest.class));
        Mockito.verify(client, times(1)).modifyDBInstance(any(ModifyDBInstanceRequest.class));
    }
}
