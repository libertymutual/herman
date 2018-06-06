package com.libertymutualgroup.herman.aws.ecs.broker.newrelic;

import com.libertymutualgroup.herman.aws.ecs.broker.domain.HermanBrokerUpdate;
import java.util.ArrayList;
import java.util.List;

public class NewRelicBrokerResponse {

    List<HermanBrokerUpdate> updates = new ArrayList<>();
    String applicationId;

    public List<HermanBrokerUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<HermanBrokerUpdate> updates) {
        this.updates = updates;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public NewRelicBrokerResponse withUpdates(
        final List<HermanBrokerUpdate> updates) {
        this.updates = updates;
        return this;
    }

    public NewRelicBrokerResponse withApplicationId(final String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    @Override
    public String toString() {
        return "NewRelicBrokerResponse{" +
            "updates=" + updates +
            ", applicationId='" + applicationId + '\'' +
            '}';
    }
}
