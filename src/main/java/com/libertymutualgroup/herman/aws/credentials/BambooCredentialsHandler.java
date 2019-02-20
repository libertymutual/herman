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
package com.libertymutualgroup.herman.aws.credentials;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;

import java.util.Map;

public class BambooCredentialsHandler extends CredentialsHandler {

    private BambooCredentialsHandler() {
        throw new IllegalAccessError("Utility class");
    }

    public static AWSCredentials getCredentials(CommonTaskContext context) {
        final AtlassianBuildLogger logger = new AtlassianBuildLogger(context.getBuildLogger());
        logger.addLogEntry("Looking for credentials...");

        if (lookupVar("custom.aws.accessKeyId", context) != null) {
            logger.addLogEntry("Found credentials using custom.aws.accessKeyId");
            return new BasicSessionCredentials(lookupVar("custom.aws.accessKeyId", context),
                lookupVar("custom.aws.secretAccessKey.password", context),
                lookupVar("custom.aws.sessionToken.password", context));

        } else if (lookupVar("secret.sts.accessKeyId", context) != null) {
            logger.addLogEntry("Found credentials using secret.sts.accessKeyId");
            return new BasicSessionCredentials(lookupVar("secret.sts.accessKeyId", context),
                lookupVar("secret.sts.secretAccessKey", context),
                lookupVar("secret.sts.sessionToken", context));

        } else {
            logger.addLogEntry("Could not find credentials as custom variables - falling back to instance profile.");
            try (InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.getInstance()) {
                return provider.getCredentials();
            } catch (Exception ex) {
                throw new RuntimeException("Error getting instance profile creds provider", ex);
            }
        }
    }

    public static ClientConfiguration getConfiguration() {
        return new ClientConfiguration().withMaxErrorRetry(10);
    }

    private static String lookupVar(String key, CommonTaskContext context) {
        VariableContext vars = context.getCommonContext().getVariableContext();
        String value = lookupVar(vars.getEffectiveVariables(), key);

        if (value == null) {
            value = lookupVar(vars.getResultVariables(), key);
        }
        if (value == null) {
            value = lookupVar(vars.getEffectiveVariables(), key);
        }
        return value;
    }

    private static String lookupVar(Map<String, VariableDefinitionContext> map, String key) {
        VariableDefinitionContext ctxt = map.get(key);
        if (ctxt != null) {
            return ctxt.getValue();
        } else {
            return null;
        }
    }
}
