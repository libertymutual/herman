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
package com.libertymutualgroup.herman.aws.ecs.broker.domain;

import java.util.Objects;

public class HermanBrokerUpdate {

    HermanBrokerStatus status;
    String message;

    public HermanBrokerStatus getStatus() {
        return status;
    }

    public void setStatus(HermanBrokerStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "HermanBrokerUpdate{" +
            "status=" + status +
            ", message='" + message + '\'' +
            '}';
    }

    public HermanBrokerUpdate withStatus(final HermanBrokerStatus status) {
        this.status = status;
        return this;
    }

    public HermanBrokerUpdate withMessage(final String message) {
        this.message = message;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HermanBrokerUpdate that = (HermanBrokerUpdate) o;
        return status == that.status &&
            Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message);
    }
}
