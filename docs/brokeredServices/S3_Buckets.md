# S3 Buckets

The S3 Broker uses the "buckets" key in your ECS manifest. This takes a
list of bucket definitions, and each bucket definition currently
supports the following configuration values:

  

## Bucket Definition

| Name       | Type   | Required | Description                                                                                                                                                                                                                                                                                                         |
|------------|--------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name       | String | YES      | Name of the provisioned S3 bucket                                                                                                                                                                                                                                                                                   |
| policyName | String | YES      | Name to a JSON file which represents the [bucket policy](https://docs.aws.amazon.com/AmazonS3/latest/dev/example-bucket-policies.html) that will be applied to the bucket. This should be in Amazon [Access Policy Language](https://docs.aws.amazon.com/AmazonS3/latest/dev/access-policy-language-overview.html). |
| snsNotifications | List | NO | List of SNS Event Notifications for the S3 Bucket |
| lambdaNotifications | List | NO | List of Lambda Event Notifications for the S3 Bucket |

  

  

App template:

**template.yml**

``` java
cluster:  ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
buckets:
- name: my-excellent-bucket-${bamboo.deploy.environment}
  policyName: bucket-policy.json
  snsNotifications: 
  - name: test-sns-notification-event
    arn: arn:aws:sns:${aws.region}:${aws.account}:sns-topic-name
    events:
    - ObjectCreatedByPut
    - ObjectCreatedByPost
    - ObjectCreatedByCopy
    - ObjectCreatedByCompleteMultipartUpload  
```

Corresponding policy if only used by your application (specified by
policyName attribute):

**bucket-policy.json**

``` java
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "${app.iam}"
            },
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::my-excellent-bucket-${bamboo.deploy.environment}/*"
        }
    ]
}
```

Corresponding policy if sharing the bucket with another application:

**bucket-policy.json**

``` java
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "${app.iam}"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::my-excellent-bucket-${bamboo.deploy.environment}/*"
        },
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::${aws.account}:role/aws-ecs/some-other-app-that-puts-stuff"}"
            },
            "Action": "s3:PutObject",
            "Resource": "arn:aws:s3:::my-excellent-bucket-${bamboo.deploy.environment}/*"
        }
 ]
}
```

To use SNS Events, you must configure your SNS Topic Policy to be similar to below:

**sns-policy.json**

``` javascript
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "test-1",
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Principal": {
        "AWS": "*"
      },
      "Resource": "arn:aws:sns:${aws.region}:${aws.account}:sns-topic-name",
      "Condition": {
        "ArnLike": {
          "aws:SourceArn": "arn:aws:s3:*:*:s3-bucket-name"
        }
      }
    }
  ]
}
```
