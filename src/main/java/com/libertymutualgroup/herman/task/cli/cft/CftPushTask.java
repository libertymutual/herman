/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 7/20/18
 */
package com.libertymutualgroup.herman.task.cli.cft;

import com.amazonaws.auth.AWSCredentials;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.cft.CftPush;
import com.libertymutualgroup.herman.aws.cft.CftPushContext;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.cft.CftPushPropertyFactory;
import com.libertymutualgroup.herman.task.cft.CftPushTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

public class CftPushTask {
    private HermanLogger logger;

    public CftPushTask(HermanLogger logger) {
        this.logger = logger;
    }

    public void runTask(CftPushTaskConfiguration configuration) {
        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();
        final PropertyHandler propertyHandler = PropertyHandlerUtil.getCliPropertyHandler(
            sessionCredentials,
            this.logger,
            configuration.getEnvironmentName(),
            configuration.getRootPath(),
            configuration.getCustomVariables()
        );
        CftPushTaskProperties taskProperties = CftPushPropertyFactory.getTaskProperties(sessionCredentials, this.logger, configuration.getRegion(), propertyHandler);
        CftPushContext cftPushContext = new CftPushContext()
            .withLogger(this.logger)
            .withEnvName(configuration.getEnvironmentName())
            .withRootPath(configuration.getRootPath())
            .withPropertyHandler(propertyHandler)
            .withSessionCredentials(sessionCredentials)
            .withAwsClientConfig(CredentialsHandler.getConfiguration())
            .withRegion(configuration.getRegion())
            .withTemplateParameters(configuration.getCustomVariables())
            .withTaskProperties(taskProperties);

        CftPush cftPush = new CftPush(cftPushContext);
        String template = getTemplate(configuration.getTemplateFile(), configuration.getRootPath());
        cftPush.push(configuration.getStackName(), template);
    }

    private String getTemplate(String templateName, String rootPath) {
        FileUtil fileUtil = new FileUtil(rootPath, this.logger);

        boolean fileExists = fileUtil.fileExists(templateName);
        if (!fileExists) {
            throw new AwsExecException("CloudFormation Template with name " + templateName + " not found!");
        }
        this.logger.addLogEntry("Found template: " + templateName);
        return fileUtil.findFile(templateName, false);
    }
}