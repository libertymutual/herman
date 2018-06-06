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

import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.OptionGroup;
import com.amazonaws.services.rds.model.Parameter;
import java.util.List;

public interface RdsClient {

    Boolean dbExists(String instanceId);

    void createNewDb(String instanceId, String masterUserPassword);

    void updateMasterPassword(String instanceId, String masterUserPassword);

    void runFullUpdate(String instanceId, String masterUserPassword);

    void waitForAvailableStatus(String instanceId);

    Endpoint getEndpoint(String instanceId);

    String getDbiResourceId(String instanceId);

    void setDBParameterGroup(String instanceId, List<Parameter> parameters);

    void setOptionGroup(String instanceId, OptionGroup options);

    void createSnapshot(String instanceId, String snapshotId);
}
