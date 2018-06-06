# Brokered Services - ECS Sidecars

ECS apps can stand alone or leverage a number of external services that
Herman can configure. The configuration for these services lives right
alongside your app manifest, so they're configured and deployed together
as a single unit.

### Convenience Variables

Policy documents often need two data elements - they're available via
injection for convenience:

-   ***${app.iam}** *- this is your app's IAM id
    -   useful for when you're providing access to your own application
-   ***${account.id}** *- the number of the AWS Account (different
    between NP and PROD).
    -   Useful for when referencing other application IAM Roles (giving
        the other app access to your resource)
    -   Useful when referencing the resource at hand (such as queue ID  
          

AWS Services that Herman supports brokering as an ECS sidecar:

-   [RDS Databases](brokeredServices/RDS_Databases.md)
-   [DynamoDB Tables](brokeredServices/DynamoDB_Tables.md)
-   [S3 Buckets](brokeredServices/S3_Buckets.md)
-   [SNS Topics](brokeredServices/SNS_Topics.md)
-   [SQS Queues](brokeredServices/SQS_Queues.md)
-   [Kinesis Streams](brokeredServices/Kinesis_Streams.md)
-   [IAM Roles](brokeredServices/IAM_Roles.md)
-   [KMS Keys](brokeredServices/KMS_Keys.md)
