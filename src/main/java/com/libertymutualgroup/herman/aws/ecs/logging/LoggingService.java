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
package com.libertymutualgroup.herman.aws.ecs.logging;

import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.util.Assert;

public class LoggingService {

    private final BuildLogger logger;
    private SplunkInstance splunkInstance = null;

    public LoggingService(BuildLogger logger) {
        this.logger = logger;
    }

    public LoggingService withSplunkInstanceValues(String splunkUrl, ECSPushTaskProperties taskProperties) {
        Assert.notNull(splunkUrl, "Splunk URL for the cluster must not be null");
        Assert.isTrue(!taskProperties.getSplunkInstances().isEmpty(),
            "There must be one or more splunk instances defined.");

        this.splunkInstance = taskProperties.getSplunkInstances().stream()
            .filter(aSplunkInstance -> aSplunkInstance.getHttpEventCollectorUrl().equals(splunkUrl))
            .findAny()
            .orElseThrow(() -> new RuntimeException(
                String.format("Splunk instance for %s could not be found.", splunkUrl)));
        return this;
    }

    public void provideSplunkLog(RegisterTaskDefinitionResult taskResult) {
        Assert.notNull(splunkInstance, "Splunk instance cannot be null");

        String family = taskResult.getTaskDefinition().getFamily();
        Integer version = taskResult.getTaskDefinition().getRevision();

        String link = String.format("%s/en-US/app/search/search?q=search%%20source%%3D*%s-%s*"
                + "&display.page.search.mode=smart&dispatch.sample_ratio=1&earliest=-15m&latest=now",
            splunkInstance.getWebUrl(),
            family,
            version);

        logSection("Splunk Logs", link);
    }

    public void logSection(String title, String link) {
        String titleEntry = "******  " + title + "  *******";
        String dividerEntry = IntStream.rangeClosed(1, titleEntry.length())
            .mapToObj(value -> "*")
            .collect(Collectors.joining());

        logger.addBuildLogEntry("\n");
        logger.addBuildLogEntry(dividerEntry);
        logger.addBuildLogEntry(titleEntry);
        logger.addBuildLogEntry(dividerEntry);
        logger.addBuildLogEntry(link);
        logger.addBuildLogEntry(dividerEntry);
    }
}