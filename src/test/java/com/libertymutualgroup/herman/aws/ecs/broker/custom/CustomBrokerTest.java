package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.lambda.AWSLambdaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.CustomBrokerDefinition;
import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import edu.emory.mathcs.backport.java.util.Arrays;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomBrokerTest {
    @Mock
    AWSLambdaClient client;
    @Mock
    EcsPushContext pushContext;
    private PropertyHandler propertyHandler;
    private HermanLogger logger = new SysoutLogger();
    private Map<String,String> customVariables = new HashMap<>();
    private List<String> variablesToPass = new ArrayList<>();
    private Map<String,Object> defaults = new HashMap<>();
    private CustomBrokerDefinition customBrokerDefinition;
    private EcsPushDefinition pushDefinition;
    private CustomBrokerConfiguration config;
    private ObjectNode props;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() throws Exception
    {
        props = mapper.createObjectNode();
        customBrokerDefinition = new CustomBrokerDefinition("herman-rds-broker-papi", props);
        propertyHandler = new CliPropertyHandler(logger, "test", ".", customVariables);
        pushDefinition = loadTemplate("template.yml");
        config = new CustomBrokerConfiguration(CustomBrokerPhase.PREPUSH, variablesToPass, defaults);
        MockitoAnnotations.initMocks(this);
        Mockito.when(pushContext.getPropertyHandler()).thenReturn(propertyHandler);
        Mockito.when(pushContext.getLogger()).thenReturn(logger);
    }

    @Test
    public void shouldIncludePropertiesFromPropertyHandler() {
        variablesToPass.add("bamboo.secret.papi-index");
        variablesToPass.add("bamboo.secret.vault-token.token");
        variablesToPass.add("bamboo.forge.deployment.guid");

        ObjectNode database = mapper.createObjectNode();
        database.put("instanceName", "test");
        database.put("engine", "mysql");

        defaults.put("vpcId", "test");
        defaults.put("database", database);
        defaults.put("subnetIds", Arrays.asList(new String[] { "sub-1", "sub-2" }));

        ObjectNode databaseOverride = mapper.createObjectNode();
        database.put("instanceName", "override");
        props.put("vpcId", "override");
        props.set("database", databaseOverride);

        propertyHandler.addProperty("bamboo.secret.papi-index", "somevalue");
        propertyHandler.addProperty("bamboo.secret.vault-token.token", "somevalue");
        propertyHandler.addProperty("bamboo.forge.deployment.guid", "somevalue");

        CustomBroker broker = new CustomBroker(customBrokerDefinition, pushContext, pushDefinition, config, client);
        broker.runBroker();
    }

    private EcsPushDefinition loadTemplate(String templateName) throws IOException {
        URL template = this.getClass().getResource("/sampleTemplates/" + templateName);
        String temp = FileUtils.readFileToString(new File(template.getFile()));

        PropertyHandler ph = mock(TaskContextPropertyHandler.class);
        when(ph.mapInProperties(any())).thenReturn(temp);

        EcsDefinitionParser parser = new EcsDefinitionParser(ph);
        return parser.parse(temp, false);
    }

    private ECSPushTaskProperties loadTaskProperties(String templateName) throws IOException {
        URL template = this.getClass().getResource("/sampleTemplates/" + templateName);
        String temp = FileUtils.readFileToString(new File(template.getFile()));

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(
            temp,
            ECSPushTaskProperties.class);
    }
}
