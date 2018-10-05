package com.libertymutualgroup.herman.util;

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
import java.util.Map;

public class PropertyHandlerUtil {

    private PropertyHandlerUtil() {
        throw new IllegalAccessError("Utility class");
    }

    public static PropertyHandler getTaskContextPropertyHandler(DeploymentTaskContext taskContext,
        AWSCredentials sessionCredentials, CustomVariableContext customVariableContext) {
        final PropertyHandler handler = new TaskContextPropertyHandler(taskContext, customVariableContext);
        PropertyHandlerUtil.addStandardProperties(sessionCredentials, handler);
        return handler;
    }

    private static void addStandardProperties(AWSCredentials sessionCredentials, PropertyHandler handler) {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
            .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
        String accountId = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
        handler.addProperty("account.id", accountId);
    }

    public static PropertyHandler getCliPropertyHandler(AWSCredentials sessionCredentials, HermanLogger logger,
            String environmentName, String rootDirectory, Map<String, String> customVariables) {
        final PropertyHandler handler = new CliPropertyHandler(logger, environmentName, rootDirectory, customVariables);
        PropertyHandlerUtil.addStandardProperties(sessionCredentials, handler);
        return handler;
    }
}
