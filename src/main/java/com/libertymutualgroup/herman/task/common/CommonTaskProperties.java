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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonTaskProperties {

    private String company;
    private String sbu;
    private String org;
    private String engine;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSbu() {
        return sbu;
    }

    public void setSbu(String sbu) {
        this.sbu = sbu;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getSbuTagKey() {
        return company + "_sbu";
    }

    public String getOrgTagKey() {
        return company + "_org";
    }

    public String getAppTagKey() {
        return company + "_app";
    }

    public String getClusterTagKey() {
        return company + "_cluster";
    }

    public CommonTaskProperties withCompany(final String company) {
        this.company = company;
        return this;
    }

    public CommonTaskProperties withSbu(final String sbu) {
        this.sbu = sbu;
        return this;
    }

    public CommonTaskProperties withOrg(final String org) {
        this.org = org;
        return this;
    }

    public CommonTaskProperties withEngine(final String engine) {
        this.engine = engine;
        return this;
    }

    @Override
    public String toString() {
        return "CommonTaskProperties{" +
            "company='" + company + '\'' +
            ", sbu='" + sbu + '\'' +
            ", org='" + org + '\'' +
            ", engine='" + engine + '\'' +
            '}';
    }
}
