package com.libertymutualgroup.herman.aws.ecs;

import static org.junit.Assert.assertTrue;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;

public class TaskContextPropertyHandlerTest {

    @Test
    public void testPattern() throws Exception {

        // GIVEN
        String template = "{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"s3:PutObject\",\"s3:DeleteObject\"],\"Resource\":[\"arn:aws:s3:::${s3-cutover.source-bucket-name}/*\",\"arn:aws:s3:::${s3-cutover.destination-bucket-name}/*\"]}";
        TaskContextPropertyHandler handler = new TaskContextPropertyHandler(Mockito.mock(DeploymentTaskContext.class),
            Mockito.mock(CustomVariableContext.class));

        // WHEN
        Set<String> propertiesToMatch = handler.getPropertiesToMatch(template);

        // THEN
        assertTrue("propertiesToMatch contains ${s3-cutover.source-bucket-name}",
            propertiesToMatch.contains("${s3-cutover.source-bucket-name}"));
        assertTrue("propertiesToMatch contains ${s3-cutover.destination-bucket-name}",
            propertiesToMatch.contains("${s3-cutover.destination-bucket-name}"));
    }
}