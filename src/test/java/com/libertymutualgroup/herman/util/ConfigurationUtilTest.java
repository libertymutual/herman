package com.libertymutualgroup.herman.util;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

public class ConfigurationUtilTest {

    @Mock
    HermanLogger hermanLogger;

    @Mock
    AWSCredentials awsCredentials;

    ConfigurationUtil configurationUtil;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenHermanConfigFileOrBucketDoNotExist() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("ERROR: This AWS Account is not configured for Herman! Please ensure that the appropriate configuration bucket and files are in place, or that you're using the correct AWS Account.\n\nRequested configuration file: properties.yml");
        configurationUtil.getHermanConfigurationAsString(awsCredentials, hermanLogger, Regions.US_EAST_1);
    }
}
