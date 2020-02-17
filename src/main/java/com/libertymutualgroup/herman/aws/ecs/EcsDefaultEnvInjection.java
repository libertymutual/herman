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
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.libertymutualgroup.herman.aws.ecs.broker.rds.RdsInstance;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import java.util.List;

public class EcsDefaultEnvInjection {

    public void injectEnvironment(EcsPushDefinition definition, String region, String deployEnv,
        EcsClusterMetadata meta) {

        for (ContainerDefinition container : definition.getContainerDefinitions()) {
            inject(container, region);
        }

    }

    private void inject(ContainerDefinition def, String region) {
        def.getEnvironment().add(new KeyValuePair().withName("aws.region").withValue(region));

    }

    public void injectRds(EcsPushDefinition definition, RdsInstance rds) {
        for (ContainerDefinition def : definition.getContainerDefinitions()) {
            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getHost())
                .withValue(rds.getEndpoint().getAddress()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getPort())
                .withValue(rds.getEndpoint().getPort().toString()));

            def.getEnvironment()
                .add(new KeyValuePair().withName(rds.getInjectNames().getDb()).withValue(rds.getDBName()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getConnectionString())
                .withValue(rds.getConnectionString()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getDbResourceId())
                .withValue(rds.getDbiResourceId()));

            // liquibase apps likely need spring.datasource.url AND liquibase.url set
            if (rds.getInjectNames().getAdminUsername().toLowerCase().startsWith("liquibase")) {
                def.getEnvironment()
                    .add(new KeyValuePair().withName("liquibase.url").withValue(rds.getConnectionString()));
            }

            def.getEnvironment().add(
                new KeyValuePair().withName(rds.getInjectNames().getUsername()).withValue(rds.getMasterUsername()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getEncryptedPassword())
                .withValue(rds.getEncryptedPassword()));

            def.getEnvironment().add(
                new KeyValuePair().withName(rds.getInjectNames().getAppUsername()).withValue(rds.getAppUsername()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getAppEncryptedPassword())
                .withValue(rds.getAppEncryptedPassword()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getAdminUsername())
                .withValue(rds.getAdminUsername()));

            def.getEnvironment().add(new KeyValuePair().withName(rds.getInjectNames().getAdminEncryptedPassword())
                .withValue(rds.getAdminEncryptedPassword()));
        }
    }

    private boolean propExists(List<KeyValuePair> env, String prop) {
        for (KeyValuePair pair : env) {
            if (pair.getName().equals(prop)) {
                return true;
            }
        }
        return false;
    }

    public void setDefaultContainerName(EcsPushDefinition definition) {
        if (definition.getContainerDefinitions().size() != 1) {
            return;
        } else {
            if (definition.getContainerDefinitions().get(0).getName() == null) {
                definition.getContainerDefinitions().get(0).setName("default");
            }
        }
    }

}
