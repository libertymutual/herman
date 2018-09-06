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
import com.amazonaws.services.rds.model.CreateDBClusterParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBClusterRequest;
import com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterNotFoundException;
import com.amazonaws.services.rds.model.DBClusterParameterGroup;
import com.amazonaws.services.rds.model.DBClusterSnapshotNotFoundException;
import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DBParameterGroupNotFoundException;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;
import com.amazonaws.services.rds.model.DescribeDBClusterParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ModifyDBClusterParameterGroupRequest;
import com.amazonaws.services.rds.model.ModifyDBClusterRequest;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.OptionGroup;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.libertymutualgroup.herman.aws.ecs.broker.rds.RdsBroker.pollingIntervalMs;

public class AuroraClient implements RdsClient {

    public static final String AVAILABLE_STATUS = "available";
    public static final String INTERRUPTED_DURING_PROCESS = "Interrupted during process";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClient.class);
    private final static String PENDING_REBOOT_STATUS = "pending-reboot";
    private AmazonRDS client;
    private RdsInstance rds;
    private EcsClusterMetadata clusterMetadata;
    private List<Tag> tags;
    private HermanLogger buildLogger;

    AuroraClient(AmazonRDS client, RdsInstance rds, EcsClusterMetadata clusterMetadata, List<Tag> tags,
        HermanLogger buildLogger) {
        this.client = client;
        this.rds = rds;
        this.clusterMetadata = clusterMetadata;
        this.tags = tags;
        this.buildLogger = buildLogger;
    }

    @Override
    public Boolean dbExists(String clusterId) {
        buildLogger.addLogEntry("Looking for cluster: " + clusterId);

        DescribeDBClustersRequest request = new DescribeDBClustersRequest().withDBClusterIdentifier(clusterId);
        try {
            client.describeDBClusters(request);
            return true;
        } catch (DBClusterNotFoundException ex) {
            LOGGER.debug("DB cluster not found: " + clusterId, ex);
            return false;
        }
    }

    @Override
    public void createNewDb(String clusterId, String masterUserPassword) {
        buildLogger.addLogEntry("Creating new Aurora cluster and instance(s)");
        DBCluster cluster = this.createAuroraCluster(clusterId, masterUserPassword);
        this.waitForAvailableStatus(cluster.getDBClusterIdentifier());

        buildLogger.addLogEntry("Creating Aurora primary");
        DBInstance instance = this.createAuroraInstance(rds.getAvailabilityZones()[0], cluster, clusterId);
        this.waitForAvailableClusterInstance(instance.getDBInstanceIdentifier());

        if (rds.getAvailabilityZones().length > 1) { // Create read replicas
            buildLogger.addLogEntry("Creating Aurora Replicas");
            for (String az : rds.getAvailabilityZones()) {
                if (az.equals(rds.getAvailabilityZones()[0])) {
                    continue; // Skip primary AZ
                }
                this.createAuroraInstance(az, cluster, clusterId);
            }
        }
    }

    @Override
    public void updateMasterPassword(String clusterId, String masterUserPassword) {
        buildLogger.addLogEntry("Modifying cluster: " + clusterId);

        ModifyDBClusterRequest clusterRequest = new ModifyDBClusterRequest().withDBClusterIdentifier(clusterId)
            .withMasterUserPassword(masterUserPassword).withApplyImmediately(true);
        client.modifyDBCluster(clusterRequest);
    }


    @Override
    public void runFullUpdate(String instanceId, String masterUserPassword) {
        DBCluster cluster = this.fullUpdateAuroraCluster(instanceId, masterUserPassword);

        for (String az : rds.getAvailabilityZones()) {
            this.modifyCreateAuroraInstance(az, cluster, instanceId);
        }
    }

    @Override
    public void waitForAvailableStatus(String clusterId) {
        buildLogger.addLogEntry("... Waiting for Aurora cluster to be available.");
        try {
            DBCluster clusterResult;
            do {
                Thread.sleep(pollingIntervalMs);
                clusterResult = this.getDbCluster(clusterId);
                buildLogger.addLogEntry("... Cluster status: " + clusterResult.getStatus());
            } while (!clusterResult.getStatus().equals(AVAILABLE_STATUS));

            buildLogger.addLogEntry("... Cluster Result: " + clusterResult);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException(INTERRUPTED_DURING_PROCESS);
        }
    }

    @Override
    public Endpoint getEndpoint(String clusterIdentifier) {
        DBCluster cluster = getDbCluster(clusterIdentifier);
        return new Endpoint().withAddress(cluster.getEndpoint()).withPort(cluster.getPort());
    }

    @Override
    public String getDbiResourceId(String clusterIdentifer) {
        DBCluster cluster = getDbCluster(clusterIdentifer);
        return cluster.getDbClusterResourceId();
    }

    @Override
    public void setDBParameterGroup(String clusterId, List<Parameter> parameters) {
        DBClusterParameterGroup dbClusterParameterGroup;
        DBCluster rdsResult = getDbCluster(clusterId);

        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = client.describeDBEngineVersions(
            new DescribeDBEngineVersionsRequest().withEngine(rdsResult.getEngine())
                .withEngineVersion(rdsResult.getEngineVersion()));
        DBEngineVersion dbEngineVersion = describeDBEngineVersionsResult.getDBEngineVersions().iterator().next();
        final String dbClusterParameterGroupName = String
            .format("%s-%s", dbEngineVersion.getDBParameterGroupFamily().replaceAll("\\.", "-"),
                rdsResult.getDBClusterIdentifier());
        buildLogger.addLogEntry("DB cluster parameter group name = " + dbClusterParameterGroupName);

        try {
            DescribeDBClusterParameterGroupsResult describeDBClusterParameterGroupsResult = client
                .describeDBClusterParameterGroups(new DescribeDBClusterParameterGroupsRequest()
                    .withDBClusterParameterGroupName(dbClusterParameterGroupName));
            dbClusterParameterGroup = describeDBClusterParameterGroupsResult.getDBClusterParameterGroups().iterator()
                .next();

            buildLogger
                .addLogEntry("Updating parameters for DB cluster parameter group " + dbClusterParameterGroupName);
            client.modifyDBClusterParameterGroup(new ModifyDBClusterParameterGroupRequest()
                .withDBClusterParameterGroupName(dbClusterParameterGroupName)
                .withParameters(parameters));

        } catch (DBParameterGroupNotFoundException ex) {
            LOGGER.debug("DB cluster parameter group for found: " + dbClusterParameterGroupName, ex);

            buildLogger.addLogEntry("Creating DB cluster parameter group " + dbClusterParameterGroupName);
            dbClusterParameterGroup = client.createDBClusterParameterGroup(new CreateDBClusterParameterGroupRequest()
                .withDBParameterGroupFamily(dbEngineVersion.getDBParameterGroupFamily())
                .withDBClusterParameterGroupName(dbClusterParameterGroupName)
                .withDescription(String.format("%s %s Parameter Group", rdsResult.getDBClusterIdentifier(),
                    dbEngineVersion.getDBParameterGroupFamily()))
                .withTags(tags));

            buildLogger
                .addLogEntry("Updating parameters for DB cluster parameter group " + dbClusterParameterGroupName);
            client.modifyDBClusterParameterGroup(new ModifyDBClusterParameterGroupRequest()
                .withDBClusterParameterGroupName(dbClusterParameterGroupName)
                .withParameters(parameters));
        }

        boolean dbParameterGroupIsSet = dbClusterParameterGroupName.equals(rdsResult.getDBClusterParameterGroup());
        if (!dbParameterGroupIsSet) {
            buildLogger.addLogEntry(
                "Modifying DB cluster cluster to use parameter group " + dbClusterParameterGroup
                    .getDBClusterParameterGroupName());
            client.modifyDBCluster(new ModifyDBClusterRequest()
                .withDBClusterIdentifier(rdsResult.getDBClusterIdentifier())
                .withDBClusterParameterGroupName(dbClusterParameterGroup.getDBClusterParameterGroupName()));
        }

        this.waitForAvailableStatus(clusterId);
        for (String az : rds.getAvailabilityZones()) {
            String instanceId = clusterIdToInstanceId(clusterId, az);
            if (this.dbParamGroupHasPendingRebootStatus(instanceId)) {
                this.rebootDb(instanceId);
                this.waitForAvailableStatus(instanceId);
            }
        }
    }

    @Override
    public void setOptionGroup(String instanceId, OptionGroup options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createSnapshot(String instanceId, String snapshotId) {
        CreateDBClusterSnapshotRequest request = new CreateDBClusterSnapshotRequest()
            .withDBClusterIdentifier(instanceId)
            .withDBClusterSnapshotIdentifier(snapshotId);
        client.createDBClusterSnapshot(request);
        try {
            DescribeDBClusterSnapshotsResult result = getDescribeDBClusterSnapshotsResult(snapshotId);
            buildLogger.addLogEntry("DB Cluster Snapshot Result: " + result.getDBClusterSnapshots().get(0));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException(INTERRUPTED_DURING_PROCESS);
        }
    }

    private DescribeDBClusterSnapshotsResult getDescribeDBClusterSnapshotsResult(String snapshotId)
        throws InterruptedException {
        String status = "";
        DescribeDBClusterSnapshotsResult result = null;

        buildLogger.addLogEntry("Waiting for snapshot to be created...");
        do {
            Thread.sleep(pollingIntervalMs);
            try {
                result = this.getDbClusterSnapshot(snapshotId);
                if (result.getDBClusterSnapshots().size() == 1) {
                    status = result.getDBClusterSnapshots().get(0).getStatus();
                }
                buildLogger.addLogEntry("... Snapshot status: " + status);
            } catch (DBClusterSnapshotNotFoundException ex) {
                LOGGER.debug("DB cluster snapshot not found: " + snapshotId, ex);
                buildLogger.addLogEntry("... Waiting for snapshot creation");
            }
        } while (result == null || !status.equals(AVAILABLE_STATUS));
        return result;
    }

    private DBCluster createAuroraCluster(String clusterId, String masterUserPassword) {
        buildLogger.addLogEntry("... Creating cluster: " + clusterId);

        if (rds.getAdditionalSecGroups() == null) {
            rds.setAdditionalSecGroups(new ArrayList<String>());
        }
        rds.getAdditionalSecGroups().add(clusterMetadata.getRdsSecurityGroup());

        CreateDBClusterRequest createDbClusterRequest = new CreateDBClusterRequest();
        createDbClusterRequest.withAvailabilityZones(rds.getAvailabilityZones())
            .withBackupRetentionPeriod(rds.getBackupRetentionPeriod())
            .withCharacterSetName(rds.getCharacterSetName())
            .withDBClusterIdentifier(clusterId)
            .withDatabaseName(rds.getDBName())
            .withDBSubnetGroupName(clusterMetadata.getDbSubnetGroup())
            .withEngine(rds.getEngine())
            .withEngineVersion(rds.getEngineVersion())
            .withKmsKeyId(rds.getKmsKeyId())
            .withMasterUsername(rds.getMasterUsername())
            .withMasterUserPassword(masterUserPassword)
            .withPort(rds.getDbInstancePort())
            .withPreferredBackupWindow(rds.getPreferredBackupWindow())
            .withPreferredMaintenanceWindow(rds.getPreferredMaintenanceWindow())
            .withStorageEncrypted(true)
            .withTags(tags)
            .withEnableIAMDatabaseAuthentication(rds.getIAMDatabaseAuthenticationEnabled())
            .withVpcSecurityGroupIds(rds.getAdditionalSecGroups());
        return client.createDBCluster(createDbClusterRequest);
    }

    private DBCluster fullUpdateAuroraCluster(String clusterId, String masterUserPassword) {
        buildLogger.addLogEntry("Modifying cluster: " + clusterId);

        if (rds.getAdditionalSecGroups() == null) {
            rds.setAdditionalSecGroups(new ArrayList<String>());
        }
        rds.getAdditionalSecGroups().add(clusterMetadata.getRdsSecurityGroup());

        ModifyDBClusterRequest clusterRequest = new ModifyDBClusterRequest();
        clusterRequest.withBackupRetentionPeriod(rds.getBackupRetentionPeriod())
            .withDBClusterIdentifier(clusterId)
            .withMasterUserPassword(masterUserPassword)
            .withPort(rds.getDbInstancePort()).withPreferredBackupWindow(rds.getPreferredBackupWindow())
            .withPreferredMaintenanceWindow(rds.getPreferredMaintenanceWindow())
            .withEnableIAMDatabaseAuthentication(rds.getIAMDatabaseAuthenticationEnabled())
            .withVpcSecurityGroupIds(rds.getAdditionalSecGroups()).withApplyImmediately(true);
        DBCluster cluster = client.modifyDBCluster(clusterRequest);
        this.applyTags(cluster.getDBClusterArn(), tags);
        return cluster;
    }

    private DBInstance createAuroraInstance(String availabilityZone, DBCluster cluster, String clusterId) {
        String instanceIdentifier = this.clusterIdToInstanceId(clusterId, availabilityZone);
        buildLogger.addLogEntry("... Creating instance: " + instanceIdentifier);

        CreateDBInstanceRequest createReplicaRequest = new CreateDBInstanceRequest();
        createReplicaRequest.withAvailabilityZone(availabilityZone)
            .withAutoMinorVersionUpgrade(rds.getAutoMinorVersionUpgrade())
            .withCharacterSetName(rds.getCharacterSetName())
            .withCopyTagsToSnapshot(rds.getCopyTagsToSnapshot())
            .withDBClusterIdentifier(cluster.getDBClusterIdentifier())
            .withDBInstanceClass(rds.getDBInstanceClass())
            .withDBInstanceIdentifier(instanceIdentifier)
            .withDBSubnetGroupName(clusterMetadata.getDbSubnetGroup())
            .withEngine(rds.getEngine())
            .withEngineVersion(rds.getEngineVersion())
            .withIops(rds.getIops())
            .withLicenseModel(rds.getLicenseModel())
            .withMonitoringInterval(rds.getMonitoringInterval())
            .withMonitoringRoleArn(rds.getMonitoringRoleArn())
            .withPubliclyAccessible(rds.getPubliclyAccessible())
            .withStorageEncrypted(true)
            .withTags(tags)
            .withTimezone(rds.getTimezone());

        return client.createDBInstance(createReplicaRequest);
    }

    private void fullUpdateAuroraInstance(String availabilityZone, String clusterId) {
        String instanceIdentifier = this.clusterIdToInstanceId(clusterId, availabilityZone);
        buildLogger.addLogEntry("Modifying instance: " + instanceIdentifier);

        ModifyDBInstanceRequest modifyDBInstanceRequest = new ModifyDBInstanceRequest();
        modifyDBInstanceRequest
            .withAutoMinorVersionUpgrade(rds.getAutoMinorVersionUpgrade())
            .withCopyTagsToSnapshot(rds.getCopyTagsToSnapshot())
            .withDBInstanceClass(rds.getDBInstanceClass())
            .withDBInstanceIdentifier(instanceIdentifier)
            .withEngineVersion(rds.getEngineVersion())
            .withIops(rds.getIops()).withLicenseModel(rds.getLicenseModel())
            .withMonitoringInterval(rds.getMonitoringInterval())
            .withMonitoringRoleArn(rds.getMonitoringRoleArn())
            .withPubliclyAccessible(rds.getPubliclyAccessible());
        DBInstance dbInstance = client.modifyDBInstance(modifyDBInstanceRequest);
        this.applyTags(dbInstance.getDBInstanceArn(), tags);
    }

    private void modifyCreateAuroraInstance(String availabilityZone, DBCluster cluster, String clusterId) {
        try {
            fullUpdateAuroraInstance(availabilityZone, clusterId);
        } catch (DBInstanceNotFoundException ex) {
            LOGGER.debug(String.format("DB instance not found: %s in the %s AZ", clusterId, availabilityZone), ex);
            createAuroraInstance(availabilityZone, cluster, clusterId);
        }
    }

    private void waitForAvailableClusterInstance(String instanceId) {
        try {
            DBInstance rdsResult;
            do {
                Thread.sleep(pollingIntervalMs);
                DescribeDBInstancesResult searchResult = client.describeDBInstances(
                    new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId));
                rdsResult = searchResult.getDBInstances().get(0);
                buildLogger.addLogEntry("... Instance status: " + rdsResult.getDBInstanceStatus());
            } while (!rdsResult.getDBInstanceStatus().equals(AVAILABLE_STATUS)
                || rdsResult.getPendingModifiedValues().getMasterUserPassword() != null);

            buildLogger.addLogEntry("RDS Result: " + rdsResult);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException(INTERRUPTED_DURING_PROCESS);
        }
    }

    private void applyTags(String resourceArn, List<Tag> tags) {
        client.addTagsToResource(
            new AddTagsToResourceRequest().withResourceName(resourceArn).withTags(tags));
    }

    private DBCluster getDbCluster(String clusterIdentifier) {
        DescribeDBClustersResult searchResult = client.describeDBClusters(
            new DescribeDBClustersRequest().withDBClusterIdentifier(clusterIdentifier));
        return searchResult.getDBClusters().get(0);
    }

    private String clusterIdToInstanceId(String clusterId, String availabilityZone) {
        return clusterId + "-" + availabilityZone;
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
                .format("DB param group %s has a status of %s", statusOptional.get().getDBParameterGroupName(),
                    PENDING_REBOOT_STATUS));
            return true;
        } else {
            return false;
        }
    }

    void rebootDb(String instanceId) {
        buildLogger.addLogEntry("Rebooting DB: " + instanceId);
        client.rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId));
    }

    private DescribeDBClusterSnapshotsResult getDbClusterSnapshot(String snapshotId) {
        DescribeDBClusterSnapshotsRequest req = new DescribeDBClusterSnapshotsRequest()
            .withDBClusterSnapshotIdentifier(snapshotId);
        return client.describeDBClusterSnapshots(req);
    }
}
