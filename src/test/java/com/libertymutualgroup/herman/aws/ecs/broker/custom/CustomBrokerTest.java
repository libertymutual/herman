package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.lambda.AWSLambdaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomBrokerTest {
    private CustomBroker broker;

    @Mock
    AWSLambdaClient client;

    @Mock
    EcsPushContext pushContext;

    PropertyHandler propertyHandler;

    HermanLogger logger = new SysoutLogger();

    Map<String,String> customVariables = new HashMap<>();

    List<String> variablesToPass = new ArrayList<>();

    @Before
    public void setup() throws Exception
    {
        propertyHandler = new CliPropertyHandler(logger, "test", ".", customVariables);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        EcsPushDefinition definition = loadTemplate("template.yml");
        CustomBrokerConfiguration config = new CustomBrokerConfiguration(CustomBrokerRuntime.LABMDA, "test", variablesToPass);
        MockitoAnnotations.initMocks(this);
        Mockito.when(pushContext.getPropertyHandler()).thenReturn(propertyHandler);
        Mockito.when(pushContext.getLogger()).thenReturn(logger);
        broker = new CustomBroker(pushContext, definition, config, client);
    }

    @Test
    public void shouldRunBroker(){
        propertyHandler.addProperty("bamboo.papi-index", "somevalue");
        variablesToPass.add("bamboo.papi-index");
        broker.runBroker();
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
}
