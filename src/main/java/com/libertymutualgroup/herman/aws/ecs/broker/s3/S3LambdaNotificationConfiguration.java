package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.amazonaws.services.s3.model.S3Event;
import java.util.EnumSet;

public class S3LambdaNotificationConfiguration {

    private String name;
    private String functionARN;
    private EnumSet<S3Event> events;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFunctionARN() {
        return functionARN;
    }

    public void setFunctionARN(String functionARN) {
        this.functionARN = functionARN;
    }

    public EnumSet<S3Event> getEvents() {
        return events;
    }

    public void setEvents(EnumSet<S3Event> events) {
        this.events = events;
    }

    public S3LambdaNotificationConfiguration withName(final String name) {
        this.name = name;
        return this;
    }

    public S3LambdaNotificationConfiguration withFunctionARN(final String functionARN) {
        this.functionARN = functionARN;
        return this;
    }

    public S3LambdaNotificationConfiguration withEvents(final EnumSet<S3Event> events) {
        this.events = events;
        return this;
    }

    @Override
    public String toString() {
        return "S3LambdaNotificationConfiguration{" +
            "name='" + name + '\'' +
            ", functionARN='" + functionARN + '\'' +
            ", events=" + events +
            '}';
    }
}