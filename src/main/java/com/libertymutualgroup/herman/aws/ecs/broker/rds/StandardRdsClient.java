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

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.CreateOptionGroupRequest;
import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBParameterGroupNotFoundException;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;
import com.amazonaws.services.rds.model.DBSecurityGroupMembership;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeOptionGroupsRequest;
import com.amazonaws.services.rds.model.DescribeOptionGroupsResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.ModifyOptionGroupRequest;
import com.amazonaws.services.rds.model.Option;
import com.amazonaws.services.rds.model.OptionConfiguration;
import com.amazonaws.services.rds.model.OptionGroup;
import com.amazonaws.services.rds.model.OptionGroupNotFoundException;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.amazonaws.services.rds.model.VpcSecurityGroupMembership;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.libertymutualgroup.herman.aws.ecs.broker.rds.RdsBroker.pollingIntervalMs;

public class StandardRdsClient implements RdsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardRdsClient.class);
    private final static String PENDING_REBOOT_STATUS = "pending-reboot";

    private AmazonRDS client;
    private RdsInstance rds;
    private EcsClusterMetadata clusterMetadata;
    private List<HermanTag> tags;
    private HermanLogger buildLogger;

    StandardRdsClient(AmazonRDS client, RdsInstance rds, EcsClusterMetadata clusterMetadata, List<HermanTag> tags,
        HermanLogger buildLogger) {
        this.client = client;
        this.rds = rds;
        this.clusterMetadata = clusterMetadata;
        this.tags = tags;
        this.buildLogger = buildLogger;
    }

    @Override
    public Boolean dbExists(String instanceId) {
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest()
            .withDBInstanceIdentifier(instanceId);
        try {
            client.describeDBInstances(describeDBInstancesRequest);
            return true;
        } catch (DBInstanceNotFoundException ex) {
            LOGGER.debug("Error describing DB instance: " + instanceId, ex);
            return false;
        }
    }

    @Override
    public void createNewDb(String instanceId, String masterUserPassword) {
        CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest();
        createDBInstanceRequest.withAllocatedStorage(rds.getAllocatedStorage())
            .withAutoMinorVersionUpgrade(rds.getAutoMinorVersionUpgrade())
            .withAvailabilityZone(rds.getAvailabilityZone())
            .withBackupRetentionPeriod(rds.getBackupRetentionPeriod())
            .withCharacterSetName(rds.getCharacterSetName())
            .withCopyTagsToSnapshot(rds.getCopyTagsToSnapshot())
            .withDBClusterIdentifier(rds.getDBClusterIdentifier())
            .withDBInstanceClass(rds.getDBInstanceClass())
            .withDBInstanceIdentifier(instanceId)
            .withDBName(rds.getDBName())
            .withDBSubnetGroupName(clusterMetadata.getDbSubnetGroup())
            .withEngine(rds.getEngine())
            .withEngineVersion(rds.getEngineVersion())
            .withIops(rds.getIops())
            .withKmsKeyId(rds.getKmsKeyId())
            .withLicenseModel(rds.getLicenseModel())
            .withMasterUsername(rds.getMasterUsername())
            .withMasterUserPassword(masterUserPassword)
            .withMonitoringInterval(rds.getMonitoringInterval())
            .withMonitoringRoleArn(rds.getMonitoringRoleArn())
            .withMultiAZ(rds.getMultiAZ())
            .withPort(rds.getDbInstancePort())
            .withPreferredBackupWindow(rds.getPreferredBackupWindow())
            .withPreferredMaintenanceWindow(rds.getPreferredMaintenanceWindow())
            .withPubliclyAccessible(rds.getPubliclyAccessible()).
            withStorageEncrypted(true)
            .withStorageType(rds.getStorageType())
            .withTags(TagUtil.hermanToRdsTags(tags))
            .withEnableIAMDatabaseAuthentication(rds.getIAMDatabaseAuthenticationEnabled())
            .withTimezone(rds.getTimezone()).
            withVpcSecurityGroupIds(clusterMetadata.getRdsSecurityGroup());
        client.createDBInstance(createDBInstanceRequest);
    }

    @Override
    public void updateMasterPassword(String instanceId, String masterUserPassword) {
        ModifyDBInstanceRequest request = new ModifyDBInstanceRequest().withDBInstanceIdentifier(instanceId)
            .withMasterUserPassword(masterUserPassword).withApplyImmediately(true);

        client.modifyDBInstance(request);
    }


    @Override
    public void runFullUpdate(String instanceId, String masterUserPassword) {
        ModifyDBInstanceRequest request = new ModifyDBInstanceRequest()
            .withAllocatedStorage(rds.getAllocatedStorage())
            .withAllowMajorVersionUpgrade(true)
            .withAutoMinorVersionUpgrade(rds.getAutoMinorVersionUpgrade())
            .withBackupRetentionPeriod(rds.getBackupRetentionPeriod())
            .withCopyTagsToSnapshot(rds.getCopyTagsToSnapshot())
            .withDBInstanceClass(rds.getDBInstanceClass())
            .withDBInstanceIdentifier(instanceId)
            .withEngineVersion(rds.getEngineVersion())
            .withIops(rds.getIops())
            .withLicenseModel(rds.getLicenseModel())
            .withMasterUserPassword(masterUserPassword)
            .withMonitoringInterval(rds.getMonitoringInterval())
            .withMonitoringRoleArn(rds.getMonitoringRoleArn())
            .withMultiAZ(rds.getMultiAZ())
            .withPreferredBackupWindow(rds.getPreferredBackupWindow())
            .withPreferredMaintenanceWindow(rds.getPreferredMaintenanceWindow())
            .withPubliclyAccessible(rds.getPubliclyAccessible())
            .withStorageType(rds.getStorageType())
            .withEnableIAMDatabaseAuthentication(rds.getIAMDatabaseAuthenticationEnabled())
            .withVpcSecurityGroupIds(clusterMetadata.getRdsSecurityGroup())
            .withApplyImmediately(true);
        DBInstance instance = client.modifyDBInstance(request);
        buildLogger.addLogEntry("... DB update applied");

        this.applyTags(instance.getDBInstanceArn(), TagUtil.hermanToRdsTags(tags));
    }

    @Override
    public void waitForAvailableStatus(String instanceId) {
        buildLogger.addLogEntry("... Waiting for RDS instance to be available.");
        try {
            DBInstance rdsResult;
            do {
                Thread.sleep(pollingIntervalMs);
                rdsResult = this.getDbInstance(instanceId);
                buildLogger.addLogEntry("... Instance status: " + rdsResult.getDBInstanceStatus());
            } while (!"available".equals(rdsResult.getDBInstanceStatus())
                || rdsResult.getPendingModifiedValues().getMasterUserPassword() != null);

            buildLogger.addLogEntry("RDS Result: " + rdsResult);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException("Interrupted during process");
        }
    }

    @Override
    public Endpoint getEndpoint(String instanceId) {
        return getDbInstance(instanceId).getEndpoint();
    }

    @Override
    public String getDbiResourceId(String instanceId) {
        return getDbInstance(instanceId).getDBInstanceIdentifier();
    }

    @Override
    public void setDBParameterGroup(String instanceId, List<Parameter> parameters) {
        buildLogger.addLogEntry("Setting the DB parameter group");

        DBParameterGroup dbParameterGroup;
        DBInstance rdsResult = getDbInstance(instanceId);

        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = client.describeDBEngineVersions(
            new DescribeDBEngineVersionsRequest().withEngine(rdsResult.getEngine())
                .withEngineVersion(rdsResult.getEngineVersion()));
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbParameterGroupName = String
            .format("%s-%s", dbEngineVersion.getDBParameterGroupFamily().replaceAll("\\.", "-"),
                rdsResult.getDBInstanceIdentifier());
        buildLogger.addLogEntry("... DB parameter group name = " + dbParameterGroupName);

        try {
            DescribeDBParameterGroupsResult describeDBParameterGroupsResult = client.describeDBParameterGroups(
                new DescribeDBParameterGroupsRequest().withDBParameterGroupName(dbParameterGroupName));
            dbParameterGroup = describeDBParameterGroupsResult.getDBParameterGroups().iterator().next();

            buildLogger.addLogEntry("... Updating parameters for DB parameter group " + dbParameterGroupName);
            client.modifyDBParameterGroup(new ModifyDBParameterGroupRequest()
                .withDBParameterGroupName(dbParameterGroupName)
                .withParameters(parameters));

        } catch (DBParameterGroupNotFoundException ex) {
            LOGGER.debug("Error getting DB param group: " + dbParameterGroupName, ex);

            buildLogger.addLogEntry("... Creating DB parameter group " + dbParameterGroupName);
            dbParameterGroup = client.createDBParameterGroup(new CreateDBParameterGroupRequest()
                .withDBParameterGroupFamily(dbEngineVersion.getDBParameterGroupFamily())
                .withDBParameterGroupName(dbParameterGroupName)
                .withDescription(String.format("%s %s Parameter Group", rdsResult.getDBInstanceIdentifier(),
                    dbEngineVersion.getDBParameterGroupFamily()))
                .withTags(TagUtil.hermanToRdsTags(tags)));

            buildLogger.addLogEntry("... Updating parameters for DB parameter group " + dbParameterGroupName);
            client.modifyDBParameterGroup(new ModifyDBParameterGroupRequest()
                .withDBParameterGroupName(dbParameterGroupName)
                .withParameters(parameters));
        }

        boolean dbParameterGroupIsSet = rdsResult.getDBParameterGroups().stream()
            .anyMatch(aDBParameterGroup -> aDBParameterGroup.getDBParameterGroupName().equals(dbParameterGroupName));
        if (!dbParameterGroupIsSet) {
            try {
                buildLogger.addLogEntry(
                    "... Modifying DB instance to use parameter group " + dbParameterGroup.getDBParameterGroupName());
                client.modifyDBInstance(new ModifyDBInstanceRequest()
                    .withDBInstanceIdentifier(rdsResult.getDBInstanceIdentifier())
                    .withDBParameterGroupName(dbParameterGroup.getDBParameterGroupName()));

                // Sleeping because the status doesn't immediately change
                Thread.sleep(pollingIntervalMs);
            } catch (Exception ex) {
                throw new AwsExecException("Error setting param group " + dbParameterGroup.getDBParameterGroupName(), ex);
            }
        }

        this.waitForAvailableStatus(instanceId);
        if (this.dbParamGroupHasPendingRebootStatus(instanceId)) {
            this.rebootDb(instanceId);
            this.waitForAvailableStatus(instanceId);
        }
    }

    @Override
    public void setOptionGroup(String instanceId, OptionGroup expectedOptionGroup) {
        if (this.rds.getEngine().contains("mysql") || this.rds.getEngine().contains("mariadb") || this.rds.getEngine()
            .contains("sqlserver") || this.rds.getEngine().contains("oracle")) {
            buildLogger.addLogEntry("Setting the DB option group");

            DBInstance rdsResult = getDbInstance(instanceId);
            final String optionGroupName = String.format("%s-%s-%s", rdsResult.getEngine(),
                expectedOptionGroup.getMajorEngineVersion().replaceAll("\\.", "-"),
                rdsResult.getDBInstanceIdentifier());
            buildLogger.addLogEntry("... DB option group name = " + optionGroupName);

            OptionGroup optionGroup;
            try {
                DescribeOptionGroupsResult describeOptionGroupsResult = client
                    .describeOptionGroups(new DescribeOptionGroupsRequest().withOptionGroupName(optionGroupName));
                optionGroup = describeOptionGroupsResult.getOptionGroupsList().iterator().next();

                // Get options to remove
                List<String> optionsToRemove = new ArrayList<>();
                for (Option option : optionGroup.getOptions()) {
                    // Loop through options, check if option should be removed
                    boolean keepGroup = expectedOptionGroup.getOptions().stream()
                        .anyMatch(option1 -> option1.getOptionName().equals(option.getOptionName()));
                    if (!keepGroup) {
                        optionsToRemove.add(option.getOptionName());
                    }
                }

                // Get options to update
                List<OptionConfiguration> optionsToInclude = expectedOptionGroup.getOptions().stream()
                    .map(this::getOptionConfiguration).collect(
                        Collectors.toList());
                buildLogger.addLogEntry("... Updating options for DB option group " + optionGroupName);
                client.modifyOptionGroup(new ModifyOptionGroupRequest()
                    .withOptionGroupName(optionGroupName)
                    .withOptionsToRemove(optionsToRemove)
                    .withOptionsToInclude(optionsToInclude)
                    .withApplyImmediately(true));

            } catch (OptionGroupNotFoundException ex) {
                LOGGER.debug("Error getting DB option group: " + optionGroupName, ex);

                buildLogger.addLogEntry("... Creating DB option group " + optionGroupName);
                client.createOptionGroup(new CreateOptionGroupRequest()
                    .withOptionGroupName(optionGroupName)
                    .withOptionGroupDescription(String
                        .format("%s Option Group for %s %s", rdsResult.getDBInstanceIdentifier(), rdsResult.getEngine(),
                            expectedOptionGroup.getMajorEngineVersion()))
                    .withEngineName(rdsResult.getEngine())
                    .withMajorEngineVersion(expectedOptionGroup.getMajorEngineVersion())
                    .withTags(TagUtil.hermanToRdsTags(tags)));

                buildLogger.addLogEntry("... Updating options for DB option group " + optionGroupName);
                List<OptionConfiguration> optionConfigurations = expectedOptionGroup.getOptions().stream()
                    .map(this::getOptionConfiguration)
                    .collect(Collectors.toList());
                client.modifyOptionGroup(new ModifyOptionGroupRequest()
                    .withOptionGroupName(optionGroupName)
                    .withOptionsToInclude(optionConfigurations)
                    .withApplyImmediately(true));
            }

            boolean optionGroupIsSet = rdsResult.getOptionGroupMemberships().stream()
                .anyMatch(optionGroupMembership -> optionGroupName.equals(optionGroupMembership.getOptionGroupName()));
            if (!optionGroupIsSet) {
                buildLogger.addLogEntry("... Setting DB option group for RDS instance " + optionGroupName);
                client.modifyDBInstance(new ModifyDBInstanceRequest()
                    .withDBInstanceIdentifier(rdsResult.getDBInstanceIdentifier())
                    .withOptionGroupName(optionGroupName)
                    .withApplyImmediately(true));
            }
        } else {
            buildLogger.addLogEntry("DB Engine does not support option groups.");
        }
    }

    @Override
    public void createSnapshot(String instanceId, String snapshotId) {
        CreateDBSnapshotRequest request = new CreateDBSnapshotRequest()
            .withDBInstanceIdentifier(instanceId)
            .withDBSnapshotIdentifier(snapshotId);
        client.createDBSnapshot(request);
        try {
            DescribeDBSnapshotsResult result = getDescribeDBSnapshotsResult(snapshotId);
            buildLogger.addLogEntry("... RDS Snapshot Result: " + result.getDBSnapshots().get(0));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException("Interrupted during process");
        }
    }

    @Override
    public void setDefaults(String kmsKeyId) {
        rds.setInjectNames(rds.getInjectNames() == null ? new RdsInjectConfiguration() : rds.getInjectNames());

        rds.getInjectNames().setDefaults();

        rds.setMasterUsername(rds.getMasterUsername() == null ? "dbAdmin" : rds.getMasterUsername());
        rds.setDBName(rds.getDBName() == null ? "appDb" : rds.getDBName());
        rds.setDBInstanceClass(rds.getDBInstanceClass() == null ? "db.t2.small" : rds.getDBInstanceClass());
        rds.setAllocatedStorage(rds.getAllocatedStorage() == null ? 5 : rds.getAllocatedStorage());
        rds.setPubliclyAccessible(rds.getPubliclyAccessible() == null ? false : rds.getPubliclyAccessible());
        rds.setAutoMinorVersionUpgrade(rds.getAutoMinorVersionUpgrade() == null ? true : rds.getAutoMinorVersionUpgrade());
        rds.setPreferredBackupWindow(rds.getPreferredBackupWindow() == null ? "01:00-02:00" : rds.getPreferredBackupWindow());
        rds.setPreferredMaintenanceWindow(
            rds.getPreferredMaintenanceWindow() == null ? "sun:20:00-sun:21:00" : rds.getPreferredMaintenanceWindow());
        rds.setBackupRetentionPeriod(rds.getBackupRetentionPeriod() == null ? 14 : rds.getBackupRetentionPeriod());
        rds.setStorageType(rds.getStorageType() == null ? "gp2" : rds.getStorageType());
        rds.setFullUpdate(rds.getFullUpdate() == null ? false : rds.getFullUpdate());
        rds.setIAMDatabaseAuthenticationEnabled(
            rds.getIAMDatabaseAuthenticationEnabled() == null ? false : rds.getIAMDatabaseAuthenticationEnabled());
        rds.setAvailabilityZones(rds.getAvailabilityZones() == null ? new String[]{"us-east-1a"} : rds.getAvailabilityZones());
        rds.setPreDeployBackup(rds.getPreDeployBackup() == null ? false : rds.getPreDeployBackup());
        rds.setExtensions(rds.getExtensions() == null ? new ArrayList<>() : rds.getExtensions());
        rds.setKmsKeyId(rds.getKmsKeyId() == null ? kmsKeyId : rds.getKmsKeyId());
    }

    private DescribeDBSnapshotsResult getDescribeDBSnapshotsResult(String snapshotId) throws InterruptedException {
        String status = "";
        DescribeDBSnapshotsResult result = null;

        do {
            Thread.sleep(pollingIntervalMs);
            try {
                result = this.getDbSnapshot(snapshotId);
                if (result.getDBSnapshots().size() == 1) {
                    status = result.getDBSnapshots().get(0).getStatus();
                }
                buildLogger.addLogEntry("... Snapshot status: " + status);
            } catch (DBSnapshotNotFoundException e) {
                LOGGER.debug("Error getting snapshot: " + snapshotId, e);
                buildLogger.addLogEntry("... Waiting for snapshot creation");
            }
        } while (result == null || !"available".equals(status));
        return result;
    }

    boolean dbParamGroupHasPendingRebootStatus(String instanceId) {
        DescribeDBInstancesResult describeDBInstancesResult = client
            .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId));
        DBInstance dbInstance = describeDBInstancesResult.getDBInstances().iterator().next();
        Optional<DBParameterGroupStatus> statusOptional = dbInstance.getDBParameterGroups().stream()
            .filter(dbParameterGroupStatus -> PENDING_REBOOT_STATUS
                .equals(dbParameterGroupStatus.getParameterApplyStatus()))
            .findAny();
        if (statusOptional.isPresent()) {
            buildLogger.addLogEntry(String
                .format("... DB param group %s has a status of %s", statusOptional.get().getDBParameterGroupName(),
                    PENDING_REBOOT_STATUS));
            return true;
        } else {
            return false;
        }
    }

    void rebootDb(String instanceId) {
        buildLogger.addLogEntry("... Rebooting DB: " + instanceId);
        client.rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId));
    }

    OptionConfiguration getOptionConfiguration(Option option) {
        return new OptionConfiguration()
            .withOptionName(option.getOptionName())
            .withDBSecurityGroupMemberships(option.getDBSecurityGroupMemberships().stream()
                .map(DBSecurityGroupMembership::getDBSecurityGroupName)
                .collect(Collectors.toList()))
            .withVpcSecurityGroupMemberships(option.getVpcSecurityGroupMemberships().stream()
                .map(VpcSecurityGroupMembership::getVpcSecurityGroupId)
                .collect(Collectors.toList()))
            .withOptionSettings(option.getOptionSettings())
            .withOptionVersion(option.getOptionVersion())
            .withPort(option.getPort());
    }

    private void applyTags(String instanceArn, List<Tag> tags) {
        client.addTagsToResource(
            new AddTagsToResourceRequest().withResourceName(instanceArn).withTags(tags));
        buildLogger.addLogEntry("... DB tags applied: " + instanceArn + " : " + tags);
    }

    private DBInstance getDbInstance(String instanceId) {
        DescribeDBInstancesResult searchResult = client.describeDBInstances(
            new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId));
        return searchResult.getDBInstances().get(0);
    }

    private DescribeDBSnapshotsResult getDbSnapshot(String snapshotId) {
        DescribeDBSnapshotsRequest req = new DescribeDBSnapshotsRequest()
            .withDBSnapshotIdentifier(snapshotId);
        return client.describeDBSnapshots(req);
    }
}
