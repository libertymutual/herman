package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.Map.Entry;
import java.util.Properties;

public class CustomBroker {
    private EcsPushContext pushContext;
    private EcsPushDefinition definition;
    private CustomBrokerConfiguration configuration;
    private AWSLambda lambdaClient;
    private HermanLogger logger;

    public CustomBroker(
        EcsPushContext pushContext,
        EcsPushDefinition definition,
        CustomBrokerConfiguration configuration,
        AWSLambda lambdaClient
    ){
        this.pushContext = pushContext;
        this.definition = definition;
        this.logger = pushContext.getLogger();
        this.configuration = configuration;
        this.lambdaClient = lambdaClient;
    }

    public void runBroker(){
        Properties props = pushContext.getPropertyHandler()
            .lookupProperties(configuration.getVariablesToPass().toArray(new String[configuration.getVariablesToPass().size()]));

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        ObjectNode payload = mapper.valueToTree(definition);

        if(props != null){
            for(Entry<Object, Object> entry : props.entrySet()){
                payload.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        if(configuration.getType() == CustomBrokerRuntime.LABMDA){
            try {
                // call lambda with pushcontext and properties
                InvokeRequest request = new InvokeRequest()
                    .withFunctionName(configuration.getName())
                    .withPayload(mapper.writeValueAsString(payload));

                logger.addLogEntry("Invoking Lambda " + configuration.getName());
                logger.addLogEntry("With payload: " + payload);
//                lambdaClient.invoke(request);
                logger.addLogEntry("Lambda " + configuration.getName() + " finished");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
