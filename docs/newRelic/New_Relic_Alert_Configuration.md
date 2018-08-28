# New Relic Alert Configuration

## The Problem

New Relic has extensive [alerting
capabilities](https://docs.newrelic.com/docs/alerts/new-relic-alerts)
that most teams don't take advantage of. Configuring alerts in the UI is
cumbersome and limited to administrator privileges. Defining alert
configuration is a good fit to live alongside application code, which
the [New Relic API ](https://rpm.newrelic.com/api/explore)supports, but
some teams don't have the bandwidth to write custom code to configure
their apps.

## Quick Start

New Relic configuration is composed of two aspects that are required -
conditions and channels. They should be defined as separate files that
live in the same directory as your template file.

-   **Channels** are how you want to be notified when an alert is
    triggered

Optional aspects

-   **Conditions** are standard APM alert conditions. These are
    typically used for applications running in ECS.
-   **NRQL conditons** are needed if custom NRQL statements need to be
    created. If you are using [Amazon
    Integrations](https://docs.newrelic.com/docs/integrations/amazon-integrations),
    NRSQL is a great option to alert on service metrics.
-   **DB name** is needed if the name of the DB is different than the
    application name. If the [RDS Broker](RDS_Databases) is being used,
    this value is not needed.
-   **Apdex** is a standard to measure users' satisfaction with the
    response time of web applications and services. It's a simplified
    Service Level Agreement (SLA) solution that gives application owners
    better insight into how satisfied users are, in contrast to
    traditional metrics like average response time, which can be skewed
    by a few very long responses. The default threshold for apdex .5 for
    each application.

There are two options for configuring alerts:

1.  Use the ECS Push task and add a newRelic block to the template.yml
    or template.json for the service:

    **template.yml**

    ``` xml
    cluster: ${ecs.cluster}
    appName: ${bamboo.maven.artifactId}-${bamboo.deploy.environment}
    ...
    newRelic:
      conditions: conditions.json
      channels: channels.json
      nrqlConditions: nrqlConditions.json
      dbName: '${db.name}'
      apdex: '.75'
    ```

    NOTE: In this case you would have the same configuration in all
    deployed environments (with the exception of dbName). Alternatively,
    you could use placeholders if you wanted to define channels or
    conditions specific to each environment. For example,
    conditions-${bamboo.deploy.environment}.json could be used if there
    are conditions-nonprod.json and conditions-prod.json files for
    nonprod and prod environments.

2.  Use the New Relic Broker Push Task. This is a standalone Bamboo task
    to configure alerting for an app. Option 1 should be used unless
    there is a specific reason why this should be used. The file name
    should be newrelic-template.yml or newrelic-template.json in this
    case. policyName is used instead of appName becaue this option is
    typically used for alerting on resources that are not ECS services.
    For example:

    **newrelic-template.yml**

    ``` xml
    policyName: ${bamboo.maven.artifactId}-${bamboo.deploy.environment}
    newRelic:
      channels: channels.json
      nrqlConditions: nrqlConditions.json
    ```

## Identifying Alert Conditions

A pattern to identify alerts is to set up the alert condition in Dev for
a policy, get the alert conditions payload for the policy using the New
Relic API, then add that alert condition to the application's Herman New
Relic config file. Once the app is deployed to each environment, the
alert policy in New Relic will get updated with that alert condition
using Herman. When creating a new alert policy, you can see the list of
alerts you can alert on. For example:


After creating the alert condition, you can use the [New Relic API
Explorer](https://rpm.newrelic.com/api/explore) to get the alert
condition in JSON format. This is what is needed in the Herman
configuration.

-   [Alerts Conditions &gt;
    List](https://rpm.newrelic.com/api/explore/alerts_conditions/list)
    page can be used to get standard alert conditions. See the
    "Conditions" section below for more information.
-   [Alerts NRQL Conditions &gt;
    List](https://rpm.newrelic.com/api/explore/alerts_nrql_conditions/list)
    page can be used to get NRQL alert conditions. See the "NRQL
    Conditons" section below for more information.

## Files Contents

### Conditions

Conditions should be a JSON array. Each object in the array should match
the payload required by the [New Relic Conditions
API](https://docs.newrelic.com/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/alerts-conditions-api-field-names).
For example

``` js
[
  {
    "type": "apm_app_metric",
    "name": "Apdex (Low)",
    "metric": "apdex",
    "enabled": true,
    "violation_close_timer": 24,
    "condition_scope": "application",
    "terms": [
      {
        "duration": "10",
        "operator": "below",
        "priority": "critical",
        "threshold": "0.6",
        "time_function": "all"
      }
    ]
  },
  {
    "type": "apm_app_metric",
    "name": "Instances Error percentage (High)",
    "metric": "error_percentage",
    "condition_scope": "instance",
    "enabled": true,
    "violation_close_timer": 24,
    "terms": [
      {
        "duration": "10",
        "operator": "above",
        "priority": "critical",
        "threshold": "5",
        "time_function": "all"
      }
    ]
  }
]
```

### NRQL Conditions

NRQL Conditions should be a JSON array Each object in the array should
match the payload required by the [New Relic NRQL Conditions
API](https://docs.newrelic.com/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/rest-api-calls-new-relic-alerts#conditions-nrql).
For example:

``` js
[
  {
    "name": "Cluster CPU Utilization",
    "enabled": false,
    "terms": [
      {
        "duration": "5",
        "operator": "above",
        "priority": "critical",
        "threshold": "80",
        "time_function": "all"
      }
    ],
    "value_function": "single_value",
    "nrql": {
      "query": "SELECT max(cpuPercent) FROM SystemSample WHERE `cluster` LIKE 'test-cluster-${bamboo.deploy.environment}%'",
      "since_value": "3"
    }
  }
]
```

### Infrastructure Conditions

Infrasturcture Conditions should be a JSON Array. Each object in the array should match
the payload required by the [New Relic Infrastructure API](https://docs.newrelic.com/docs/infrastructure/new-relic-infrastructure/infrastructure-alert-conditions/rest-api-calls-new-relic-infrastructure-alerts). 
For Example:

``` js
[
  {
    "type": "infra_metric",
    "name": "efs-alert",
    "enabled": true,
    "filter": {
      "and": [
        {
          "is": {
            "displayName": "fs-123456"
          }
        }
      ]
    },
    "select_value": "provider.lastKnownSizeInBytes",
    "comparison": "above",
    "critical_threshold": {
      "value": 20,
      "duration_minutes": 5,
      "time_function": "all"
    },
    "integration_provider": "EfsFileSystem"
  }
]
```

### Synthetics Monitors and Alerts
Synthetics should be a JSON Array. Each object in the array should match
the payload required by the [New Relic Synthetics API](https://docs.newrelic.com/docs/apis/synthetics-rest-api/monitor-examples/manage-synthetics-monitors-rest-api#create-monitor). 
For Example:

```js
[
  {
    "type": "BROWSER",
    "frequency": 10,
    "uri": "https://google.com",
    "locations": ["AWS_US_EAST_1", "AWS_US_WEST_2"],
    "status": "ENABLED",
    "slaThreshold": 1.0,
    "options": {
      "verifySSL": true
    }
  }
]
```

### Channels

Channels should be a JSON array. Each object in the array should match
the payload required by the [New Relic Channels
API](https://rpm.newrelic.com/api/explore/alerts_channels/create). For
example:

``` js
[
  {
    "name": "your-slack-channel",
    "type": "Slack",
    "configuration": {
      "url": "https://hooks.slack.com/services/your-slack-hook",
      "channel": "your-slack-channel"
    }
  },
  {
    "name": "your-team-pagerduty",
    "type": "PagerDuty",
    "configuration": {
      "service_key": "your-pagerduty-service-key"
    }
  }
]
```
