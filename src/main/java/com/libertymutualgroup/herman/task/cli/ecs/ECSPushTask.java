/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 6/25/18
 */
package com.libertymutualgroup.herman.task.cli.ecs;

import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushPropertyFactory;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;

import java.util.Map;

public class ECSPushTask {
    private HermanLogger logger;

    public ECSPushTask(HermanLogger logger) {
        this.logger = logger;
    }

    public void doExecute(String rootPath, int timeout, String environmentName, Map<String, String> customVariables) {
        ECSPushTaskProperties taskProperties = ECSPushPropertyFactory.getTaskProperties();

        PropertyHandler propertyHandler = new CliPropertyHandler(logger, environmentName, rootPath, customVariables);
    }


}
