package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Tag;
import com.libertymutualgroup.herman.aws.ecs.EcsPush;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static com.libertymutualgroup.herman.aws.ecs.broker.rds.RdsCommonTestObjects.initDbInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

public class RdsBrokerTest {

    @Mock
    AmazonRDS client;
    @Mock
    AWSKMS kmsClient;
    @Mock
    EcsClusterMetadata clusterMetadata;
    @Mock
    PropertyHandler propertyHandler;
    @Mock
    EcsPushContext pushContext;
    @Mock
    HermanLogger logger;
    @Mock
    EcsPush ecsPush;
    @Mock
    EcsPushFactory pushFactory;
    @Mock
    FileUtil fileUtil;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ECSPushTaskProperties taskProperties = new ECSPushTaskProperties()
            .withCompany("lm")
            .withOrg("LMB")
            .withSbu("CI");

        Mockito.when(pushContext.getTaskProperties()).thenReturn(taskProperties);
        Mockito.when(pushContext.getLogger()).thenReturn(logger);
        Mockito.when(propertyHandler.lookupVariable("herman.rdsCredentialBrokerImage")).thenReturn("testCredentialBroker");
        Mockito.when(pushContext.getPropertyHandler()).thenReturn(propertyHandler);
        DescribeDBInstancesResult result = new DescribeDBInstancesResult();
        result.setDBInstances(Arrays.asList(initDbInstance()));
        Mockito.when(client.describeDBInstances(Mockito.any())).thenReturn(result);
        Mockito.when(pushFactory.createPush(any())).thenReturn(ecsPush);
    }

    private RdsBroker initBroker(EcsPushDefinition definition) {
        RdsBroker.pollingIntervalMs = 0;
        return new RdsBroker(pushContext, client, kmsClient, "123", definition, clusterMetadata, pushFactory, fileUtil);
    }

    private RdsClient initClient(EcsPushDefinition definition, ArrayList<Tag> tags) {
        RdsClient rdsClient;
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (definition.getDatabase().getEngine().contains("aurora")) {
            rdsClient = new AuroraClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
        } else if (definition.getDatabase().getEngine().contains("oracle")) {
            rdsClient = new OracleClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
        } else {
            rdsClient = new StandardRdsClient(client, definition.getDatabase(), clusterMetadata, tags, logger);
        }
        return rdsClient;
    }

    private void mockEncryptionResult(String result) {
        EncryptResult encResult = new EncryptResult();
        DecryptResult decResult = new DecryptResult();
        encResult.setCiphertextBlob(ByteBuffer.wrap(result.getBytes()));
        decResult.setPlaintext(ByteBuffer.wrap(result.getBytes()));
        Mockito.when(kmsClient.encrypt(Mockito.any())).thenReturn(encResult);
        Mockito.when(kmsClient.decrypt(Mockito.any())).thenReturn(decResult);
    }

    private RdsInstance initInstanceDefinition() {
        RdsInstance instance = new RdsInstance();
        instance.setEngine("mysql");
        instance.setEngineVersion("5.6");

        return instance;
    }

    @Test
    public void shouldNotBrokerCredentialsWhenIamAuthIsUsed() {
        EcsPushDefinition definition = new EcsPushDefinition();
        RdsInstance instance = initInstanceDefinition();
        instance.setAdminUsername("adminUser");
        instance.setIAMDatabaseAuthenticationEnabled(true);
        definition.setDatabase(instance);
        definition.setUseKms(Boolean.TRUE.toString());

        RdsBroker broker = initBroker(definition);
        RdsInstance result = broker.brokerDb();
        assertNotNull(result.getAdminUsername());
        Mockito.verify(ecsPush, never()).push();
    }

    @Test
    public void shouldNotBrokerCredentialsWhenUsingStaticPassword() {
        EcsPushDefinition definition = new EcsPushDefinition();
        RdsInstance instance = initInstanceDefinition();
        String encryptedPassword = "123";
        instance.setAppEncryptedPassword(encryptedPassword);
        definition.setDatabase(instance);
        mockEncryptionResult(encryptedPassword);
        definition.setUseKms(Boolean.TRUE.toString());
        RdsBroker broker = initBroker(definition);

        broker.brokerDb();
        Mockito.verify(ecsPush, never()).push();
    }

    @Test
    public void shouldBrokerCredentialsWhenUsingStaticPasswordWithFullUpdate() {
        EcsPushDefinition definition = new EcsPushDefinition();
        RdsInstance instance = initInstanceDefinition();
        String encryptedPassword = "123";
        instance.setEncryptedPassword(encryptedPassword);
        instance.setFullUpdate(true);
        definition.setDatabase(instance);
        mockEncryptionResult(encryptedPassword);
        definition.setUseKms(Boolean.TRUE.toString());
        RdsBroker broker = initBroker(definition);

        Mockito.when(client.modifyDBInstance(any()))
            .thenReturn(initDbInstance());

        RdsInstance result = broker.brokerDb();
        assertEquals(encryptedPassword, result.getEncryptedPassword());
        Mockito.verify(ecsPush).push();
    }

    @Test
    public void shouldBrokerCredentialsWhenDbIsNew() {
        EcsPushDefinition definition = new EcsPushDefinition();
        definition.setDatabase(initInstanceDefinition());
        RdsBroker broker = initBroker(definition);
        mockEncryptionResult("123");
        definition.setUseKms(Boolean.TRUE.toString());

        DescribeDBInstancesResult result = new DescribeDBInstancesResult();
        result.setDBInstances(Arrays.asList(initDbInstance()));

        Mockito.when(client.describeDBInstances(any()))
            .thenThrow(new DBInstanceNotFoundException(""))
            .thenReturn(result);

        broker.brokerDb();
        Mockito.verify(ecsPush).push();
    }

    @Test
    public void shouldUseCredPrefixWhenSpecified() {
        String prefix = "{cipher}";
        EcsPushDefinition definition = new EcsPushDefinition();
        RdsInstance instance = initInstanceDefinition();
        instance.setCredPrefix(prefix);
        definition.setDatabase(instance);

        RdsBroker broker = initBroker(definition);
        mockEncryptionResult("123");
        definition.setUseKms(Boolean.TRUE.toString());

        DescribeDBInstancesResult result = new DescribeDBInstancesResult();
        result.setDBInstances(Arrays.asList(initDbInstance()));

        Mockito.when(client.describeDBInstances(any()))
            .thenThrow(new DBInstanceNotFoundException(""))
            .thenReturn(result);

        RdsInstance rdsResult = broker.brokerDb();
        assertTrue(rdsResult.getAppEncryptedPassword().startsWith(prefix));
        assertTrue(rdsResult.getAdminEncryptedPassword().startsWith(prefix));
        assertTrue(rdsResult.getEncryptedPassword().startsWith(prefix));
    }
}
