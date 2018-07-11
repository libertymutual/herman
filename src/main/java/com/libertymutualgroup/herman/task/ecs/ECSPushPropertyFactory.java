/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.task.ecs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;

public class ECSPushPropertyFactory {

    public static ECSPushTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region) {
        return getTaskProperties(sessionCredentials, hermanLogger, null, region);
    }

    public static ECSPushTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, String customConfigurationBucket, Regions region) {
        try {
            String ecsPushTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, customConfigurationBucket, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(ecsPushTaskPropertiesYml, ECSPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting ECS Push Task Properties", ex);
        }
    }
}
