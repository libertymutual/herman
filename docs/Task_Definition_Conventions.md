# Task Definition Conventions

##### Top Level Parameters:

-   cluster (required)  
    -   The compute to provision to
-   appName (required)  
    -   Used to name the various resources when created

  

##### Service Block:

This block is optional - if left out, app will run in batch mode (start
=&gt; run =&gt; stop)

-   instanceCount (required)  
    -   Number to keep running
-   urlPrefixOverride (optional)
    -   starting portion of your url, if a url is desired.  if not
        supplied, defaults to your appName
-   urlSuffix (optional)
    -   domain for your URL. (eg, np-lmb.lmig.com)
    -   Internal URLs can leverage existing SSL certs; external URLs
        will need extra handling
    -   If not supplied, app will not get a url/route (daemon/backround
        process only)
-   healthCheck (optional)  
    -   required if URL specified
    -   path for app health check after root context
    -   &lt;todo : TCP example&gt;

  

##### Container Definitions:

The section of the configuration for "containerDefinitions" follows AWS
format convention: 
<http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters.html>

  

### Example Definition - Web

  

The "service" block is defined with URL parameters, so the URL/Route is
created, and the platform keeps the app running.   "urlPrefixOverride"
is option - if not supplied, the url will be the appname.

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
  image: <Account ID>.dkr.ecr.us-east-1.amazonaws.com/${bamboo.maven.artifactId}:${bamboo.maven.version}
```

### Example Definition - Daemon/Background Process

The "service" block is still defined, but there are no URL parameters,
so the URL/Route doesn't get created, but the platform keeps the app
running (1 in this case):

``` java
cluster: "${ecs.cluster}"
appName: "${bamboo.maven.artifactId}-${bamboo.deploy.environment}"
service:
  instanceCount: 1
containerDefinitions:
- memory: 512
  environment:
  - name: spring.profiles.active
    value: "${bamboo.deploy.environment}"
  - name: DEPLOY_ENVIRONMENT
    value: "${bamboo.deploy.environment}"
  image: <Account ID>.dkr.ecr.us-east-1.amazonaws.com/${bamboo.maven.artifactId}:${bamboo.maven.version}
```

### Example Definition - Batch Task

The "service" block is not defined, so the container will run until
completion and then terminate, returning it's success or error code.

``` java
cluster: "${ecs.cluster}"
appName: "${bamboo.maven.artifactId}-${bamboo.deploy.environment}"
containerDefinitions:
- memory: 1800
  environment:
  - name: spring.profiles.active
    value: "${bamboo.deploy.environment},query"
  - name: DEPLOY_ENVIRONMENT
    value: "${bamboo.deploy.environment}"
  - name: VERSION
    value: "${bamboo.maven.version}"
  - name: db.instance
    value: "${db.instance}"
  image: <Account ID>.dkr.ecr.us-east-1.amazonaws.com/${bamboo.maven.artifactId}:${bamboo.maven.version}
```

  

  
