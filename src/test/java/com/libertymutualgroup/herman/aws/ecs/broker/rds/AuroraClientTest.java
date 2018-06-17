package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.CreateDBClusterParameterGroupRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterParameterGroup;
import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DBParameterGroupNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBClusterParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBClusterRequest;
import com.amazonaws.services.rds.model.Parameter;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

public class AuroraClientTest {

    @Mock
    FileUtil fileUtil;
    @Mock
    private AmazonRDS client;
    @Mock
    private EcsClusterMetadata clusterMetadata;
    @Mock
    private HermanLogger logger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DescribeDBInstancesResult result = new DescribeDBInstancesResult();
        result.setDBInstances(Arrays.asList(RdsCommonTestObjects.initDbInstance()));
        Mockito.when(client.describeDBInstances(Mockito.any())).thenReturn(result);

        DescribeDBClustersResult clusterResult = new DescribeDBClustersResult();
        clusterResult.setDBClusters(Arrays.asList(RdsCommonTestObjects.initDbCluster()));
        Mockito.when(client.describeDBClusters(Mockito.any())).thenReturn(clusterResult);
    }

    private AuroraClient initClient(EcsPushDefinition definition, List<Tag> tags) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        RdsBroker.pollingIntervalMs = 0;
        definition.getDatabase().setAvailabilityZones(new String[]{"us-east-1a", "us-east-1c"});
        return new AuroraClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotHaveOptionGroupsImplemented() {
        // GIVEN
        final String engine = "aurora-mysql";

        RdsInstance rds = new RdsInstance();
        rds.setEngine(engine);
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);

        List<Tag> tags = new ArrayList<>();
        RdsClient rdsClient = initClient(definition, tags);

        rdsClient.setOptionGroup("test", null);
    }

    @Test
    public void setDBParameterGroup_CreateGroup() throws Exception {
        // GIVEN
        final String paramGroupFile = "/rds/aurora-parameter-group-parameters.json";
        final String dbId = "lmb-app-ui-dev";

        RdsInstance rds = new RdsInstance();
        rds.setEngine("aurora-mysql");
        rds.setEngineVersion("5.7.12");
        rds.setParameterGroupFile(paramGroupFile);
        rds.setDBInstanceIdentifier(dbId);
        rds.setDBClusterIdentifier(dbId);

        List<Tag> tags = Collections.EMPTY_LIST;
        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();
        DBCluster dbcResult = new DBCluster();
        dbcResult.setDBClusterParameterGroup(null);
        dbcResult.setDBClusterIdentifier(dbId);
        dbcResult.setStatus("available");

        Mockito.when(client.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(dbId)))
            .thenReturn(new DescribeDBClustersResult().withDBClusters(dbcResult));

        // Set up Param group
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult()
            .withDBEngineVersions(new DBEngineVersion().withDBParameterGroupFamily("aurora-mysql5-7"));
        Mockito.when(client.describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class)))
            .thenReturn(describeDBEngineVersionsResult);
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbParameterGroupName = String.format("%s-%s", dbEngineVersion.getDBParameterGroupFamily(),
            dbcResult.getDBClusterIdentifier());

        // Set mock option group options
        URL template = this.getClass().getResource(paramGroupFile);
        String paramGroupJson = FileUtils.readFileToString(new File(template.getFile()));
        Mockito.when(fileUtil.findFile(rds.getParameterGroupFile(), false)).thenReturn(paramGroupJson);

        List<Parameter> parameters = objectMapper.readValue(paramGroupJson, new TypeReference<List<Parameter>>() {
        });

        // Throw DescribeDBParameterGroupsRequest when getting group - Trigger
        // create
        Mockito.when(client.describeDBClusterParameterGroups(any(DescribeDBClusterParameterGroupsRequest.class)))
            .thenThrow(DBParameterGroupNotFoundException.class);

        // Set mock DB Engine Version
        Mockito.when(client.createDBClusterParameterGroup(any(CreateDBClusterParameterGroupRequest.class)))
            .thenReturn(new DBClusterParameterGroup().withDBClusterParameterGroupName(rds.getDBInstanceIdentifier()));
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        RdsClient rdsClient = initClient(definition, tags);

        // WHEN
        rdsClient.setDBParameterGroup(rds.getDBClusterIdentifier(), parameters);

        // THEN
        Mockito.verify(client, times(1))
            .describeDBClusterParameterGroups(any(DescribeDBClusterParameterGroupsRequest.class));
        Mockito.verify(client, times(1)).describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class));
        Mockito.verify(client, times(1))
            .createDBClusterParameterGroup(new CreateDBClusterParameterGroupRequest()
                .withDBParameterGroupFamily("aurora-mysql5-7").withDBClusterParameterGroupName(dbParameterGroupName)
                .withDescription(String.format("%s %s Parameter Group", rds.getDBInstanceIdentifier(),
                    dbEngineVersion.getDBParameterGroupFamily()))
                .withTags(tags));
        Mockito.verify(client, times(1)).modifyDBCluster(any(ModifyDBClusterRequest.class));
    }

    @Test
    public void setDBParameterGroup_UpdateGroup() throws Exception {
        // GIVEN
        final String paramGroupFile = "/rds/aurora-parameter-group-parameters.json";
        final String dbId = "lmb-app-ui-dev";

        RdsInstance rds = new RdsInstance();
        rds.setEngine("aurora-mysql");
        rds.setEngineVersion("5.7.12");
        rds.setParameterGroupFile(paramGroupFile);
        rds.setDBInstanceIdentifier(dbId);
        rds.setDBClusterIdentifier(dbId);

        List<Tag> tags = Collections.EMPTY_LIST;
        DBCluster dbcResult = new DBCluster();
        dbcResult.setDBClusterParameterGroup(null);
        dbcResult.setDBClusterIdentifier(dbId);
        dbcResult.setStatus("available");

        Mockito.when(client.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(dbId)))
            .thenReturn(new DescribeDBClustersResult().withDBClusters(dbcResult));

        ObjectMapper objectMapper = RdsCommonTestObjects.objectMapper();

        // Set up Param group
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult()
            .withDBEngineVersions(new DBEngineVersion().withDBParameterGroupFamily("aurora-mysql5-7"));
        Mockito.when(client.describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class)))
            .thenReturn(describeDBEngineVersionsResult);
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbParameterGroupName = String.format("%s-%s", dbEngineVersion.getDBParameterGroupFamily(),
            dbcResult.getDBClusterIdentifier());

        // Set mock option group options
        URL template = this.getClass().getResource(paramGroupFile);
        String paramGroupJson = FileUtils.readFileToString(new File(template.getFile()));
        Mockito.when(fileUtil.findFile(rds.getParameterGroupFile(), false)).thenReturn(paramGroupJson);

        List<Parameter> parameters = objectMapper.readValue(paramGroupJson, new TypeReference<List<Parameter>>() {
        });

        Mockito.when(client.describeDBClusterParameterGroups(any(DescribeDBClusterParameterGroupsRequest.class)))
            .thenReturn(new DescribeDBClusterParameterGroupsResult()
                .withDBClusterParameterGroups(
                    new DBClusterParameterGroup().withDBClusterParameterGroupName(dbParameterGroupName)));

        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);
        RdsClient rdsClient = initClient(definition, tags);

        // WHEN
        rdsClient.setDBParameterGroup(rds.getDBInstanceIdentifier(), parameters);

        // THEN
        Mockito.verify(client, times(1))
            .describeDBClusterParameterGroups(any(DescribeDBClusterParameterGroupsRequest.class));
        Mockito.verify(client, times(1)).describeDBEngineVersions(any(DescribeDBEngineVersionsRequest.class));
        Mockito.verify(client, times(0)).createDBClusterParameterGroup(any(CreateDBClusterParameterGroupRequest.class));
        Mockito.verify(client, times(1)).modifyDBCluster(any(ModifyDBClusterRequest.class));
    }

}
