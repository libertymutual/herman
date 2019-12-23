package com.libertymutualgroup.herman.aws.ecs;

import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class EcsDefinitionParserTest {
    PropertyHandler handler = new CliPropertyHandler(
        new SysoutLogger(),
        "junit",
        ".",
        new HashMap<>()
    );
    EcsDefinitionParser parser = new EcsDefinitionParser(handler);
    StringBuilder baseTemplate = new StringBuilder()
        .append("containerDefinitions: \n")
        .append("- memory: 1024\n")
        .append("  portMappings:\n")
        .append("  - hostPort: 0\n")
        .append("    containerPort: 8443\n");


    @Test(expected = AwsExecException.class)
    public void shouldFailWhenTemplatePropertyIsUndefined(){
        String template = baseTemplate.append("useKms: ${useKms}").toString();
        parser.parse(template, false);
    }

    @Test
    public void shouldMapInProperties(){
        String var = "testValue";
        handler.addProperty("myVar", var);
        String template = baseTemplate.append("appName: ${myVar}").toString();
        EcsPushDefinition def = parser.parse(template, false);
        Assert.assertEquals(var, def.getAppName());
    }

    @Test
    public void shouldNotMapPropertiesIntoComments(){
        String template = baseTemplate.append("# ${test}").toString();
        parser.parse(template, false);
    }

    @Test
    public void shouldNotIgnoreProtocolInPortMappings() {
        String template = baseTemplate.append("    protocol: udp\n").toString();
        EcsPushDefinition def = parser.parse(template, false);
        Assert.assertEquals("udp", def.getContainerDefinitions().get(0).getPortMappings().get(0).getProtocol());
    }
}
