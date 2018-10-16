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
package com.libertymutualgroup.herman.task.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import com.libertymutualgroup.herman.util.ConfigurationUtil;

public class S3CreatePropertyFactory {

    private S3CreatePropertyFactory() {
        throw new IllegalAccessError("Utility class");
    }

//    public static S3CreateTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region) {
//        return getTaskProperties(sessionCredentials, hermanLogger, null, region);
//    }

    public static S3CreateTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region) {
        try {
            String s3CreateTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(s3CreateTaskPropertiesYml, S3CreateTaskProperties.class);
        } catch (Exception ex) {
            hermanLogger.addErrorLogEntry("Error getting S3 Create Task Task Properties. Continuing...", ex);
            return null;
        }
    }
}
