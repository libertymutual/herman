package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Tag;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

public class OracleClientTest {

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
    }

    private StandardRdsClient initClient(EcsPushDefinition definition, List<Tag> tags) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return new OracleClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotCreateOracleDb() {
        // GIVEN
        final String engine = "oracle";

        RdsInstance rds = new RdsInstance();
        rds.setEngine(engine);
        EcsPushDefinition definition = RdsCommonTestObjects.ecsPushDefinition(rds);

        List<Tag> tags = new ArrayList<>();
        RdsClient rdsClient = initClient(definition, tags);

        rdsClient.createNewDb("test", "pw");

        Mockito.verify(client, times(0)).createDBInstance(any());
    }

}
