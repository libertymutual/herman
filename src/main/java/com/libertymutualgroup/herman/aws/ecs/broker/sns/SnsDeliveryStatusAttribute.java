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
package com.libertymutualgroup.herman.aws.ecs.broker.sns;

/**
 * Enum of topic message delivery status attribute
 * https://docs.aws.amazon.com/sns/latest/dg/msg-status-topics.html
 */
public enum SnsDeliveryStatusAttribute {

    ApplicationSuccessFeedbackRoleArn("ApplicationSuccessFeedbackRoleArn", ""),
    ApplicationSuccessFeedbackSampleRate("ApplicationSuccessFeedbackSampleRate", "100"),
    ApplicationFailureFeedbackRoleArn("ApplicationFailureFeedbackRoleArn", ""),

    HTTPSuccessFeedbackRoleArn("HTTPSuccessFeedbackRoleArn", ""),
    HTTPSuccessFeedbackSampleRate("HTTPSuccessFeedbackSampleRate", "100"),
    HTTPFailureFeedbackRoleArn("HTTPFailureFeedbackRoleArn", ""),

    LambdaSuccessFeedbackRoleArn("LambdaSuccessFeedbackRoleArn", ""),
    LambdaSuccessFeedbackSampleRate("LambdaSuccessFeedbackSampleRate", "100"),
    LambdaFailureFeedbackRoleArn("LambdaFailureFeedbackRoleArn", ""),

    SQSSuccessFeedbackRoleArn("SQSSuccessFeedbackRoleArn", ""),
    SQSSuccessFeedbackSampleRate("SQSSuccessFeedbackSampleRate", "100"),
    SQSFailureFeedbackRoleArn("SQSFailureFeedbackRoleArn", "");

    private String name;

    private String value;

    SnsDeliveryStatusAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}

