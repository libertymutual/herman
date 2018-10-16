package com.libertymutualgroup.herman.task.cli;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Broker;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateContext;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.cli.s3.S3CreateTask;
import com.libertymutualgroup.herman.task.cli.s3.S3CreateTaskConfiguration;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class S3CreateTaskTest {

    @Test
    public void shouldCallBroker(){

        S3CreateContext fakeContext = new S3CreateContext();
        S3Broker fakeBroker = mock(S3Broker.class);
        S3CreateTask task = spy(new S3CreateTask(new SysoutLogger()));

        doReturn(fakeContext).when(task).initializeContext(any(), any(), any(), any());
        doReturn(fakeBroker).when(task).initializeBroker(fakeContext);

        S3CreateTaskConfiguration config = new S3CreateTaskConfiguration();

        task.runTask(config);

        verify(task).initializeContext(any(), any(), any(), any());
        verify(fakeBroker).brokerFromConfigurationFile();

    }

    @Test
    public void shouldCreateContext(){
        AWSCredentials creds = CredentialsHandler.getCredentials();
        S3CreateTaskConfiguration config = new S3CreateTaskConfiguration()
                .withRegion(Regions.US_EAST_1)
                .withRootPath("test1");
        HermanLogger logger = new SysoutLogger();
        S3CreateTask task = new S3CreateTask(logger);
        S3CreateContext context = task.initializeContext(
                new CliPropertyHandler(logger, "test", "", new HashMap<String, String>()),
                logger,
                config,
                creds);
        assertEquals(context.getRootPath(), "test1");
        assertEquals(context.getRegion(), Regions.US_EAST_1);
    }

}
