# RDS Databases

## The Problem

In the past, database provisioning has been a lengthy and confusing
process that involved multiple handoffs and manual steps. [AWS
Relational Database Service](https://aws.amazon.com/rds/) (RDS)
automates much of the infrastructure provisioning out of the box. 

However, RDS is designed as Infrastructure as a Service (IaaS) and is
not a fully integrated database solution. To provision "native" RDS, you
need to consider:

-   Credential management - who creates and manages database
    credentials? How does the app access passwords securely at runtime?
-   Least privilege access - separate concerns for schema management
    tasks vs. runtime needs
-   IaaS level detail - ugly CloudFormation templates requiring teams to
    provide low-level details including security groups, subnets, VPCs,
    etc

## Quick Start

Below is the bare minimum amount of YAML necessary to provision an RDS
instance using Herman. Just add this to your existing **template.yml**
or **template.json** file. When your app gets deployed, Herman will
search for an existing RDS instance using your **appName** as the
instance ID. If no RDS instance exists, it will create one using the
details specified.

<span class="underline">**PostgreSQL**</span>

**template.yml**

``` xml
cluster: ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
database:
  engine: postgres
  engineVersion: 9.6.6
  dbInstancePort: 3306
```

PostgreSQL engine versions can be found on this
page: <http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html>

  

<span class="underline">**MySQL**</span>

**template.yml**

``` xml
cluster: ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
database:
  engine: mysql
  engineVersion: 5.7.19
  dbInstancePort: 3306
```

MySQL engine versions can be found on this
page: <http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MySQL.html>

See "optional properties" below for more detailed usage.

<span class="underline">**Aurora MySQL**</span>

**template.yml**

``` xml
cluster: ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
database:
  engine: aurora-mysql
  engineVersion: 5.7.12
  dbInstancePort: 3306
  availabilityZones: 
    - us-east-1a
    - us-east-1c
```

Aurora is an AWS database engine that offers cloud-native improvements
to MySQL including greater performance and high-availability. By
default, Aurora instances are created in clusters, with one master and
some number of read-replicas that can be promoted in the event of a
master failure. The broker will place the master in the first listed AZ,
and replicas in each subsequent AZ. The injected connection string will
reference the cluster, and Aurora will handle repointing the URL in the
event of a master failure.

Use of the **fullUpdate** flag between Aurora and non-Aurora instance
types works a little differently as well. Because of how instances are
named in a cluster, the Aurora cluster will remain unchanged if
migrating away from Aurora, and a non-Aurora instance should remain
unchanged if updating into Aurora. **You'll need to handle data
migration and instance cleanup manually.**

The minimum instance size with Aurora and IAM authentication is
db.t2.medium.

Recommended content for further Aurora information: [Deep Dive on the
Amazon Aurora MySQL-compatible Edition
(YouTube)](https://www.youtube.com/watch?v=rPmKo2g9znA)

See "optional properties" below for more detailed usage.

## Supported RDBMS types

As of now, support for RDS brokering is limited to PostgreSQL and MySQL.
We plan to add Oracle support in the near future.

## App Integration

Once your database is up and running, Herman will automatically inject
connection information and two sets of credentials as environment
variables for your app. By default, it will set:

-   rds\_host
-   rds\_port
-   rds\_db
-   rds\_connection\_string
-   rds\_app\_username
-   rds\_app\_encrypted\_password
-   rds\_admin\_username
-   rds\_admin\_encrypted\_password

The "app" username is granted **select, insert, update, and delete**
permissions for all tables in the default schema. It is meant to be used
by the app at runtime.

The "admin" username is granted the same permission as "app", along
with **create** and **usage** permissions on the default schema,
allowing it to be used for schema management. It is designed to support
database refactoring via tools like
[Liquibase](http://www.liquibase.org/). 

Passwords for both accounts will be randomly generated on each
deployment and encrypted using the KMS key on the ECS cluster that the
app is deployed to. The reference app below provides sample code for
accessing the KMS key and decrypting the passwords at runtime.

## Optional Properties

### injectNames

Provides control over the environment variables that get injected into
the app runtime.

``` java
  database:
    engine: postgres
    engineVersion: 9.6.3
    dbInstancePort: 3306
    injectNames:
      connectionString: 'spring.datasource.url'
      appUsername: 'spring.datasource.username'
      appEncryptedPassword: 'spring.datasource.password'
      adminUsername: 'liquibase.user'
      adminEncryptedPassword: 'liquibase.password'
```

**Note**: if you use *spring.datasource.password* or
*liquibase.password *for any password field, the broker will
automatically prefix the value with *{cipher}* in accordance with the
KMS Zalando decryption library.

### appUsername **and** adminUsername

If your app needs static usernames, you can specify them as part of your
database definition. If you don't specify them, you will get a new user
created on each deploy (rolling credentials).

``` java
database:
  engine: postgres
  engineVersion: 9.6.3
  dbInstancePort: 3306
  appUsername: my-app-username
  adminUsername: my-admin-username
```

### appEncryptedPassword and adminEncryptedPassword

If your app needs static passwords, you can specify them in the database
definition. The password must be encrypted using the KMS key of the
cluster that the app is being deployed to.

``` java
database:
  engine: mysql
  engineVersion: 5.7.17
  dbInstancePort: 3306
  adminUsername: myAdminUser
  adminEncryptedPassword: AQECAHh9JTcKDCR7Yt+9HH1NbYlw/fOfvRFwmO6YYPeak7oKNAAAAGgwZgYJKoZIhvcNAQcGoFkwVwIBADBSBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDAWKKQ1G3C8X/D/RjwIBEIAlsmCVuu8U2aQW6Q/KBcjICFXVXPPK8ivB2UOeph3K+6Q1znot2g==
```

### dbinstanceIdentifier

This one is particularly useful if you have an existing non-brokered RDS
database. It can also be used to share database instances between apps.
Sharing databases between apps is only recommended for
production-support DBA-type activities. 

``` java
database:
  dbinstanceIdentifier: my-rds-instance
  engine: postgres
  engineVersion: 9.6.3
  dbInstancePort: 3306
```

### dbname

Some applications we need to deploy expect a specific database name to
boot, such as the Atlassian tools. Specifying the dbname allows us to
create the name the tool expects. 

``` java
database:
  engine: postgres
  engineVersion: 9.6.3
  dbInstancePort: 3306
  dbname: my-db-name
```

NOTE: Based on the documents it may be assumed that dBName would be the
value, but it must be in all lowercase to work.

### Option Group

``` java
database:
  engine: postgres
  engineVersion: 9.6.3
  dbInstancePort: 3306
  optionGroupFile: rds/option-group.json
```

Create the JSON file using the path above. This file needs to contain a
majorEngineVersion value and one to many options. i.e.:

``` java
{
  "majorEngineVersion": "12.1",
  "options": [
    {
      "optionName": "SQLT",
      "optionSettings": [
        {
          "name": "LICENSE_PACK",
          "value": "T"
        }
      ]
    }
  ]
}
```

### Parameter Group

``` java
database:
  engine: postgres
  engineVersion: 9.6.3
  dbInstancePort: 3306
  parameterGroupFile: rds/parameter-group-parameters.json
```

Create a JSON file using the path above. This file needs to contain one
to many param groups. I.e.:

``` java
[
  {
    "parameterName":"optimizer_adaptive_features",
    "parameterValue":"FALSE",
    "applyMethod":"immediate"
  }
]
```

### RDS native properties

You can specify just about anything that you would put into a
CloudFormation template, with a few exceptions. See the [full list of
available RDS
properties ](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/rds/model/DBInstance.html)

## Upgrading Instances

By default, instances created with the RDS broker will automatically
apply minor version upgrades.

To upgrade a major version (e.g. Postgres 9.5.4 → Postgres 9.6.6) you
must change the engineVersion in the template and also specify the
fullUpdate flag. The upgrade will add an additional ~10 minutes to the
deploy.

``` java
database:
    engine: postgres
    engineVersion: 9.6.6
    dbInstancePort: 3306
    fullUpdate: true
```

  

## FAQ

-   **Do brokered RDS instances support encryption at rest? **  
    -   Yes, all storage is encrypted at rest using KMS keys. This is
        one of the few settings that cannot be overridden in the
        template.yml database block.
-   **Where is the clear text password stored and who has access to
    it?**
    -   Nowhere and no one. The passwords are generated and encrypted on
        each deploy.
-   **Can I use brokered RDS for local development?**
    -   No - we recommended that you don't use RDS for local
        development. Instead, you should install and run your database
        of choice on your local machine.
-   **Is there any downtime during deployment?**
    -   It depends. If you're "rolling" credentials on each deploy
        (default behavior), there will be zero downtime. You may see
        downtime if you choose to modify core properties of the RDS
        instance like instance type, allocated storage, and database
        engine. AWS has all of this documented.
-   **Can I connect to the brokered RDS instance from my local
    machine?**
    -   No. Database changes should be managed in code and run through
        tools like Liquibase or Flyway. If you need to run one-off
        queries or have other production support needs, you can create a
        separate app and pipeline for it.
-   **Can I bring in an existing RDS instance? Can I share a database
    between apps?**
    -   Yes, using the **dbinstanceIdentifier** property described
        above.
