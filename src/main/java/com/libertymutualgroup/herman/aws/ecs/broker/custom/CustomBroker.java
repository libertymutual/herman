package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libertymutualgroup.herman.aws.ecs.CustomBrokerDefinition;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

public class CustomBroker {
    CustomBrokerDefinition definition;
    private EcsPushContext pushContext;
    private EcsPushDefinition pushDefinition;
    private CustomBrokerConfiguration configuration;
    private AWSLambda lambdaClient;
    private HermanLogger logger;

    public CustomBroker(
        CustomBrokerDefinition definition,
        EcsPushContext pushContext,
        EcsPushDefinition pushDefinition,
        CustomBrokerConfiguration configuration,
        AWSLambda lambdaClient
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

        try {
            InvokeRequest request = new InvokeRequest()
                .withFunctionName(definition.getName())
                .withPayload(mapper.writeValueAsString(payload));

            logger.addLogEntry("Invoking Lambda " + definition.getName());
            logger.addLogEntry("With payload: " + mapper.writeValueAsString(payload));
            lambdaClient.invoke(request);
            logger.addLogEntry("Lambda " + definition.getName() + " finished");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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
