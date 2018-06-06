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
package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Tag;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import java.util.List;

public class OracleClient extends StandardRdsClient {

    private BuildLogger logger;

    OracleClient(AmazonRDS client, RdsInstance rds, EcsClusterMetadata clusterMetadata, List<Tag> tags,
        BuildLogger logger) {
        super(client, rds, clusterMetadata, tags, logger);
        this.logger = logger;
    }

    @Override
    public void createNewDb(String instanceId, String masterUserPassword) {
        logger.addBuildLogEntry("CREATE not implemented for Oracle");
        throw new UnsupportedOperationException();
    }
}
