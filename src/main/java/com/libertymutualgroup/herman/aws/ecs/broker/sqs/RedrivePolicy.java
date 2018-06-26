package com.libertymutualgroup.herman.aws.ecs.broker.sqs;

public class RedrivePolicy {
    private String maximumReceiveCount = "5";

    public String getMaximumReceiveCount() {
        return maximumReceiveCount;
    }

    public void setMaximumReceiveCount(String maximumReceiveCount) {
        this.maximumReceiveCount = maximumReceiveCount;
    }

    @Override
    public String toString() {
        return "RedrivePolicy{" +
                "maximumReceiveCount='" + maximumReceiveCount + '\'' +
                '}';
    }
}
