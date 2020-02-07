/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.aws.tags;

import com.amazonaws.services.ec2.model.Tag;

import java.util.Objects;

public class HermanTag {
    private String key;
    private String value;

    public HermanTag() {}

    public HermanTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static HermanTag fromEc2Tag(Tag tag) {
        return new HermanTag(tag.getKey(), tag.getValue());
    }

    public static HermanTag fromKmsTag(com.amazonaws.services.kms.model.Tag tag) {
        return new HermanTag(tag.getTagKey(), tag.getTagValue());
    }

    public static HermanTag fromRdsTag(com.amazonaws.services.rds.model.Tag tag) {
        return new HermanTag(tag.getKey(), tag.getValue());
    }

    public static HermanTag fromEcsTag(com.amazonaws.services.ecs.model.Tag tag) {
        return new HermanTag(tag.getKey(), tag.getValue());
    }

    public static HermanTag fromCftTag(com.amazonaws.services.cloudformation.model.Tag tag) {
        return new HermanTag(tag.getKey(), tag.getValue());
    }

    public Tag toEc2Tag() {
        return new Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.kms.model.Tag toKmsTag() {
        return new com.amazonaws.services.kms.model.Tag()
            .withTagKey(this.key)
            .withTagValue(this.value);
    }

    public com.amazonaws.services.rds.model.Tag toRdsTag() {
        return new com.amazonaws.services.rds.model.Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.dynamodbv2.model.Tag toDynamoTag() {
        return new com.amazonaws.services.dynamodbv2.model.Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.ecs.model.Tag toEcsTag() {
        return new com.amazonaws.services.ecs.model.Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.cloudformation.model.Tag toCftTag() {
        return new com.amazonaws.services.cloudformation.model.Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.identitymanagement.model.Tag toIamTag() {
        return new com.amazonaws.services.identitymanagement.model.Tag()
            .withKey(this.key)
            .withValue(this.value);
    }

    public com.amazonaws.services.cloudwatchevents.model.Tag toCloudWatchEventsTag() {
        return new com.amazonaws.services.cloudwatchevents.model.Tag()
                .withKey(this.key)
                .withValue(this.value);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public HermanTag withKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public HermanTag withValue(String value) {
        this.value = value;
        return this;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HermanTag hermanTag = (HermanTag) o;
        return Objects.equals(key, hermanTag.key) && Objects.equals(value, hermanTag.value);
    }

    @Override public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override public String toString() {
        return "HermanTag{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }
}
