package com.libertymutualgroup.herman.aws.ecs;

import java.util.Objects;

/**
 * Created by n0309882 on 11/7/18.
 */
public class EnvironmentVariableDefinition {

    private String name;
    private String value;
    private Boolean encrypt = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvironmentVariableDefinition withName(String name) {
        setName(name);
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EnvironmentVariableDefinition withValue(String value) {
        setValue(value);
        return this;
    }

    public Boolean isEncrypt() {
        return encrypt;
    }

    public void setEncrypt(Boolean encrypt) {
        this.encrypt = encrypt;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(this.getName() != null) {
            sb.append("Name: ").append(this.getName()).append(",");
        }

        if(this.getValue() != null) {
            sb.append("Value: ").append(this.getValue());
        }

        if(this.isEncrypt() != null) {
            sb.append("Encrypt: ").append(this.isEncrypt());
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvironmentVariableDefinition that = (EnvironmentVariableDefinition) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(encrypt, that.encrypt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, encrypt);
    }
}
