package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class S3BrokerProperties {

    private S3EncryptionOption defaultEncryption;


    public S3EncryptionOption getDefaultEncryption() {
        return defaultEncryption;
    }

    public void setDefaultEncryption(S3EncryptionOption defaultEncryption) {
        this.defaultEncryption = defaultEncryption;
    }

    public S3BrokerProperties withDefaultEncryption(final S3EncryptionOption defaultEncryption) {
        this.defaultEncryption = defaultEncryption;
        return this;
    }

    @Override
    public String toString() {
        return "S3BrokerProperties{" +
            "defaultEncryption='" + defaultEncryption + '\'' +
            '}';
    }
}