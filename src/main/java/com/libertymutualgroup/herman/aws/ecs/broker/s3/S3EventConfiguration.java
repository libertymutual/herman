package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.amazonaws.services.s3.model.S3Event;
import java.util.EnumSet;

public class S3EventConfiguration {

    private String name;
    private String arn;
    private EnumSet<S3Event> events;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public EnumSet<S3Event> getEvents() {
        return events;
    }

    public void setEvents(EnumSet<S3Event> events) {
        this.events = events;
    }

    public S3EventConfiguration withName(final String name) {
        this.name = name;
        return this;
    }

    public S3EventConfiguration withARN(final String functionARN) {
        this.arn = functionARN;
        return this;
    }

    public S3EventConfiguration withEvents(final EnumSet<S3Event> events) {
        this.events = events;
        return this;
    }

    @Override
    public String toString() {
        return "S3EventConfiguration{" +
            "name='" + name + '\'' +
            ", arn='" + arn + '\'' +
            ", events=" + events +
            '}';
    }
}