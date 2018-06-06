# DynamoDB Tables

This broker provides [AWS DynamoDB
Tables](https://aws.amazon.com/dynamodb/). The configuration key
"dynamoDBTables" takes a list of DynamoDBTable definition objects which
currently supports the following fields:

## DynamoDBTable Definition

| Name                   | Type                                                                                                                                        | Required | Description                                                                   |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------|
| tableName              | String                                                                                                                                      | YES      | Name of provisioned table                                                     |
| attributes             | List&lt;[AttributeDefinition](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-attributedef.html)&gt; | YES      | Attributes that define the key schema for the table                           |
| globalSecondaryIndexes | List&lt;[GlobalSecondaryIndex](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-gsi.html)&gt;         | NO       | Global secondary indexes to be created on the table (Max 5)                   |
| keySchema              | List&lt;[KeySchemaElement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-keyschema.html)&gt;       | YES      | Specifies attribute that make up the table's primary key                      |
| localSecondaryIndexes  | List&lt;[LocalSecondaryIndex](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-lsi.html)&gt;          | NO       | Local secondary indexes to be created on the table                            |
| provisionedThroughput  | [ProvisionedThroughput](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-provisionedthroughput.html)  | YES      | ReadCapacityUnits and WriteCapacityUnits provisioned for the table            |
| sseSpecification       | [SSESpecification](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-table-ssespecification.html)      | NO       | Specifies server-side encryption. Default = Unencrypted                       |
| streamSpecification    | [StreamSpecification](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-dynamodb-streamspecification.html)      | NO       | Settings for DyanmoDB Table stream, used to capture changes made to the table |

  

## Creating Table

Tables create with the name "&lt;appName&gt;-&lt;tableName&gt;". So an
app will have different table names for each environment. For example, a
service call "some-service" with a table specified as "the-table" with
environments "non-prod" and "prod", the table will be called
"some-service-nonprod-the-table" and "some-service-prod-the-table". 

An IAM policy is also created for the table which restricts access to
the deploying service and grants all authorities on the table.

## Updating Table

DyanmoDB is limited in what can be edited post creation. Local Secondary
Indexes can only be created when the table is created and is not
editable.

Editable properties:

-   Attributes
-   Global Indexes
-   Provision Throughput
-   Stream Specification

## Deleting Table

The broker does not support automatically deleting a table to prevent
accidental data loss. Deletion of the table needs to be done through the
console (or CLI). Also remember to delete the IAM policy created for the
table. 

## Missing Features

-   Table backups are not configured by the broker
-   Auto scaling
-   Global Tables
-   Triggers

## Manifest Example

**template.json**

``` js
"dynamoDBTables": [
    {
      "tableName": "count-table",
      "attributes" : [
        {
          "attributeName": "topic",
          "attributeType": "S"
        },
        {
          "attributeName": "position",
          "attributeType": "N"
        },
        {
          "attributeName": "time",
          "attributeType": "N"
        }
      ],
      "keySchema": [
        {
          "attributeName": "topic",
          "keyType": "HASH"
        },
        {
          "attributeName": "position",
          "keyType": "RANGE"
        }
      ],
      "localSecondaryIndexes": [
        {
          "indexName": "timeIndex",
          "keySchema": [
            {
              "attributeName": "topic",
              "keyType": "HASH"
            },
            {
              "attributeName": "time",
              "keyType": "RANGE"
            }
          ],
          "projection": {
            "projectionType": "KEYS_ONLY"
          }
        }
      ],
      "provisionedThroughput": {
        "readCapacityUnits": 5,
        "writeCapacityUnits": 2
      },
      "sseSpecification": {
        "enabled": true
      },
      "streamSpecification": {
        "streamEnabled": true,
        "streamViewType": "NEW_IMAGE"
      },
      "tableName": "event-table"
    },
    {
      "attributes": [
        {
          "attributeName": "topic",
          "attributeType": "S"
        }
      ],
      "keySchema": [
        {
          "attributeName": "topic",
          "keyType": "HASH"
        }
      ],
      "provisionedThroughput": {
        "readCapacityUnits": 2,
        "writeCapacityUnits": 2
      },
      "sseSpecification": {
        "enabled": true
      },
      "streamSpecification": {
        "streamEnabled": false
      }
    }
```

  

  
