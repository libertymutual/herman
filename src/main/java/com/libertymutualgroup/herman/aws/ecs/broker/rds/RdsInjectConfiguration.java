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

/**
 * Defines what the environment variables should be named when injecting a brokered RDS instance.
 */
public class RdsInjectConfiguration {

    private String host;
    private String port;
    private String db;
    private String dbResourceId;
    private String connectionString;
    private String username;
    private String encryptedPassword;
    private String appUsername;
    private String appEncryptedPassword;
    private String adminUsername;
    private String adminEncryptedPassword;

    public void setDefaults() {
        host = (host == null ? "rds_host" : host);
        port = (port == null ? "rds_port" : port);
        db = (db == null ? "rds_db" : db);
        dbResourceId = (dbResourceId == null ? "rds_resource_id" : dbResourceId);
        connectionString = (connectionString == null ? "rds_connection_string" : connectionString);
        username = (username == null ? "rds_username" : username);
        encryptedPassword = (encryptedPassword == null ? "rds_encrypted_password" : encryptedPassword);
        appUsername = (appUsername == null ? "rds_app_username" : appUsername);
        appEncryptedPassword = (appEncryptedPassword == null ? "rds_app_encrypted_password" : appEncryptedPassword);
        adminUsername = (adminUsername == null ? "rds_admin_username" : adminUsername);
        adminEncryptedPassword = (adminEncryptedPassword == null ? "rds_admin_encrypted_password"
            : adminEncryptedPassword);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getDbResourceId() {
        return dbResourceId;
    }

    public void setDbResourceId(String dbResourceId) {
        this.dbResourceId = dbResourceId;
    }

    @Override
    public String toString() {
        return "RdsInjectConfiguration{" +
            "host='" + host + '\'' +
            ", port='" + port + '\'' +
            ", db='" + db + '\'' +
            ", dbResourceId='" + dbResourceId + '\'' +
            ", connectionString='" + connectionString + '\'' +
            ", username='" + username + '\'' +
            ", appUsername='" + appUsername + '\'' +
            ", adminUsername='" + adminUsername + '\'' +
            '}';
    }
}
