package com.libertymutualgroup.herman.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.HashMap;
import java.util.Map;

public class PropertyHandlerUtil {

    public static PropertyHandler getTaskContextPropertyHandler(DeploymentTaskContext taskContext,
        AWSCredentials sessionCredentials, CustomVariableContext customVariableContext) {
        final PropertyHandler handler = new TaskContextPropertyHandler(taskContext, customVariableContext);
        PropertyHandlerUtil.addStandardProperties(sessionCredentials, handler);
        return handler;
    }

    public PropertyHandler getCliPropertyHandler(AWSCredentials sessionCredentials, HermanLogger logger,
            String environmentName, String rootDirectory, Map<String, String> customVariables) {
        final PropertyHandler handler = new CliPropertyHandler(logger, environmentName, rootDirectory, customVariables);
        PropertyHandlerUtil.addStandardProperties(sessionCredentials, handler);
        return handler;
    }

    public static void addStandardProperties(AWSCredentials sessionCredentials, PropertyHandler propertyHandler){
        String accountId = getAccountId(sessionCredentials);
        propertyHandler.addProperty("account.id", accountId);
    }

    public static String getAccountId(AWSCredentials sessionCredentials){
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .withClientConfiguration(new ClientConfiguration().withMaxErrorRetry(10)).build();
        return stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
    }
}
