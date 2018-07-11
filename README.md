# Herman: AWS ECS Runtime and Deployment (Docker-on-AWS)
[Join us on Slack!](https://join.slack.com/t/herman-dev/shared_invite/enQtMzU0ODIyNzkxOTQxLWU1NjExOTdkY2I2ZmYyYzQxNGI4OTI0OTU0ZTBkNWY2OWQyNzNiZDFkZTAyMTAyNjcxZDk4NWRjODdjZGNjYTQ)

[![Build Status](https://travis-ci.org/libertymutual/herman.svg?branch=master)](https://travis-ci.org/libertymutual/herman)

## Background

Herman was created due to a need to deploy Docker containers to ECS for
a team within Liberty Mutual group. Herman is a Bamboo plugin that provides a
standard way for teams to deploy containers to ECS and provision AWS
resources (such as RDS instances, S3 buckets, etc) using a deployment
task in Bamboo. Herman will read a deployment configuration YML file and
handle making calls to AWS APIs to create or modify resources
as needed. For example:

``` java
cluster: ${ecs.cluster}
appName: ${bamboo.maven.artifactId}-${bamboo.deploy.environment}
service:
  instanceCount: ${instance.count}
  urlPrefixOverride: ${bamboo.deploy.environment}-${aws.region}-${bamboo.maven.artifactId}
  urlSuffix: ${url.suffix}
  healthCheck:
    target: "/health"
containerDefinitions:
- memory: 512
  portMappings:
  - hostPort: 0
    containerPort: 8443
  environment:
  - name: spring.profiles.active
    value: ${bamboo.deploy.environment}
  image: 892823.dkr.ecr.us-east-1.amazonaws.com/${bamboo.maven.artifactId}:${bamboo.maven.version}
```

## Setup

#### Plugin Setup

Teams must add organization-specific configuration files to an S3 bucket before using Herman in each unique AWS account.
[See: Plugin configuration](docs/Plugin_Configuration.md)
    
#### Broker Setup

There are four broker services that need to be deployed before Herman is operational:
-  DNS Broker: [See: DNS Broker Setup](docs/brokers/DNS_Broker_Setup.md)
-  New Relic Broker: [See: New Relic Broker Setup](docs/brokers/NR_Broker_Setup.md)
-  RDS Credential Broker: [See: RDS Credential Broker Setup](docs/brokers/RDS_Cred_Broker_Setup.md)
-  CFT Push Variable Broker: [See: CFT Push Variable Broker Setup](docs/brokers/CFT_Push_Variable_Broker_Setup.md)

## Supported Workload Types

ECS supports three basic styles of container workloads:

-   web (keep my app running, and give it a URL)
-   daemon (keep my app running, but no url required)
-   batch (run this task, shut down when done)

[See: Task Definition Conventions](docs/Task_Definition_Conventions.md)

## Application Identity (AWS "IAM")

As part of deployment, your application will get provisioned an identity
to be used when accessing other AWS resources such as RDS, SQS, and S3. 
By convention, this IAM Role will match the appName field in your
deployment manifest YML/JSON.

Permissions are limited to start with to follow the "least privilege"
model.  More details found on the IAM page, as well as specific examples
for brokered services on their pages.

[see: IAM Roles](docs/IAM_Roles.md)

## Operational Services

-   Application logs are centrally collected into Splunk per region - no
    application changes are required
-   NewRelic is available for performance monitoring.   Some app config
    required, but labels and naming is injected automatically.

## Provisioned Resources ("Brokered Services")

Commonly used application resources are able to be provisioned via your
deployment YML. For more information, see the page on [brokered
services](docs/Brokered_Services_-_ECS_Sidecars.md).

## Standalone Brokered Services
Some AWS services can be used outside of the ECS context, and Herman
provides a way to leverage its brokering capabilities without requiring
an ECS deployment. These brokers include:

-   [S3 Buckets](docs/standaloneBrokeredServices/S3_Websites.md) - Primarily used for static web content
-   [Lambda Functions](docs/standaloneBrokeredServices/Lambda_Functions.md)

## New Relic Monitoring and Alerting

New Relic is commonly used for application monitoring and alerting.
Herman will inform New Relic of a new deployment and your application
will be able to send metrics assuming the New Relic agent is configured
in your app code. To go along with this, Herman will register alert
conditions and notification channels for your application as well. For
more information, see [New Relic](docs/New_Relic.md).

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md)

## Credits
Herman was created through the hard work of a small, dynamic community within Liberty Mutual. Original authors:
-   [Mike Dodge](https://github.com/dodgemich)
-   [Blake Janelle](https://github.com/binaryblake)
-   [Ryan Batchelder](https://github.com/c1phr)
-   [Daniel Fritz](https://github.com/fritzdj)
-   [Stephen Humer](https://github.com/stevehumer)
-   [Chris Doherty](https://github.com/CWDoherty)
-   [Evan Kellogg](https://github.com/evankellogg)
-   [John Lazos](https://github.com/jelazos7)
-   [Trevor Creed](https://github.com/tcreeds)
-   Calum McElhone
-   Michael Van Berkel
-   Andrew Barter
-   Alex Schwartz
-   Nathan Ridlon
-   Michael Masscotte
-   Sabir Iqbal
