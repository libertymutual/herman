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
package com.libertymutualgroup.herman.aws.ecs.broker.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateGlobalSecondaryIndexAction;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteGlobalSecondaryIndexAction;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexUpdate;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateGlobalSecondaryIndexAction;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DynamoDBBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBBroker.class);
    private static final String INTERRUPTED_WHILE_POLLING = "Interrupted while polling";

    private HermanLogger buildLogger;
    private DynamoAppDefinition pushDefinition;

    /**
     * Default constructor
     */
    public DynamoDBBroker(HermanLogger buildLogger, DynamoAppDefinition pushDefinition) {
        this.buildLogger = buildLogger;
        this.pushDefinition = pushDefinition;
    }

    /**
     * Entry point
     */
    public void createDynamoDBTables(AmazonDynamoDB client) {
        for (DynamoDBTable table : pushDefinition.getDynamoDBTables()) {
            // prefix table name with app name
            table.setTableName(pushDefinition.getAppName() + "-" + table.getTableName());
            brokerDynamoDBTable(client, table);
        }
    }

    /**
     * Creates and updates tables
     */
    private void brokerDynamoDBTable(AmazonDynamoDB client, DynamoDBTable table) {
        buildLogger.addLogEntry("Brokering table: " + table.getTableName());

        // Check if table exists
        DescribeTableResult describeTableResult = null;
        try {
            describeTableResult = client.describeTable(table.getTableName());
        } catch (RuntimeException e) {
            LOGGER.debug("Error getting DescribeTableResult for " + table.getTableName(), e);
        }

        // Check if updating or new
        if (describeTableResult != null) {
            updateTable(client, table, describeTableResult);
        } else {
            // If the table is new, create it
            createTable(client, table);
        }
        if (this.pushDefinition.getTags() != null) {
            tagTable(client, table.getTableName(), this.pushDefinition.getTags());
        }
    }

    /**
     * Create a table
     */
    private void createTable(AmazonDynamoDB client, DynamoDBTable table) {
        buildLogger.addLogEntry("Creating table: " + table.getTableName());
        buildLogger.addLogEntry("Table spec: \n" + table.toString());
        CreateTableRequest createTableRequest = new CreateTableRequest(table.getAttributes(), table.getTableName(),
            table.getKeySchema(), table.getProvisionedThroughput());

        if (table.getSseSpecification() != null) {
            createTableRequest.setSSESpecification(table.getSseSpecification());
        }

        if (table.getLocalSecondaryIndexes() != null) {
            createTableRequest.setLocalSecondaryIndexes(table.getLocalSecondaryIndexes());
        }
        if (table.getGlobalSecondaryIndexes() != null) {
            createTableRequest.setGlobalSecondaryIndexes(table.getGlobalSecondaryIndexes());
        }

        if (table.getStreamSpecification() != null) {
            createTableRequest.setStreamSpecification(table.getStreamSpecification());
        }

        CreateTableResult tableResult = client.createTable(createTableRequest);

        boolean success = waitForIt(client, table.getTableName());

        if (!success) {
            buildLogger.addErrorLogEntry(
                "Seems we failed to create the table " + table.getTableName() + " . Check AWS console");
        }
    }

    /**
     * Updates a table enumerating on updatable elements
     * Things that can't be updated are silently ignored
     */
    private void updateTable(AmazonDynamoDB client, DynamoDBTable table, DescribeTableResult describeTableResult) {
        // check if anything needs to be updated and if so, update
        // Updates need to be done one by one and you have to wait for the update to finish before applying the next

        buildLogger.addLogEntry("Checking for updates on: " + table.getTableName());

        // Update attributes
        if (!checkAndUpdateAttributes(client, table, describeTableResult)) {
            buildLogger.addErrorLogEntry("Something has gone wrong updating attributes and is now abandoning updates");
            return;
        }

        // Update global indexes
        if (!checkAndUpdateGlobalSecondaryIndex(client, table, describeTableResult)) {
            buildLogger
                .addErrorLogEntry("Something went wrong with updating index on table and is now abandoning updates");
            return;
        }

        // Update provision throughput
        if (!checkAndUpdateProvisionedThroughputs(client, table, describeTableResult)) {
            buildLogger.addErrorLogEntry(
                "Something went wrong with updating provionedThroughput and is now abandoning updates");
            return;
        }

        // Update stream specification
        if (!checkAndUpdateStreamSpecification(client, table, describeTableResult)) {
            buildLogger.addErrorLogEntry(
                "Something went wrong with updating streamSpecification and is now abandoning updates");
            return;
        }

    }

    private void tagTable(AmazonDynamoDB client, String tableName, List<HermanTag> tags) {
        this.buildLogger.addLogEntry("...Setting tags on table " + tableName);
        DescribeTableResult describeResult = client.describeTable(tableName);
        TagResourceRequest tagRequest = new TagResourceRequest()
            .withResourceArn(describeResult.getTable().getTableArn())
            .withTags(TagUtil.hermanToDynamoTags(tags));
        client.tagResource(tagRequest);
    }

    /**
     * @return true = success / false = fail
     */
    private boolean checkAndUpdateStreamSpecification(AmazonDynamoDB client, DynamoDBTable table,
        DescribeTableResult describeTableResult) {

        if (describeTableResult.getTable().getStreamSpecification() == null && table.getStreamSpecification() == null) {
            return true;
        }

        if (checkIfStreamIsDifferent(describeTableResult.getTable(), table)) {
            buildLogger.addLogEntry("Updating Stream Specification");
            UpdateTableRequest updateTableRequest = new UpdateTableRequest();
            updateTableRequest.setTableName(table.getTableName());
            updateTableRequest.setStreamSpecification(table.getStreamSpecification());
            client.updateTable(updateTableRequest);
            return waitForIt(client, table.getTableName());
        }

        // no change
        return true;
    }

    private boolean checkIfStreamIsBeingEnabled(TableDescription currentTable, DynamoDBTable updatedTable) {
        return (currentTable.getStreamSpecification() == null && updatedTable.getStreamSpecification() != null
            && updatedTable.getStreamSpecification().isStreamEnabled());
    }

    private boolean checkIfStreamIsDifferent(TableDescription currentTable, DynamoDBTable updatedTable) {
        if (updatedTable.getStreamSpecification().isStreamEnabled() == null && currentTable.getStreamSpecification().getStreamEnabled() == null) {
            return false;
        }

        // If stream was off and is now on
        if (checkIfStreamIsBeingEnabled(currentTable, updatedTable)) {
            return true;
        }


        // If stream was off and is still off
        if (!updatedTable.getStreamSpecification().isStreamEnabled() && currentTable.getStreamSpecification() == null) {
            return false;
        }

        // If stream is on, return if changed
        return !updatedTable.getStreamSpecification().isStreamEnabled()
            .equals(currentTable.getStreamSpecification().isStreamEnabled())
            || !updatedTable.getStreamSpecification().getStreamViewType()
            .equals(currentTable.getStreamSpecification().getStreamViewType());
    }

    /**
     * @return true = success / false = failed
     */
    private boolean checkAndUpdateProvisionedThroughputs(AmazonDynamoDB client, DynamoDBTable table,
        DescribeTableResult describeTableResult) {
        if (!table.getProvisionedThroughput().getReadCapacityUnits().equals(describeTableResult.getTable().getProvisionedThroughput().getReadCapacityUnits()) ||
            !table.getProvisionedThroughput().getWriteCapacityUnits().equals(describeTableResult.getTable().getProvisionedThroughput().getWriteCapacityUnits())) {
            buildLogger.addLogEntry("Updating Provisioned Throughput");
            UpdateTableRequest updateTableRequest = new UpdateTableRequest();
            updateTableRequest.setTableName(table.getTableName());
            updateTableRequest.setProvisionedThroughput(table.getProvisionedThroughput());
            client.updateTable(updateTableRequest);
            return waitForIt(client, table.getTableName());
        }

        // no change
        return true;
    }

    /**
     * @return true if success / false if not
     */
    private boolean checkAndUpdateGlobalSecondaryIndex(AmazonDynamoDB client, DynamoDBTable table,
        DescribeTableResult describeTableResult) {
        List<GlobalSecondaryIndex> globalSecondaryIndexes = table.getGlobalSecondaryIndexes();
        List<GlobalSecondaryIndexDescription> currentGlobalSecondaryIndexes = describeTableResult.getTable()
            .getGlobalSecondaryIndexes();
        List<GlobalSecondaryIndexUpdate> globalSecondaryIndexUpdate = new ArrayList<>();

        if (globalSecondaryIndexes == null) {
            globalSecondaryIndexes = new ArrayList<>();
        }

        if (currentGlobalSecondaryIndexes == null) {
            currentGlobalSecondaryIndexes = new ArrayList<>();
        }

        for (GlobalSecondaryIndex globalSecondaryIndex : globalSecondaryIndexes) {
            // Check for new
            boolean found = false;
            for (GlobalSecondaryIndexDescription current : currentGlobalSecondaryIndexes) {
                if (current.getIndexName().equals(globalSecondaryIndex.getIndexName())) {
                    // check for updates (only provisioned throughput is allowed)
                    if (!current.getProvisionedThroughput().getReadCapacityUnits()
                        .equals(globalSecondaryIndex.getProvisionedThroughput().getReadCapacityUnits()) ||
                        !current.getProvisionedThroughput().getWriteCapacityUnits()
                            .equals(globalSecondaryIndex.getProvisionedThroughput().getWriteCapacityUnits())) {
                        // push update
                        UpdateGlobalSecondaryIndexAction updateAction = new UpdateGlobalSecondaryIndexAction();
                        updateAction.setIndexName(globalSecondaryIndex.getIndexName());
                        updateAction.setProvisionedThroughput(globalSecondaryIndex.getProvisionedThroughput());
                        buildLogger.addLogEntry("Updating index for " + table.getTableName());
                        globalSecondaryIndexUpdate.add(new GlobalSecondaryIndexUpdate().withUpdate(updateAction));
                    }
                    found = true;
                }
            }

            if (!found) {
                buildLogger.addLogEntry("Creating index on " + table.getTableName());
                CreateGlobalSecondaryIndexAction createAction = new CreateGlobalSecondaryIndexAction()
                    .withIndexName(globalSecondaryIndex.getIndexName())
                    .withKeySchema(globalSecondaryIndex.getKeySchema())
                    .withProjection(globalSecondaryIndex.getProjection())
                    .withProvisionedThroughput(globalSecondaryIndex.getProvisionedThroughput());
                globalSecondaryIndexUpdate.add(new GlobalSecondaryIndexUpdate().withCreate(createAction));
            }
        }

        // Now loop the other way to look for deletes
        for (GlobalSecondaryIndexDescription current : currentGlobalSecondaryIndexes) {
            boolean found = false;
            for (GlobalSecondaryIndex globalSecondaryIndex : globalSecondaryIndexes) {
                if (globalSecondaryIndex.getIndexName().equals(current.getIndexName())) {
                    found = true;
                }
            }
            if (!found) {
                buildLogger.addLogEntry("Deleting index on " + table.getTableName());
                DeleteGlobalSecondaryIndexAction deleteAction = new DeleteGlobalSecondaryIndexAction();
                deleteAction.setIndexName(current.getIndexName());
                globalSecondaryIndexUpdate.add(new GlobalSecondaryIndexUpdate().withDelete(deleteAction));
            }
        }

        // push the global index change
        if (!globalSecondaryIndexUpdate.isEmpty()) {
            buildLogger.addLogEntry("Updating global secondary indexes");
            UpdateTableRequest updateTableRequest = new UpdateTableRequest();
            updateTableRequest.setGlobalSecondaryIndexUpdates(globalSecondaryIndexUpdate);
            client.updateTable(updateTableRequest);
            return waitForIt(client, table.getTableName());
        }

        // No change
        return true;
    }

    /**
     * @return Success = true / failed = false
     */
    private boolean checkAndUpdateAttributes(AmazonDynamoDB client, DynamoDBTable table,
        DescribeTableResult describeTableResult) {
        final List<AttributeDefinition> attributes = table.getAttributes();
        final List<AttributeDefinition> existingAttributes = describeTableResult.getTable().getAttributeDefinitions();
        buildLogger.addLogEntry("Checking for attribute updates");

        // Check if update needed
        if (!existingAttributes.containsAll(attributes) || !attributes.containsAll(existingAttributes)) {

            // Create update request
            buildLogger.addLogEntry("Found attribute change, updating");
            UpdateTableRequest updateTableRequest = new UpdateTableRequest();
            updateTableRequest.setAttributeDefinitions(attributes);

            // make update
            UpdateTableResult updateTableResult = client.updateTable(updateTableRequest);
            if (!updateTableResult.getTableDescription().getAttributeDefinitions().containsAll(attributes)) {
                buildLogger.addErrorLogEntry("Updating attribute definition seems to have failed");
            }

            // wait for update to apply
            boolean result = waitForIt(client, table.getTableName());
            if (result) {
                buildLogger.addLogEntry("Attributes updated");
            } else {
                buildLogger.addLogEntry("Timed out waiting for attribute update");
            }
            return result;
        }

        // Nothing to do
        return true;

    }

    /**
     * Waits for table to not be in "UPDATING" or "CREATING" (deleting is fine cause what's to wait for?)
     */
    private boolean waitForIt(AmazonDynamoDB client, String tableName) {

        // wait at most 60 seconds?
        int maxCount = 20;
        int currentCount = 0;
        int wait = 3000;
        int pause = 1000;

        // pause to let AWS react to the request
        try {
            Thread.sleep(pause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            buildLogger.addLogEntry(INTERRUPTED_WHILE_POLLING);
            throw new AwsExecException(INTERRUPTED_WHILE_POLLING);
        }

        String tableStatus = client.describeTable(tableName).getTable().getTableStatus();
        buildLogger.addLogEntry("Table status is " + tableStatus + "...");
        while (tableStatus.matches("CREATING|UPDATING")) {

            if (currentCount > maxCount) {
                buildLogger.addErrorLogEntry("Waiting too long to update/create table, exiting");
                return false;
            }
            currentCount++;
            try {
                Thread.sleep(wait);
                tableStatus = client.describeTable(tableName).getTable().getTableStatus();
                buildLogger.addLogEntry("... Status: " + tableStatus);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                buildLogger.addLogEntry(INTERRUPTED_WHILE_POLLING);
                throw new AwsExecException(INTERRUPTED_WHILE_POLLING);
            }

        }
        return true;
    }
}
