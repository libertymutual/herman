package com.libertymutualgroup.herman.aws.ecs;

import com.libertymutualgroup.herman.logging.SysoutLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

public class PropertyHandlerTest {
    private PropertyHandler handler;
    private String value = "value";

    @Before
    public void setup() {
        handler = new CliPropertyHandler(
            new SysoutLogger(),
            "junit",
            ".",
            new HashMap<>()
        );
    }

    @Test
    public void shouldMapSingleProperty() {
        handler.addProperty("prop", value);
        String template = "{\"property\": \"${prop}\"}";
        String result = handler.mapInProperties(template);
        Assert.assertEquals("{\"property\": \"" + value + "\"}", result);
    }

    @Test
    public void shouldMapNestedProperty() {
        handler.addProperty("prop", "prop");
        handler.addProperty("test-prop", value);
        String template = "{\"property\": \"${test-${prop}}\"}";
        String result = handler.mapInProperties(template);
        Assert.assertEquals("{\"property\": \"" + value + "\"}", result);
    }

    @Test
    public void shouldLoadPropertiesFromFile() throws Exception {
        File file = new File("junit.properties");
        file.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getName()));
        writer.write("prop=" + value);
        writer.close();

        String template = "{\"property\": \"${prop}\"}";
        String result = handler.mapInProperties(template);
        Assert.assertEquals("{\"property\": \"" + value + "\"}", result);
    }
}
