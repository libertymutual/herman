package com.libertymutualgroup.herman.task.lambda;

import static org.mockito.Mockito.mock;

import com.atlassian.bamboo.variable.CustomVariableContext;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import org.junit.Assert;
import org.junit.Test;

public class LambdaCreateTaskTest {

    @Test
    public void getTaskProperties() {
        // GIVEN
        LambdaCreateTask task = new LambdaCreateTask(mock(CustomVariableContext.class));

        // WHEN
        CommonTaskProperties lambdaCreateTaskProperties = task.getTaskProperties();

        // THEN
        Assert.assertNotNull(lambdaCreateTaskProperties);

        Assert.assertEquals("company-test-1", lambdaCreateTaskProperties.getCompany());
        Assert.assertEquals("sbu-test-1", lambdaCreateTaskProperties.getSbu());
        Assert.assertEquals("org-test-1", lambdaCreateTaskProperties.getOrg());
    }
}