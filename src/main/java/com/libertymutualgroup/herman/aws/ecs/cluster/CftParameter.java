package com.libertymutualgroup.herman.aws.ecs.cluster;

/**
 * Created by n0309882 on 10/22/18.
 */
public class CftParameter {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override public String toString() {
        return "CftParameter{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }
}
