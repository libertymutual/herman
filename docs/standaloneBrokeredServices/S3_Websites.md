# S3 Websites

## <span class="underline">Background</span>

There are two methods to brokering S3 resources. The first involves
creating a bucket as part of an ECS deployment (see the header "Example
for S3"
[here](https://forge.lmig.com/wiki/pages/viewpage.action?pageId=78055744#s3Example)).
The second involves using a standalone task provided by Herman which
supports a similar interface for configuring a bucket through code,
without pushing a Docker container to ECS. This can be a good pattern
for for static UI deployments, from a simplicity and cost perspective.
The task for these types of deployments is: **Herman - S3 Bucket
Create.**

## <span class="underline">Usage</span>

This task uses two files: **s3\_template.json** and an optional policy
document. The idea behind s3\_template.json is similar to the
template.json file used for ECS deployments, but trimmed down to what's
needed for S3 provisioning. The template supports the same
parameterization pattern as the standard ECS push task leveraging named
properties files per environment.

**s3\_template.json**

``` js
{
    "appName": "${app.name}-${app.environment}",
    "sbu": "CI",
    "org": "LMB",
    "policyName": "s3_test_policy.json",
    "website": true,
    "indexFile": "index.html",
    "errorFile": "error.html",
    "tags": [
        {
            "key": "team",
            "value": "herman-s3-team"
        }
    ]
}
```

  

**Parameters:**

| Key        | Required | Value                                                                                                                                                                  |
|------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| appName    | Yes      | This will be the name of the S3 bucket. Because of that, it must be globally unique across all buckets.                                                                |
| sbu        | No       | Will populate required "lm\_sbu" tag, see [ETS Tagging Standards](https://forge.lmig.com/wiki/display/ETSPC/AWS+Resource+Tagging+Standards). Defaults to "CI"          |
| org        | No       | Will populate optional "lm\_org" tag, see [ETS Tagging Standards](https://forge.lmig.com/wiki/display/ETSPC/AWS+Resource+Tagging+Standards). Defaults to "LMB"         |
| policyName | No       | Reference to an S3 Policy Document file                                                                                                                                |
| website    | No       | Will configure S3 bucket as a website, defaults to false                                                                                                               |
| indexFile  | No       | Configure the index file if website hosting is enabled, defaults to index.html                                                                                         |
| errorFile  | No       | Configure the error file if website hosting is enabled ([AWS Docs](http://docs.aws.amazon.com/AmazonS3/latest/dev/CustomErrorDocSupport.html)), defaults to error.html |
| tags       | No       | List of tags to be applied to lambda and provisioned resources                                                                                                         |
| snsNotifications | No | List of SNS Event Notifications for the S3 Bucket |
| lambdaNotifications | No | List of Lambda Event Notifications for the S3 Bucket |

  

After deploy, the Bamboo variable **$bamboo\_s3\_brokered\_name** will
be available to use for uploading files.

## <span class="underline">Helpful Info for Website Hosting</span>

Website hosting is relatively simple through S3, but does require a
specific policy document to be applied to allow access to files that are
put into the bucket. The following is an example of a policy that will
give access from an internal network and resources in Non-Prod and Prod VPCs. 
The VPC bit is important if you're using a router (Zuul or Nginx) in front of the 
bucket in an API Gateway pattern.


**s3\_website\_policy.json**

``` js
{
    "Version": "2012-10-17",
    "Id": "S3AccessPolicy",
    "Statement": [
        {
            "Sid": "IPAllow",
            "Effect": "Allow",
            "Principal": "*",
            "Action": [
                "s3:Get*",
                "s3:List*",
                "s3:Put*",
                "s3:DeleteObject*"
            ],
            "Resource": "arn:aws:s3:::${app.name}-${app.environment}/*",
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": [
                        <IP List>
                    ]
                }
            }
        },
        {
            "Sid": "VpcAllow",
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": [
                "s3:Get*",
                "s3:List*",
                "s3:Put*",
                "s3:DeleteObject*"
            ],
            "Resource": "arn:aws:s3:::${app.name}-${app.environment}/*",
            "Condition": {
                "StringEquals": {
                        "aws:sourceVpc": [<VPC List>]
                    }
            }
        }
    ]
}
```
