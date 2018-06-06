# RDS Credential Broker

## Background

The RDS Credential Broker is an ECS task that runs to reset the IDs and/or PWs for an RDS 
instance when an application is being deployed using the ECS Push Task.

Environment variable values are passed into the broker task during runtime:
* spring.profiles.active: This is the RDS engine type. The driver used depends on the profile.
* DB_HOST: Instance host URL
* DB_PORT: Instance port number
* DB_NAME: Database name
* DB_USERNAME: Master username
* DB_PASSWORD: Master password (encrypted)
* DB_APP_USERNAME: App username
* DB_APP_PASSWORD: App password (encrypted)
* DB_ADMIN_USERNAME: Admin username
* DB_ADMIN_PASSWORD: Admin password (encrypted)

See: [credential-broker.yml](../../src/main/resources/brokerTemplates/rds/credential-broker.yml), used to run the RDSCredential broker as an ECS task using the Herman ECS Push task.

## Setup

The Herman RDS Credential Broker code is available on GitHub: [Herman RDS Credential Broker](https://github.com/libertymutual/herman-rds-credential-broker)

The broker code must be built and the Docker image for the application needs to be pushed to ECR before Herman is fully operational.
The "rdsCredentialBrokerImage" value in [plugin-tasks.yml](../../src/main/resources/config/plugin-tasks.yml) 
must be updated to include the image location in ECR.