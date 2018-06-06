# SQS Queues

This broker provides [AWS SQS Queues](https://aws.amazon.com/sqs/). The
configuration key "queues" takes a list of queue definition objects
which currently supports the following fields:

Queue Definition

| Name                          | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------|--------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                          | String | YES      | Name that will be assigned to the provisioned queue                                                                                                                                                                                                                                                                                             |
| policyName                    | String | YES      | Name to a JSON file which represents the [queue policy](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html) that will be applied to the bucket. This should be in Amazon [Access Policy Language](https://docs.aws.amazon.com/AmazonS3/latest/dev/access-policy-language-overview.html). |
| delaySeconds                  | Int    | NO       | Time that messages will be delayed before delivery                                                                                                                                                                                                                                                                                              |
| maximumMessageSize            | Int    | NO       | Max size (in bytes) of a message sent in the queue. Default = 256kb                                                                                                                                                                                                                                                                             |
| messageRetentionPeriod        | Int    | NO       | Number of seconds that SQS retains a message. Default = 4 days                                                                                                                                                                                                                                                                                  |
| receiveMessageWaitTimeSeconds | Int    | NO       | Number of seconds that a ReceiveMessage action will wait for messages before returning. Default = 0 See [Long Polling Documentation](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html)                                                                                                          |
| visibilityTimeout             | Int    | NO       | Number of seconds that a message won't be available after being delivered to a recipient. Default = 30 seconds                                                                                                                                                                                                                                  |
| serverSideEncryption          | Bool   | NO       | Indicates if server side encryption should be turned on. Default = true                                                                                                                                                                                                                                                                         |
| kmsMasterKeyId                | String | NO       | KMS Key used to encrypt messages in the queue. Default = alias/aws/sqs or null if serverSideEncryption is disabled                                                                                                                                                                                                                              |
| fifoQueue                     | Bool   | NO       | Indicates if the queue is a [FIFO queue](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html). Default = false                                                                                                                                                                                           |

  

  

Example of SQS Manifest and resource policy:

App template:  
template.yml

<table>
<tbody>
<tr class="odd">
<td><pre><code>cluster:  ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
queues:
- name: herman-task-some-queue-${bamboo.deploy.environment}
  maximumMessageSize: 1024
  policyName: queue-policy.json</code></pre></td>
</tr>
</tbody>
</table>

Corresponding policy (specified by policyName attribute):  
**queue-policy.json**

<table>
<tbody>
<tr class="odd">
<td><pre><code>{
    &quot;Version&quot;: &quot;2012-10-17&quot;,
    &quot;Statement&quot;: [
        {
             &quot;Action&quot;: &quot;sqs:SendMessage&quot;,
             &quot;Principal&quot;: {&quot;AWS&quot; : &quot;arn:aws:iam::${aws.account}:role/aws-ecs/some-other-app-that-sends&quot;},
             &quot;Effect&quot;: &quot;Allow&quot;,
             &quot;Resource&quot;: &quot;arn:aws:sqs:us-east-1:${aws.account}:herman-task-some-queue-${bamboo.deploy.environment}&quot;
        },
        {
             &quot;Action&quot;: &quot;sqs:ReceiveMessage&quot;,
             &quot;Principal&quot;: {&quot;AWS&quot; : &quot;${app.iam}&quot;},
             &quot;Effect&quot;: &quot;Allow&quot;,
             &quot;Resource&quot;: &quot;arn:aws:sqs:${aws.region}:${account.id}:herman-task-some-queue-${bamboo.deploy.environment}&quot;
        }
         
    ]
}</code></pre></td>
</tr>
</tbody>
</table>
