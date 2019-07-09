package com.libertymutualgroup.herman.aws.ecs.broker.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.GetTopicAttributesResult;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test Case for SnsBroker
 *
 * @author Sabir Iqbal on 03/21/18.
 */
public class SnsBrokerTest {

    @Mock
    private HermanLogger logger;

    @Mock
    private PropertyHandler handler;

    @Mock
    private AmazonSNS client;

    private SnsBroker snsBroker;

    private String topicArn = "arn:aws:sns:us-east-1:111111:junit-arn-dummy";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        snsBroker = new SnsBroker(logger, handler);
    }

    @Test
    public void brokerTopicTopicAttribute() throws Exception {
        SnsTopic snsTopic = new SnsTopic();
        CreateTopicResult createTopicResult = new CreateTopicResult();
        createTopicResult.setTopicArn(topicArn);
        when(client.createTopic((any(CreateTopicRequest.class)))).thenReturn(createTopicResult);
        GetTopicAttributesResult result = new GetTopicAttributesResult();
        Map<String, String> attributesMap = getPossibleDeliveryAttributeNameValueMap();
        result.setAttributes(attributesMap);
        when(client.getTopicAttributes(topicArn)).thenReturn(result);
        snsBroker.brokerTopic(client, snsTopic, null);
        verify(client, times(12)).setTopicAttributes(any(SetTopicAttributesRequest.class));
    }

    @Test
    public void shouldNotSetRawMessageDeliveryIfNotSpecified() throws Exception {
        SnsTopic snsTopic = new SnsTopic();
        SnsSubscription sqs = new SnsSubscription();
        sqs.setProtocol("sqs");
        sqs.setEndpoint("test");
        sqs.setRawMessageDelivery("true");

        SnsSubscription lambda = new SnsSubscription();
        lambda.setProtocol("lambda");
        lambda.setEndpoint("test");

        List<SnsSubscription> subscriptions = new ArrayList<>();
        subscriptions.add(sqs);
        subscriptions.add(lambda);

        snsTopic.setSubscriptions(subscriptions);

        when(client.createTopic(any(CreateTopicRequest.class)))
            .thenReturn(new CreateTopicResult().withTopicArn("test"));

        when(client.getTopicAttributes(any(String.class))).thenReturn(new GetTopicAttributesResult());
        when(client.subscribe(any(SubscribeRequest.class))).thenReturn(new SubscribeResult().withSubscriptionArn("test"));

        snsBroker.brokerTopic(client, snsTopic, null);

        verify(client).subscribe(new SubscribeRequest()
            .withTopicArn("test")
            .withProtocol(lambda.getProtocol())
            .withEndpoint(lambda.getEndpoint())
        );

        Map<String,String> expectedAttributes = new HashMap<>();
        expectedAttributes.put("RawMessageDelivery", "true");

        verify(client).subscribe(new SubscribeRequest()
            .withTopicArn("test")
            .withProtocol(sqs.getProtocol())
            .withEndpoint(sqs.getEndpoint())
            .withAttributes(expectedAttributes)
        );
    }

    private Map<String, String> getPossibleDeliveryAttributeNameValueMap() {

        Map<String, String> possibleDeliveryAttributeNameValueMap = new HashMap<>();
        possibleDeliveryAttributeNameValueMap.put("Policy", "");
        possibleDeliveryAttributeNameValueMap.put("Owner", "111111");
        possibleDeliveryAttributeNameValueMap.put("SubscriptionsPending", "1");
        possibleDeliveryAttributeNameValueMap.put("TopicArn", topicArn);
        possibleDeliveryAttributeNameValueMap.put("EffectiveDeliveryPolicy", "");
        possibleDeliveryAttributeNameValueMap.put("SubscriptionsConfirmed", "1");
        possibleDeliveryAttributeNameValueMap.put("DisplayName", "");
        possibleDeliveryAttributeNameValueMap.put("SubscriptionsDeleted", "1");

        possibleDeliveryAttributeNameValueMap.put("ApplicationSuccessFeedbackRoleArn", "");
        possibleDeliveryAttributeNameValueMap.put("ApplicationSuccessFeedbackSampleRate", "100");
        possibleDeliveryAttributeNameValueMap.put("ApplicationFailureFeedbackRoleArn", "");

        possibleDeliveryAttributeNameValueMap.put("LambdaSuccessFeedbackRoleArn", "");
        possibleDeliveryAttributeNameValueMap.put("LambdaSuccessFeedbackSampleRate", "100");
        possibleDeliveryAttributeNameValueMap.put("LambdaFailureFeedbackRoleArn", "");

        possibleDeliveryAttributeNameValueMap.put("HTTPSuccessFeedbackRoleArn", "");
        possibleDeliveryAttributeNameValueMap.put("HTTPSuccessFeedbackSampleRate", "100");
        possibleDeliveryAttributeNameValueMap.put("HTTPFailureFeedbackRoleArn", "");

        possibleDeliveryAttributeNameValueMap
            .put("SQSSuccessFeedbackRoleArn", "arn:aws:iam::111111:role/SNSSuccessFeedback");
        possibleDeliveryAttributeNameValueMap.put("SQSSuccessFeedbackSampleRate", "100");
        possibleDeliveryAttributeNameValueMap
            .put("SQSFailureFeedbackRoleArn", "arn:aws:iam::111111:role/SNSFailureFeedback");
        return possibleDeliveryAttributeNameValueMap;
    }

}
