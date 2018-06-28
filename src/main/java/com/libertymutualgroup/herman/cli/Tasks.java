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
package com.libertymutualgroup.herman.cli;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Tasks {
    ECS_PUSH("ecs-push"),
    CFT_PUSH("cft-push"),
    ECR_REPO_CREATE("ecr-repo-create"),
    LAMBDA_CREATE("lambda-create"),
    NEW_RELIC_DEPLOYMENT("newrelic-deployment"),
    S3_CREATE("s3_create");

    private String commandInput;

    Tasks(String commandInput) {
        this.commandInput = commandInput;
    }

    public String getText() {
        return this.commandInput;
    }

    public static Tasks fromString(String commandInput) {
        for (Tasks task: Tasks.values()) {
            if (task.commandInput.equalsIgnoreCase(commandInput)) {
                return task;
            }
        }
        return null;
    }

    public static List<String> getOptions() {
        return Arrays.stream(Tasks.values()).map(Tasks::getText).collect(Collectors.toList());
    }
}
