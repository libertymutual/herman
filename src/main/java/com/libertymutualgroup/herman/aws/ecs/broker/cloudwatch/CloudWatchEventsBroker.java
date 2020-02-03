package com.libertymutualgroup.herman.aws.ecs.broker.cloudwatch;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.DescribeRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.DescribeRuleResult;
import com.amazonaws.services.cloudwatchevents.model.PutRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutRuleResult;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsResult;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.ResourceNotFoundException;
import com.amazonaws.services.cloudwatchevents.model.RuleState;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.libertymutualgroup.herman.aws.lambda.LambdaInjectConfiguration;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;

public class CloudWatchEventsBroker {

    private HermanLogger buildLogger;
    private AmazonCloudWatchEvents amazonCloudWatchEvents;

    public CloudWatchEventsBroker(HermanLogger buildLogger, AmazonCloudWatchEvents amazonCloudWatchEvents) {
        this.buildLogger = buildLogger;
        this.amazonCloudWatchEvents = amazonCloudWatchEvents;
    }

    public void brokerScheduledRule(LambdaInjectConfiguration configuration, GetFunctionResult output) {
        if (configuration.getScheduleExpression() != null) {
            this.buildLogger.addLogEntry("Brokering Scheduled Rule with schedule expression: " + configuration.getScheduleExpression());
            PutRuleRequest putRuleRequest = new PutRuleRequest()
                    .withName(configuration.getFunctionName() + "-scheduled-trigger")
                    .withScheduleExpression(configuration.getScheduleExpression())
                    .withState(RuleState.ENABLED)
                    .withTags(TagUtil.hermanToCloudWatchEventsTags(configuration.getTags()));

            PutRuleResult putRuleResult = this.amazonCloudWatchEvents.putRule(putRuleRequest);
            this.buildLogger.addLogEntry("Created Rule: " + putRuleResult.getRuleArn());

            Target target = new Target().withArn(output.getConfiguration().getFunctionArn()).withId(configuration.getFunctionName());
            PutTargetsRequest putTargetsRequest = new PutTargetsRequest().withTargets(target).withRule(configuration.getFunctionName() + "-scheduled-trigger");
            PutTargetsResult putTargetsResult = this.amazonCloudWatchEvents.putTargets(putTargetsRequest);
            this.buildLogger.addLogEntry("Added target " + putTargetsResult.toString() + "to rule " + putRuleResult.getRuleArn());
        } else {
            this.buildLogger.addLogEntry("No schedule expression provided. Removing any existing scheduled rules.");
            DescribeRuleRequest describeRuleRequest = new DescribeRuleRequest()
                    .withName(configuration.getFunctionName() + "-scheduled-trigger");
            try {
                DescribeRuleResult describeRuleResult = this.amazonCloudWatchEvents.describeRule(describeRuleRequest);

                RemoveTargetsRequest removeTargetsRequest = new RemoveTargetsRequest()
                        .withRule(configuration.getFunctionName() + "-scheduled-trigger")
                        .withIds(configuration.getFunctionName());
                buildLogger.addLogEntry("Removing target " + configuration.getFunctionName());
                this.amazonCloudWatchEvents.removeTargets(removeTargetsRequest);

                DeleteRuleRequest deleteRuleRequest = new DeleteRuleRequest()
                        .withName(describeRuleResult.getName());
                this.amazonCloudWatchEvents.deleteRule(deleteRuleRequest);
                buildLogger.addLogEntry("Deleted existing scheduled rule: " + describeRuleResult.getName());
            } catch (ResourceNotFoundException e) {
                buildLogger.addLogEntry("No scheduled rule found. Skipping...");
            } catch (Exception e) {
                buildLogger.addErrorLogEntry("Exception while deleting scheduled rule.", e);
            }
        }
    }
}