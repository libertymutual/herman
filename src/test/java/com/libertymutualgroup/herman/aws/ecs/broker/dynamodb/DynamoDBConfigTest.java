package com.libertymutualgroup.herman.aws.ecs.broker.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.libertymutualgroup.herman.aws.ecs.EcsDefinitionParser;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DynamoDBConfigTest {

    @Test
    public void shouldMapProperties() throws Exception {
        EcsPushDefinition ecsPushDefinition = loadTemplate("dynamo.json");
        List<DynamoDBTable> dynamoDBTables = ecsPushDefinition.getDynamoDBTables();
        assertTrue(dynamoDBTables.size() == 1);

        DynamoDBTable dynamoDBTable = dynamoDBTables.get(0);
        assertEquals("test-table", dynamoDBTable.getTableName());
        assertEquals(1, dynamoDBTable.getAttributes().size());
        assertEquals("test", dynamoDBTable.getAttributes().get(0).getAttributeName());
        assertEquals("S", dynamoDBTable.getAttributes().get(0).getAttributeType());
        assertEquals(5, dynamoDBTable.getProvisionedThroughput().getReadCapacityUnits().longValue());
        assertEquals(2, dynamoDBTable.getProvisionedThroughput().getWriteCapacityUnits().longValue());
        assertTrue(dynamoDBTable.getSseSpecification().isEnabled());
        assertTrue(dynamoDBTable.getStreamSpecification().isStreamEnabled());
        assertEquals("NEW_IMAGE", dynamoDBTable.getStreamSpecification().getStreamViewType());
        assertEquals(1, dynamoDBTable.getKeySchema().size());
        assertEquals("test", dynamoDBTable.getKeySchema().get(0).getAttributeName());
        assertEquals("HASH", dynamoDBTable.getKeySchema().get(0).getKeyType());
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
