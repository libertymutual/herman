# KMS Keys

The purpose of introducing a KMS broker is to enable application secrets
to be encrypted with an application-specific KMS key. Having keys
created for each application provides important benefits:

-   Tighter security of secrets
-   Tighter audit of secret access
-   Simpler/clearer application code
-   Simplifies cluster maintenance
-   Allows app to move clusters without impacts

The application key should be used to encrypt passwords stored in source
control for a given application. It will also be used by other brokers
(i.e. RDS Broker) that need to pass encrypted passwords to the
application.

**Creating an using an application key**

1.  Add a useKms flag to the Herman template file with a value of
    true.  
    ``` java
    cluster: ${ecs.cluster}
    appName: herman-task-${bamboo.deploy.environment}
    useKms: true
    ```
    
2.  Deploy the application with the Herman change to each environment. A
    key will be created with an alias prefixed with "herman/\*" and the
    appName value from the Herman template file. The name of the key and
    tags for the key can be used to tie the application back to an ECS
    service or task.
      
3.  Encrypt application passwords using the application key.  
      
4.  Update the project to use the default container creds.\[JAVA
    example\] - The Zalando spring-cloud-config-aws dependency can be
    used to provide the funcionality to decrypt passwords set in
    application property files.

    ``` xml
    <dependency>
     <groupId>org.zalando</groupId>
     <artifactId>spring-cloud-config-aws-kms</artifactId>
     <version>1.7</version>
    </dependency>
    ```

5.  Re-deploy the application in each environment.

**Optional Elements**

Custom IAM policy: If kms-policy.json is present in the deployment
directory, the KMS Broker will attempt to use that IAM policy for the
app key. [View the default IAM policy.](../../src/main/resources/iam/kms-policy.json)

Custom Key name: It is recommended to use the default key name (app
name). However, it is possibly to use a custom key name using a
"kmsKeyName" value if that is a requirement:  
``` java
cluster: ${ecs.cluster}
appName: herman-task-${bamboo.deploy.environment}
...
useKms: true
kmsKeyName: herman-key-name
```

  

  
