/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 3/18/18
 */
package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.PendingModifiedValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import java.util.ArrayList;

public class RdsCommonTestObjects {

    public static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(Parameter.class, RdsBrokerMixIns.ParameterMixIn.class);
        return objectMapper;
    }

    public static EcsPushDefinition ecsPushDefinition(RdsInstance rds) {
        EcsPushDefinition definition = new EcsPushDefinition();
        definition.setDatabase(rds);

        return definition;
    }

    public static DBInstance initDbInstance() {
        DBInstance instance = new DBInstance();
        instance.setDBInstanceStatus("available");
        instance.setPendingModifiedValues(new PendingModifiedValues());
        Endpoint endpoint = new Endpoint();
        endpoint.setPort(3306);
        instance.setEndpoint(endpoint);

        return instance;
    }

    public static DBCluster initDbCluster() {
        ArrayList<String> availabilityZones = new ArrayList<>();
        availabilityZones.add("us-east-1a");
        availabilityZones.add("us-east-1c");

        DBCluster cluster = new DBCluster();
        cluster.setStatus("available");
        cluster.setPort(3306);
        cluster.setAvailabilityZones(availabilityZones);
        return cluster;
    }
}
