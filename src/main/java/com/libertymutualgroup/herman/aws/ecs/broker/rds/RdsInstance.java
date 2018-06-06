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
package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.model.DBInstance;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RdsInstance extends DBInstance {

    private transient RdsInjectConfiguration injectNames;
    private String encryptedPassword;
    private String appUsername;
    private String appEncryptedPassword;
    private String adminUsername;
    private String adminEncryptedPassword;
    private String credPrefix;
    private Boolean fullUpdate;
    private String[] availabilityZones;
    private String optionGroupFile;
    private String parameterGroupFile;
    private List<String> additionalSecGroups;
    private Boolean preDeployBackup;

    public void setDefaults() {
        this.setInjectNames(getInjectNames() == null ? new RdsInjectConfiguration() : getInjectNames());

        injectNames.setDefaults();

        this.setMasterUsername(getMasterUsername() == null ? "dbAdmin" : getMasterUsername());
        this.setDBName(getDBName() == null ? "appDb" : getDBName());
        this.setDBInstanceClass(getDBInstanceClass() == null ? "db.t2.small" : getDBInstanceClass());
        this.setAllocatedStorage(getAllocatedStorage() == null ? 5 : getAllocatedStorage());
        this.setPubliclyAccessible(getPubliclyAccessible() == null ? false : getPubliclyAccessible());
        this.setAutoMinorVersionUpgrade(getAutoMinorVersionUpgrade() == null ? true : getAutoMinorVersionUpgrade());
        this.setPreferredBackupWindow(getPreferredBackupWindow() == null ? "01:00-02:00" : getPreferredBackupWindow());
        this.setPreferredMaintenanceWindow(
            getPreferredMaintenanceWindow() == null ? "sun:20:00-sun:21:00" : getPreferredMaintenanceWindow());
        this.setBackupRetentionPeriod(getBackupRetentionPeriod() == null ? 14 : getBackupRetentionPeriod());
        this.setStorageType(getStorageType() == null ? "gp2" : getStorageType());
        this.setFullUpdate(getFullUpdate() == null ? false : getFullUpdate());
        this.setIAMDatabaseAuthenticationEnabled(
            getIAMDatabaseAuthenticationEnabled() == null ? false : getIAMDatabaseAuthenticationEnabled());
        this.setAvailabilityZones(getAvailabilityZones() == null ? new String[]{"us-east-1a"} : getAvailabilityZones());
        this.setPreDeployBackup(getPreDeployBackup() == null ? false : getPreDeployBackup());
    }

    public String getConnectionString() {
        String instanceType = this.getEngine().toLowerCase();
        String connectionType = instanceType.contains("postgres") ? "postgresql" : instanceType;
        if ("aurora".equalsIgnoreCase(this.getEngine()) || "aurora-mysql".equalsIgnoreCase(this.getEngine())) {
            connectionType = "mysql";
        }
        String host = this.getEndpoint().getAddress();
        String port = this.getEndpoint().getPort().toString();
        String db = this.getDBName();

        return "jdbc:" + connectionType + "://" + host + ":" + port + "/" + db
            + "?useSSL=true&requireSSL=true&verifyServerCertificate=false";
    }

    public RdsInjectConfiguration getInjectNames() {
        return injectNames;
    }

    public void setInjectNames(RdsInjectConfiguration injectNames) {
        this.injectNames = injectNames;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getAppUsername() {
        return appUsername;
    }

    public void setAppUsername(String appUsername) {
        this.appUsername = appUsername;
    }

    public String getAppEncryptedPassword() {
        return appEncryptedPassword;
    }

    public void setAppEncryptedPassword(String appEncryptedPassword) {
        this.appEncryptedPassword = appEncryptedPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEncryptedPassword() {
        return adminEncryptedPassword;
    }

    public void setAdminEncryptedPassword(String adminEncryptedPassword) {
        this.adminEncryptedPassword = adminEncryptedPassword;
    }

    public String getCredPrefix() {
        return credPrefix;
    }

    public void setCredPrefix(String credPrefix) {
        this.credPrefix = credPrefix;
    }

    public Boolean getFullUpdate() {
        return fullUpdate;
    }

    public void setFullUpdate(Boolean fullUpdate) {
        this.fullUpdate = fullUpdate;
    }

    public String[] getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(String[] availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public String getOptionGroupFile() {
        return optionGroupFile;
    }

    public void setOptionGroupFile(String optionGroupFile) {
        this.optionGroupFile = optionGroupFile;
    }

    public String getParameterGroupFile() {
        return parameterGroupFile;
    }

    public void setParameterGroupFile(String parameterGroupFile) {
        this.parameterGroupFile = parameterGroupFile;
    }

    @Override
    public String toString() {
        return "RdsInstance{" +
            "injectNames=" + injectNames +
            ", encryptedPass='" + encryptedPassword + '\'' +
            "} " + super.toString();
    }

    public List<String> getAdditionalSecGroups() {
        return additionalSecGroups;
    }

    public void setAdditionalSecGroups(List<String> additionalSecGroups) {
        this.additionalSecGroups = additionalSecGroups;
    }

    public Boolean getPreDeployBackup() {
        return preDeployBackup;
    }

    public void setPreDeployBackup(Boolean preDeployBackup) {
        this.preDeployBackup = preDeployBackup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RdsInstance that = (RdsInstance) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(injectNames, that.injectNames)
            .append(encryptedPassword, that.encryptedPassword)
            .append(appUsername, that.appUsername)
            .append(appEncryptedPassword, that.appEncryptedPassword)
            .append(adminUsername, that.adminUsername)
            .append(adminEncryptedPassword, that.adminEncryptedPassword)
            .append(credPrefix, that.credPrefix)
            .append(fullUpdate, that.fullUpdate)
            .append(availabilityZones, that.availabilityZones)
            .append(optionGroupFile, that.optionGroupFile)
            .append(parameterGroupFile, that.parameterGroupFile)
            .append(additionalSecGroups, that.additionalSecGroups)
            .append(preDeployBackup, that.preDeployBackup)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(injectNames)
            .append(encryptedPassword)
            .append(appUsername)
            .append(appEncryptedPassword)
            .append(adminUsername)
            .append(adminEncryptedPassword)
            .append(credPrefix)
            .append(fullUpdate)
            .append(availabilityZones)
            .append(optionGroupFile)
            .append(parameterGroupFile)
            .append(additionalSecGroups)
            .append(preDeployBackup)
            .toHashCode();
    }
}
