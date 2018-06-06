package com.libertymutualgroup.herman.task.s3;

import static org.mockito.Mockito.mock;

import com.atlassian.bamboo.variable.CustomVariableContext;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import org.junit.Assert;
import org.junit.Test;

public class S3CreateTaskTest {

    @Test
    public void getTaskProperties() {
        // GIVEN
        S3CreateTask task = new S3CreateTask(mock(CustomVariableContext.class));

        // WHEN
        CommonTaskProperties taskProperties = task.getTaskProperties();

        // THEN
        Assert.assertNotNull(taskProperties);

        Assert.assertEquals("company-test-1", taskProperties.getCompany());
        Assert.assertEquals("sbu-test-1", taskProperties.getSbu());
        Assert.assertEquals("org-test-1", taskProperties.getOrg());
    }
}