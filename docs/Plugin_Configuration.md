# Plugin Configuration

1. An S3 bucket needs to be created in each AWS account that is a target for Herman. 
The standard name for this bucket is herman-configuration-<AWS account ID>-<version>. 
Version is pulled from a property file [See: Version Properties File](/src/main/resources/version.properties). 
    * NOTE: The configuration bucket name can be overridden for the CLI option using the `--config` option.
2. Add a properties.yml file to the config bucket (see "Herman Configuration" section below).
[See: Sample Properties File](/src/main/resources/config/samples/properties.yml) for the structure of this file.
3. Add a ecr-policy.json file to the config bucket. The contents of this file is an ECR IAM policy, 
used as the default IAM policy used when creating ECR repositories.
4. Add a kms-policy.json file to the config bucket. The contents of this file is a KMS IAM policy, 
used as the default IAM policy used when creating KMS keys.

## Herman Configuration (properties.yml in the configuration S3 bucket)

| Property                    | Valid Value                                                                                                                                                                                  |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| company                     | String - Short name for a company                                                                                                                                                            |
| sbu                         | String - Short name for a business unit                                                                                                                                                      |
| org                         | String - Short name for an organization within a business unit                                                                                                                               |
| ecsConsoleLinkPattern       | String - Format string passed into String.format(...) to create a URL to the AWS console. There must be four variables set using the %s specifiers: AWS account, Region, Cluster, and Family |   
| rdsCredentialBrokerImage    | String - ID for the RDS credential broker image in ECR                                                                                                                                       | 
| cftPushVariableBrokerLambda | String - Name of the CFT Push Variable Broker Lambda                                                                                                                                         |
| dnsBrokerLambda:            | String - Name of the DNS Broker Lambda                                                                                                                                                       |
| sslCertificates             | See "SSL Certificate Properties" below                                                                                                                                                       |  
| splunkInstances             | See "Splunk Properties" below                                                                                                                                                                |
| newRelic                    | See "New Relic Properties" below                                                                                                                                                             |

### SSL Certificate Properties

***"sslCertificates"*** is a list of objects that represent SSL
certificates. There are four required property files for each
certificate:

| SSL Cert Property | Valid Value                                                                                                                                |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| urlPrefix         | String - "\*" or a specific URL prefix. For example, given the test-app.np-lmb.lmig.com URL, the prefix could either be "\*" OR "test-app" |
| urlSuffix         | String - Suffix for the URL. For example, given the test-app.np-lmb.lmig.com URL, the suffix would be "np-lmb.lmig.com"                    |
| pathSuffix        | String - Path for the SSL cert in IAM                                                                                                      |
| internetFacingUrl | true if the URL is externally-facing                                                                                                       |

### Splunk Properties

***"splunkInstances"*** is a list of objects that represent Splunk
instances:

| Splunk instance Property | Valid Value                                                                                                   |
|--------------------------|---------------------------------------------------------------------------------------------------------------|
| httpEventCollectorUrl    | String - URL for the Splunk event collector for the Splunk instance. This is set for the ECS cluster.         |
| webUrl                   | String - Web UI URL for the instance. This is used to print out a link to the logs for a specific deployment. |

### New Relic Properties

| New Relic Property             | Valid Value                                                                                             |
|--------------------------------|---------------------------------------------------------------------------------------------------------|
| newRelic.accountId             | Integer - AWS account ID                                                                                |
| newRelic.nrLambda              | String - NR Broker lambda name                                                                          |

