package com.libertymutualgroup.herman.aws.ecs.broker.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.logging.HermanLogger;
import java.util.ArrayList;
import java.util.List;

public class SqlServerClient extends StandardRdsClient {

  private AmazonRDS client;
  private RdsInstance rds;
  private EcsClusterMetadata clusterMetadata;
  private List<HermanTag> tags;
  private HermanLogger logger;

  SqlServerClient(AmazonRDS client, RdsInstance rds, EcsClusterMetadata clusterMetadata, List<HermanTag> tags,
      HermanLogger logger) {
    super(client, rds, clusterMetadata, tags, logger);
    this.client = client;
    this.rds = rds;
    this.clusterMetadata = clusterMetadata;
    this.tags = tags;
    this.logger = logger;
  }

  @Override
  public void setDefaults(String kmsKeyId) {
    rds.setInjectNames(rds.getInjectNames() == null ? new RdsInjectConfiguration() : rds.getInjectNames());

    rds.getInjectNames().setDefaults();

    rds.setMasterUsername(rds.getMasterUsername() == null ? "dbAdmin" : rds.getMasterUsername());
    rds.setDBInstanceClass(rds.getDBInstanceClass() == null ? "db.m4.large" : rds.getDBInstanceClass());
    rds.setAllocatedStorage(rds.getAllocatedStorage() == null ? 200 : rds.getAllocatedStorage());
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
}
