package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.custom.CustomBrokerResponse.Status;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CustomBroker {
    private String name;
    private Object definition;
    private EcsPushDefinition pushDefinition;
    private EcsClusterMetadata clusterMetadata;
    private CustomBrokerConfiguration configuration;
    private AWSLambdaAsync lambdaClient;
    private HermanLogger logger;

    public CustomBroker(
        String name,
        Object definition,
        EcsPushContext pushContext,
        EcsPushDefinition pushDefinition,
        EcsClusterMetadata clusterMetadata,
        CustomBrokerConfiguration configuration,
        AWSLambdaAsync lambdaClient
    ){
        this.name = name;
        this.definition = definition;
        this.pushDefinition = pushDefinition;
        this.clusterMetadata = clusterMetadata;
        this.logger = pushContext.getLogger();
        this.configuration = configuration;
        this.lambdaClient = lambdaClient;
    }

    public void runBroker(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        ObjectNode brokerDefinition = mapper.valueToTree(configuration.getDefaults());

        if(brokerDefinition == null){
            brokerDefinition = mapper.createObjectNode();
        }

        overlay(brokerDefinition, mapper.valueToTree(definition));
        CustomBrokerPayload payload = new CustomBrokerPayload(pushDefinition, clusterMetadata, brokerDefinition);

        try{
            InvokeRequest request = new InvokeRequest()
                .withFunctionName(name)
                .withPayload(mapper.writeValueAsString(payload))
                .withLogType(LogType.Tail);

            logger.addLogEntry("**************************************************************");
            logger.addLogEntry("Custom broker - " + name);
            logger.addLogEntry(configuration.getDescription());

            if(configuration.getReadme() != null){
                logger.addLogEntry("README: " + configuration.getReadme());
            }

            logger.addLogEntry("**************************************************************");

            logger.addLogEntry("Invoking Lambda " + name);
            logger.addLogEntry("With payload: " + mapper.writeValueAsString(payload));
            Future<InvokeResult> future = lambdaClient.invokeAsync(request);
            while(!future.isDone()){
                logger.addLogEntry("Custom broker " + name + " running...");
                Thread.sleep(5000);
            }

            InvokeResult result = future.get();
            logger.addLogEntry(new String(Base64.getDecoder().decode(result.getLogResult())));

            CustomBrokerResponse response =
                mapper.readValue(result.getPayload().array(), CustomBrokerResponse.class);

            if(response.getVariablesToInject() != null){
                for(Entry<String,String> entry: response.getVariablesToInject().entrySet()) {
                    for(ContainerDefinition containerDef: pushDefinition.getContainerDefinitions()) {
                        KeyValuePair pair = new KeyValuePair().withName(entry.getKey()).withValue(entry.getValue());
                        containerDef.getEnvironment().add(pair);
                    }
                }
            }

            logger.addLogEntry("Lambda " + name + " finished");

            if(response.getStatus() == Status.SUCCESS){
                logger.addLogEntry("Lambda response: " + response.getMessage());
            }
            else {
                logger.addErrorLogEntry("Lambda error: " + response.getMessage());
            }

        } catch(IOException | InterruptedException | ExecutionException exception){
            logger.addErrorLogEntry("Custom broker failed", exception);
        }
    }

    private void overlay(ObjectNode from, JsonNode with) {
        for (Iterator<Entry<String, JsonNode>> i = with.fields(); i.hasNext();){
            Entry<String, JsonNode> field = i.next();
            if(field.getValue().isObject()){
                if(!from.has(field.getKey())){
                    from.set(field.getKey(), field.getValue());
                } else {
                    overlay((ObjectNode)from.get(field.getKey()), field.getValue());
                }
            } else {
                from.set(field.getKey(), field.getValue());
            }
        }
    }
}
