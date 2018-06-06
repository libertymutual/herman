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
package com.libertymutualgroup.herman.task.common;

public class EnvironmentProperties {

    private String nonprod;
    private String prod;

    public String getNonprod() {
        return nonprod;
    }

    public void setNonprod(String nonprod) {
        this.nonprod = nonprod;
    }

    public String getProd() {
        return prod;
    }

    public void setProd(String prod) {
        this.prod = prod;
    }

    public EnvironmentProperties withNonprod(final String nonprod) {
        this.nonprod = nonprod;
        return this;
    }

    public EnvironmentProperties withProd(final String prod) {
        this.prod = prod;
        return this;
    }

    @Override
    public String toString() {
        return "EnvironmentProperties{" +
            "nonprod='" + nonprod + '\'' +
            ", prod='" + prod + '\'' +
            '}';
    }
}
