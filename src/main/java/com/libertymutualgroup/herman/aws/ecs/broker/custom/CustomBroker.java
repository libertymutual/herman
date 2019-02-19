package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libertymutualgroup.herman.aws.ecs.CustomBrokerDefinition;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CustomBroker {
    private CustomBrokerDefinition definition;
    private EcsPushContext pushContext;
    private EcsPushDefinition pushDefinition;
    private CustomBrokerConfiguration configuration;
    private AWSLambdaAsync lambdaClient;
    private HermanLogger logger;

    public CustomBroker(
        CustomBrokerDefinition definition,
        EcsPushContext pushContext,
        EcsPushDefinition pushDefinition,
        CustomBrokerConfiguration configuration,
        AWSLambdaAsync lambdaClient
    ){
        this.definition = definition;
        this.pushContext = pushContext;
        this.pushDefinition = pushDefinition;
        this.logger = pushContext.getLogger();
        this.configuration = configuration;
        this.lambdaClient = lambdaClient;
    }

    public void runBroker(){
        Properties props = pushContext.getPropertyHandler()
            .lookupProperties(configuration.getVariablesToPass().keySet().toArray(new String[configuration.getVariablesToPass().size()]));

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        ObjectNode environment = mapper.createObjectNode();
        ObjectNode brokerDefinition = mapper.valueToTree(configuration.getDefaults());

        if(props != null){
            for(Entry<Object, Object> entry : props.entrySet()){
                environment.set(configuration.getVariablesToPass().get(entry.getKey().toString()), mapper.valueToTree(entry.getValue()));
            }
        }

        overlay(brokerDefinition, mapper.valueToTree(definition.getProperties()));

        CustomBrokerPayload payload = new CustomBrokerPayload(pushDefinition, brokerDefinition, environment);

        try{
            InvokeRequest request = new InvokeRequest()
                .withFunctionName(definition.getName())
                .withPayload(mapper.writeValueAsString(payload));

            logger.addLogEntry("Invoking Lambda " + definition.getName());
            logger.addLogEntry("With payload: " + mapper.writeValueAsString(payload));
            Future<InvokeResult> future = lambdaClient.invokeAsync(request);
            while(!future.isDone()){
                logger.addLogEntry("Custom broker Lambda running...");
                Thread.sleep(2000);
            }

            InvokeResult result = future.get();
            logger.addLogEntry(result.getLogResult());

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

            logger.addLogEntry("Lambda " + definition.getName() + " finished");
            logger.addLogEntry("Lambda response: " + response.getMessage());

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
