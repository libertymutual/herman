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
package com.libertymutualgroup.herman.aws.ecs;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.PortMapping;
import com.libertymutualgroup.herman.aws.AwsExecException;

public class EcsPortHandler {

    private static final int DOCKER_MIN_PORT = 8080;
    private static final int DOCKER_MAX_PORT = 32768;

    public TaskType getTaskType(EcsPushDefinition definition) {
        if (definition.getService() == null) {
            return TaskType.TASK;
        } else if (definition.getService().getUrlSuffix() == null) {
            return TaskType.DAEMON;
        } else if (definition.getService().getUrlSuffix() != null) {
            return TaskType.WEB;
        } else {
            throw new AwsExecException("Unable to determine if Task/Daemon/Web definition");
        }

    }

    public HermanContainerDefinition findContainerWithExposedPort(EcsPushDefinition definition, boolean isAlb) {
        if (!getTaskType(definition).equals(TaskType.WEB)) {
            throw new AwsExecException("Trying to run a ELB for non-configured task: bug in runner");
        }
        HermanContainerDefinition container = null;
        for (HermanContainerDefinition containerDefinition : definition.getContainerDefinitions()) {
            if (containerDefinition.getPortMappings() != null && !containerDefinition.getPortMappings().isEmpty()) {
                for (PortMapping p : containerDefinition.getPortMappings()) {
                    if (p.getHostPort() == 0) {
                        if (container == null) {
                            container = containerDefinition;
                        } else {
                            throw new AwsExecException("Multiple loadbalancer ports found (hostPort mapped as 0)");
                        }
                    }
                }
            }
        }
        if(container!=null){
            if(!isAlb){
                int randomPort = (int) ((Math.random() * (DOCKER_MAX_PORT - DOCKER_MIN_PORT)) + DOCKER_MIN_PORT);
                container.getPortMappings().get(0).setHostPort(randomPort);
            }
        } else {
            throw new AwsExecException("Url mapped for loadbalancer, but no hostPort of 0 found to connect with");
        }
        return container;

    }

}
