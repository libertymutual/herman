# IAM Roles

In order to provide specific access to AWS resources, applications
deployed to ECS are given a unique IAM Role be default - convention is
that the IAM Role name matches the application name in ECS as specified
by appName in the YML/JSON. "Task role" value will the the same as appName.  

This role defaults to limited/base permissions.  For the vast majority
of access that goes thru IAM (S3, SQS, SNS), no customization will be
necessary and the role ID/ARN will be used in the corresponding resource
policies.  (See: S3/SQS pages).

If the application requires unique access - either atypical actions
(s3:ListBuckets, or things like rds:CreateSnapshot), those permissions
will need to be applied to your application role.

By convention, the policy will need to be placed in a file
**iam-policy.json** that lives alongside the application deployment
manifest.   Any properties or variable substitution applies, in the same
pattern as the main manifest.

### Example 1 - Access to List Buckets:

**iam-policy.json**

``` java
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Action": [
                "s3:ListBucket"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
```

### Example 2 - Access to Snapshot RDS:

Example below shows the use of the ${—} variable pattern, with values
originating in the deployment properties.

**iam-policy.json**

``` java
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Action": [
                "rds:CreateDBSnapshot",
                "rds:DeleteDBSnapshot",
                "rds:Describe*"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:rds:us-east-1:*:db:${db.instance}"
        },
        {
            "Action": [
                "rds:CreateDBSnapshot",
                "rds:DeleteDBSnapshot",
                "rds:DescribeDBSnapshots"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:rds:us-east-1:*:snapshot:lmb-eis-${db.instance}*"
        }
    ]
}
```