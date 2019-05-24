package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.custom.CustomBrokerResponse.Status;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import com.sun.syndication.io.impl.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;

public class CustomBrokerTest {
    @Mock
    AWSLambdaAsync lambdaClient;
    @Mock
    EcsPushContext pushContext;
    private PropertyHandler propertyHandler;
    private HermanLogger logger = new SysoutLogger();
    private EcsPushDefinition pushDefinition;
    private EcsClusterMetadata clusterMetadata;
    private CustomBrokerConfiguration config;
    private ObjectMapper mapper = new ObjectMapper();
    private Object brokerDefinition;
    private String brokerName = "herman-custom-broker";

    @Before
    public void setup() throws Exception
    {
        propertyHandler = new CliPropertyHandler(logger, "test", ".", new HashMap<>());
        propertyHandler.addProperty("env.var", "envvarvalue");
        pushDefinition = loadTemplate("template.yml", propertyHandler);
        clusterMetadata = new EcsClusterMetadata();
        ECSPushTaskProperties ecsPushTaskProperties = loadTaskProperties("properties.yml", propertyHandler);
        config = ecsPushTaskProperties.getCustomBrokers().get(brokerName);
        brokerDefinition = pushDefinition.getCustomBrokers().get(brokerName);
        MockitoAnnotations.initMocks(this);
        Mockito.when(pushContext.getPropertyHandler()).thenReturn(propertyHandler);
        Mockito.when(pushContext.getLogger()).thenReturn(logger);
    }

    @Test
    public void shouldIncludePropertiesFromPropertyHandler() throws IOException {
        CustomBrokerResponse response = new CustomBrokerResponse();
        response.setStatus(Status.SUCCESS);

        Future<InvokeResult> resultFuture = CompletableFuture.completedFuture(
            new InvokeResult()
                .withPayload(ByteBuffer.wrap(mapper.writeValueAsBytes(response)))
                .withLogResult(Base64.encode("lambda log result"))
        );

        Mockito.when(lambdaClient.invokeAsync(any(InvokeRequest.class)))
            .thenReturn(resultFuture);

        CustomBroker broker = new CustomBroker(
            brokerName,
            brokerDefinition,
            pushContext,
            pushDefinition,
            clusterMetadata,
            config,
            lambdaClient
        );
        broker.runBroker();
    }

    private EcsPushDefinition loadTemplate(String templateName, PropertyHandler propertyHandler) throws IOException {
        URL template = this.getClass().getResource("/sampleTemplates/" + templateName);
        String temp = FileUtils.readFileToString(new File(template.getFile()));
        EcsDefinitionParser parser = new EcsDefinitionParser(propertyHandler);
        return parser.parse(temp, false);
    }

    private ECSPushTaskProperties loadTaskProperties(String templateName, PropertyHandler propertyHandler) throws IOException {
        URL template = this.getClass().getResource("/sampleTemplates/" + templateName);
        String temp = FileUtils.readFileToString(new File(template.getFile()));
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        return objectMapper.readValue(
            propertyHandler.mapInProperties(temp),
            ECSPushTaskProperties.class);
    }
}
