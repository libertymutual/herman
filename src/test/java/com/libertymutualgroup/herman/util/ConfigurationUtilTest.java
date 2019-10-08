package com.libertymutualgroup.herman.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSSecurityTokenServiceClientBuilder.class, BambooCredentialsHandler.class})
public class ConfigurationUtilTest {

    @Mock
    HermanLogger hermanLogger;

    @Mock
    AWSCredentials awsCredentials;

    @Mock
    AWSSecurityTokenService stsClient;

    @Mock
    GetCallerIdentityResult resultClient;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(AWSSecurityTokenServiceClientBuilder.class);
        PowerMockito.mockStatic(BambooCredentialsHandler.class);

        ClientConfiguration clientConfig = PowerMockito.mock(ClientConfiguration.class);
        PowerMockito.when(BambooCredentialsHandler.getConfiguration()).thenReturn(clientConfig);

        AWSSecurityTokenServiceClientBuilder builder = PowerMockito.mock(AWSSecurityTokenServiceClientBuilder.class);

        PowerMockito.when(AWSSecurityTokenServiceClientBuilder.standard()).thenReturn(builder);
        PowerMockito.when(builder.withCredentials(Mockito.any())).thenReturn(builder);
        PowerMockito.when(builder.withClientConfiguration(clientConfig)).thenReturn(builder);
        PowerMockito.when(builder.build()).thenReturn(stsClient);

        when(stsClient.getCallerIdentity(Mockito.any())).thenReturn(resultClient);
        when(resultClient.getAccount()).thenReturn("1234567890");
        when(resultClient.getUserId()).thenReturn("test-aws-user");
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenHermanConfigFileOrBucketDoNotExist() {

        // Exception is thrown
        exceptionRule.expect(RuntimeException.class);

        // Exception message contains AWS account info
        exceptionRule.expectMessage("ERROR: This AWS Account is not configured for Herman! Please ensure that the appropriate configuration bucket and files are in place, or that you're using the correct AWS Account.\n" +
                "Requested configuration file: properties.yml\n" +
                "AWS Account ID: 1234567890\n" +
                "AWS User ID: test-aws-user");

        ConfigurationUtil.getHermanConfigurationAsString(awsCredentials, hermanLogger, Regions.US_EAST_1);
    }
}
