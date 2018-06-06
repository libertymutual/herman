# CFT Push Variable Broker

## Background

The CFT Push Variable Broker is a Lambda function that handles retrieving and returning organization-specific variable keys and values to be used to set CFT parameters.

## Contract
| Input/Output  | Type                | Notes                                                                                                              |
|---------------|---------------------|--------------------------------------------------------------------------------------------------------------------|
| Input         | String              | AWS region - See: [CloudFormation Regions](https://docs.aws.amazon.com/general/latest/gr/rande.html#cfn_region)    |
| Output        | Map<String, String> | Map of variable key to variable value                                                                              |

## Setup

The code for this broker is not available on GitHub because it is organization-specific. It must be implemented by each organization. 
Liberty Mutual used an interface provided by the AWS Lambda Java core library ([aws-lambda-java-core](https://docs.aws.amazon.com/lambda/latest/dg/java-handler-using-predefined-interfaces.html)) 
to create a Lambda function handler for the CFT Push Variable Broker.