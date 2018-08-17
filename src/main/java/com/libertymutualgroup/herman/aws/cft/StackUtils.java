/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 8/1/18
 */
package com.libertymutualgroup.herman.aws.cft;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StackUtils {
    private AmazonCloudFormation cftClient;
    private HermanLogger logger;

    private static final int POLLING_INTERVAL_MS = 10000;

    public StackUtils(AmazonCloudFormation cftClient, HermanLogger logger) {
        this.cftClient = cftClient;
        this.logger = logger;
    }

    public List<StackSummary> findStacksWithName(String name) {
        ListStacksResult stacksResult = this.cftClient.listStacks();
        ArrayList<StackSummary> allStacks = new ArrayList<>(stacksResult.getStackSummaries());
        String nextToken = stacksResult.getNextToken();
        while (nextToken != null) {
            ListStacksRequest listRequest = new ListStacksRequest()
                .withNextToken(nextToken);
            ListStacksResult listStacksResult = this.cftClient.listStacks(listRequest);
            allStacks.addAll(listStacksResult.getStackSummaries());
            nextToken = listStacksResult.getNextToken();
        }

        List<StackSummary> filteredStacks = allStacks.stream().filter(stack -> stack.getStackName().contains(name)).distinct().collect(Collectors.toList());

        return filteredStacks;
    }

    public void waitForCompletion(String stackName) {
        DescribeStacksRequest wait = new DescribeStacksRequest();
        wait.setStackName(stackName);
        Boolean completed = false;

        logger.addLogEntry("Waiting...");

        // Try waiting at the start to avoid a race before the stack starts updating
        sleep();
        while (!completed) {
            List<Stack> stacks = cftClient.describeStacks(wait).getStacks();

            completed = reportStatusAndCheckCompletionOf(stacks);

            // Not done yet so sleep for 10 seconds.
            if (!completed) {
                sleep();
            }
        }

        logger.addLogEntry("done");
    }

    private void sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.addLogEntry("Interrupted while polling");
            throw new AwsExecException("Interrupted while polling");
        }
    }

    private Boolean reportStatusAndCheckCompletionOf(List<Stack> stacks) {
        for (Stack stack: stacks) {
            reportStatusOf(stack);
            if (stack.getStackStatus().contains("IN_PROGRESS")) {
                return false;
            }

            if (stack.getStackStatus().contains("FAILED") || stack.getStackStatus().contains("ROLLBACK")) {
                throw new AwsExecException("CFT pushed failed - " + stack.getStackStatus());
            }
        }
        return true;
    }

    private void reportStatusOf(Stack stack) {

        String status = stack.getStackStatus();
        String reason = stack.getStackStatusReason();
        if (reason != null) {
            status += " : " + reason;
        }
        logger.addLogEntry(status);
    }
}
