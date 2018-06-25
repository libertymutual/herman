/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 6/25/18
 */
package com.libertymutualgroup.herman.task.ecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class ECSPushPropertyFactory {
    private final static String TASK_CONFIG_FILE = "/config/plugin-tasks.yml";

    public static ECSPushTaskProperties getTaskProperties() {
        try {
            InputStream ecsPushTaskPropertiesStream = ECSPushPropertyFactory.class.getResourceAsStream(TASK_CONFIG_FILE);
            String ecsPushTaskPropertiesYml = IOUtils.toString(ecsPushTaskPropertiesStream);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(ecsPushTaskPropertiesYml, ECSPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting ECS Push Task Properties from " + TASK_CONFIG_FILE, ex);
        }
    }
}
