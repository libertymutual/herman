package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import java.util.Map;

public class CustomBrokerResponse {
    enum Status {
        SUCCESS, FAILED
    }
    private String message;
    private Status status;
    private Map<String,String> variablesToInject;

    public CustomBrokerResponse() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, String> getVariablesToInject() {
        return variablesToInject;
    }

    public void setVariablesToInject(Map<String, String> variablesToInject) {
        this.variablesToInject = variablesToInject;
    }
}
