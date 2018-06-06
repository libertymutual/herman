# New Relic Broker

## Background

The New Relic Broker is a Lambda function that has two important pieces of functionality:
1. [Adding application deployments](../newRelic/New_Relic_Application_Deployments.md)
2. [Configuring alerts for an application](../newRelic/New_Relic_Alert_Configuration.md) - See here for how to configure application alerting.

The New Relic Broker is invoked by Herman after a service is deployed during each ECS Push Task run. A [NewRelicBrokerRequest](../../src/main/java/com/libertymutualgroup/herman/aws/ecs/broker/newrelic/NewRelicBrokerRequest.java)
 object is passed to the broker and a [NewRelicBrokerResponse](../../src/main/java/com/libertymutualgroup/herman/aws/ecs/broker/newrelic/NewRelicBrokerResponse.java) object is returned.

## Setup

The code for the Herman New Relic Broker is available on GitHub: [New Relic Broker](https://github.com/libertymutual/herman-newrelic-broker)

The [Herman Lambda Push Task](../standaloneBrokeredServices/Lambda_Functions.md) can be used to deploy the broker itself. 
An org-specific New Relic account ID and the Lambda name needs to be updated in [plugin-tasks.yml](../../src/main/resources/config/plugin-tasks.yml) before Herman is functional 
(See [Plugin configuration](/Users/n0201186/IdeaProjects/aws-ecs-tasks-plugin/docs/Plugin_Configuration.md)).
