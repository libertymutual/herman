package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import com.amazonaws.services.cloudformation.model.Tag;
import java.util.List;

public class DnsBrokerRequest {

    private String vanityUrl;
    private String awsUrl;
    private String appName;
    private List<Tag> tags;

    public String getVanityUrl() {
        return vanityUrl;
    }

    public void setVanityUrl(String vanityUrl) {
        this.vanityUrl = vanityUrl;
    }

    public String getAwsUrl() {
        return awsUrl;
    }

    public void setAwsUrl(String awsUrl) {
        this.awsUrl = awsUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public DnsBrokerRequest withVanityUrl(final String vanityUrl) {
        this.vanityUrl = vanityUrl;
        return this;
    }

    public DnsBrokerRequest withAwsUrl(final String awsUrl) {
        this.awsUrl = awsUrl;
        return this;
    }

    public DnsBrokerRequest withAppName(final String appName) {
        this.appName = appName;
        return this;
    }

    public DnsBrokerRequest withTags(final List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public String toString() {
        return "DnsBrokerRequest{" +
            "vanityUrl='" + vanityUrl + '\'' +
            ", awsUrl='" + awsUrl + '\'' +
            ", appName='" + appName + '\'' +
            ", tags=" + tags +
            '}';
    }
}
