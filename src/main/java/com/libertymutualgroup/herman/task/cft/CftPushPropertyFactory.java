/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 10/5/18
 */
package com.libertymutualgroup.herman.task.cft;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;

public class CftPushPropertyFactory {
    private CftPushPropertyFactory() {
        throw new IllegalAccessError("Utility class");
    }

    public static CftPushTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region, PropertyHandler handler) {
        try {
            String cftPushTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(handler.mapInProperties(cftPushTaskPropertiesYml), CftPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting Cft Push Task Properties", ex);
        }
    }
}
