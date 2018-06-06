# Kinesis Streams

The Kinesis Broker uses the "streams" key in your ECS manifest. This
takes a list of Kinesis stream definitions, each of which take the
following configuration values:

## Kinesis Stream Definition

| Name       | Type   | Required | Description                                |
|------------|--------|----------|--------------------------------------------|
| name       | String | YES      | Name of the provisioned Kinesis stream     |
| shardCount | Int    | YES      | Number of shard provisioned for the stream |

  

Note that Herman brokered Kinesis streams currently will **always
enable** encryption using the default alias/aws/kinesis key in your
account.
