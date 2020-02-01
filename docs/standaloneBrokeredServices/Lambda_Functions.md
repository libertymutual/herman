# Lambda Functions

## <span class="underline">Background</span>

[AWS Lambda](https://aws.amazon.com/lambda/) provides a way to run code
without provisioning underlying servers, containers or other
infrastructure. Billing is handled by only paying for the time that a
function spends executing, and concurrent lambdas can be run to scale as
additional concurrent invocations of your function occur. Lambda
functions can be triggered through a variety of different means,
including API requests, AWS account events, scheduled invocations and
more.

## <span class="underline">Usage</span>

This task uses potentially 3 files, a required **lambda\_template.yml**
and 2 optional files: **lambda-execution-permission.json** and
**iam-policy.json**. The template support the same parameterization
pattern as the standard ECS push task, leveraging named properties files
per environment.

**s3\_template.json**

``` json
{
    "functionName": "${function.name}",
    "zipFileName": "app.zip",
    "environment":
        [
            {"name": "env", "value": "dev"}
        ],
    "handler": "src/main/app/app.handler",
    "memorySize": 256,
    "runtime": "nodejs6.10",
    "timeout": "60",
    "tags": [
        {
            "key": "team",
            "value": "herman-lambda-team"
        }
    ],
    "scheduleExpression": "cron(0 12 * * ? *)"
}
```

**Parameters:**

| Key          | Required | Value                                                                                                                                                                                                            |
|--------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| functionName | Yes      | This will be the name of the deployed function                                                                                                                                                                   |
| zipFileName  | Yes      | Zip file that is located with the rest of the template files at deploy time. This will be uploaded to AWS with the source of your lambda.                                                                        |
| environment  | No       | Array of environment variables passed to the new lambda                                                                                                                                                          |
| handler      | Yes      | Entry point for your lambda function, see [AWS docs](https://docs.aws.amazon.com/lambda/latest/dg/API_CreateFunction.html#SSS-CreateFunction-request-Handler) for runtime-specific information.                  |
| memorySize   | No       | The amount of memory (and proportionally CPU) available to the lambda (Default is 128mb). [AWS Docs](https://docs.aws.amazon.com/lambda/latest/dg/API_CreateFunction.html#SSS-CreateFunction-request-MemorySize) |
| runtime      | Yes      | The runtime environment that will be configured for the lambda. [AWS Docs](https://docs.aws.amazon.com/lambda/latest/dg/API_CreateFunction.html#SSS-CreateFunction-request-Runtime)                              |
| timeout      | No       | Time (in seconds) that the lambda will execute before timing out. (Default is 5s)                                                                                                                                |
| vpcId      | No       | Not shown above\* - This will run the lambda within the context of the passed VPC ID.                                                           |
| useKms       | No       | This will create a KMS key for the lambda to encrypt environment variables and other secrets                                                                                                                     |
| tags       | No       | List of tags to be applied to lambda and provisioned resources
| scheduleExpression | No | Schedule Expression for cloudwatch rule trigger, see [AWS docs](https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/ScheduledEvents.html)                                                                                                                     

## <span class="underline">Permissions and Policies</span>

Just as with an ECS task, the Lambda broker will provision an IAM role
with the same name as the function which the lambda will execute as. By
default, an execution role will be provided that enables logging and
various VPC-related permissions:

**Default Lambda Execution Policy**

``` js
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSubnets",
        "ec2:DescribeVpcs",
        "ec2:DescribeNetworkInterfaces",
        "ec2:CreateNetworkInterface",
        "ec2:DeleteNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
```

To override, include a file with the name **iam-policy.json**.

In addition to the IAM role that the Lambda will execute as, permissions
can be set to determine which roles will be allowed to execute the
Lambda. By default, no permission is set, which means role in the
account can execute. To restrict this, include a file
called **lambda-execution-permission.json. **

**lambda-execution-permission.json**

``` js
[
    {
        "Sid": "<OPTIONAL>",
        "Action": "lambda:InvokeFunction",
        "Effect": "Allow",
        "Principal": "*",
        "SourceArn": "<arn>"
    }
]
```

<table>
<colgroup>
<col style="width: 8%" />
<col style="width: 8%" />
<col style="width: 83%" />
</colgroup>
<thead>
<tr class="header">
<th>Key</th>
<th>Required</th>
<th>Value</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>Action</td>
<td>Yes</td>
<td>Permission being provided to the user</td>
</tr>
<tr class="even">
<td>Effect</td>
<td>Yes</td>
<td><p>Allow or Deny</p></td>
</tr>
<tr class="odd">
<td>Principal</td>
<td>Yes</td>
<td>Principal source for AWS triggers (wildcard &quot;*&quot; if not specifying a principal)</td>
</tr>
<tr class="even">
<td>SourceArn</td>
<td>No</td>
<td>SourceArn if giving permission to another IAM Role</td>
</tr>
</tbody>
</table>

More information can be found in the [AWS
Docs](https://docs.aws.amazon.com/lambda/latest/dg/access-control-identity-based.html).

<span class="underline">  
</span>

## <span class="underline">Function Triggers</span>

The Herman broker currently supports provisioning a single scheduled cloudwatch event as a trigger. See `scheduleExpression` parameter above.