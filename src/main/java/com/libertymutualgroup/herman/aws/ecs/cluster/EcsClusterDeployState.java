/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 8/1/18
 */
package com.libertymutualgroup.herman.aws.ecs.cluster;

import com.amazonaws.services.cloudformation.model.Stack;

public class EcsClusterDeployState {
    private String clusterName;
    private Stack sharedStack;
    private Stack instanceStack;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public EcsClusterDeployState withClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public Stack getSharedStack() {
        return sharedStack;
    }

    public void setSharedStack(Stack sharedStack) {
        this.sharedStack = sharedStack;
    }

    public EcsClusterDeployState withSharedStack(Stack sharedStackName) {
        this.sharedStack = sharedStackName;
        return this;
    }

    public Stack getInstanceStack() {
        return instanceStack;
    }

    public void setInstanceStack(Stack instanceStack) {
        this.instanceStack = instanceStack;
    }

    public EcsClusterDeployState withInstanceStack(Stack instanceStackName) {
        this.instanceStack = instanceStackName;
        return this;
    }
}
