# ECS Cluster Deployment

While ECS clusters can be created in a few different ways, Herman provides some functionality to make creating and updating cluster infrastructure easier.

Cloudformation Templates are the traditional AWS tool that would be used for repeatable infrastructure deployments, so Herman leverages this across 2 templates: a shared stack and an instance stack.

## Shared Stack
The shared stack contains all of the components that are "shared" among container instances and don't change often. This could include networking components, security groups, IAM roles, etc.

## Instance Stack
The instance stack sets up an autoscaling group and launch configuration for container instances that will make-up your cluster. This stack can change frequently as new capacity is added or removed, or instance updates are applied (such as new AMIs).

## Template Syntax
Since most of the configuration lives in the cloudformation templates, Herman uses a very straightforward template syntax for clusters.

```yaml
clusterName: my-herman-cluster
sharedCftName: shared.yml
instanceCftName: instance.yml
drainingEnabled: true
maxConcurrentDraining: 3
instanceRegistrationTimeout: 3
``` 

`clusterName`: This is the name of the ECS cluster that will be created or updated

`sharedCftName`: This is the name of the Cloudformation Template which represents the "shared" stack

`instanceCftName`: This is the name of the Cloudformation Template which represents the "instance" stack

`drainingEnabled`: This flag will set container instances to drain traffic gracefully to new instances on update, without causing service downtime

`maxConcurrentDraining`: This will set the number of container instances to drain at once. Default: 3

`instanceRegistrationTimeout`: Number of minutes to wait for new EC2 instances to register to the ECS cluster before considering the instance unhealthy

## Update Process

When a new cluster is created, Herman will first create the cluster, then create the shared and instance stacks. 

When a cluster is updated, Herman will check to see if there are any shared stack updates to apply, and apply them as an update. Then, it will create a NEW instance 
stack with a new autoscaling group and set of EC2 instances. After all instances have come online and registered with the cluster successfully, Herman will start to 
drain containers from the old instances over to new instances in batches of `maxConcurrentDraining`. During this time, new deployments will not schedule 
containers to run on instances that are about to be drained. If draining fails or instances fail to come up healthy, the process will be reversed and reverted.

Once all containers have moved over to the new infrastructure, the previous instance stack will be deleted, removing the old autoscaling group and EC2 instances.