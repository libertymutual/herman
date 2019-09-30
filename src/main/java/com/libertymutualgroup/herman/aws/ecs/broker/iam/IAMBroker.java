/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.aws.ecs.broker.iam;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.Tag;
import com.amazonaws.services.identitymanagement.model.TagRoleRequest;
import com.amazonaws.services.identitymanagement.model.UntagRoleRequest;
import com.amazonaws.services.identitymanagement.model.UpdateAssumeRolePolicyRequest;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.PushType;
import com.libertymutualgroup.herman.aws.tags.HermanTag;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class IAMBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(IAMBroker.class);

    private static final String POLICY_SUFFIX = "-policy";
    private HermanLogger buildLogger;

    public IAMBroker(HermanLogger buildLogger) {
        this.buildLogger = buildLogger;
    }

    public Role brokerAppRole(AmazonIdentityManagement client, IamAppDefinition definition, String rolePolicy,
        PropertyHandler propertyHandler) {
        return brokerAppRole(client, definition, rolePolicy, propertyHandler, PushType.ECS, definition.getTags());
    }

    public Role brokerAppRole(AmazonIdentityManagement client, IamAppDefinition definition, String rolePolicy,
        PropertyHandler propertyHandler, PushType pushType, List<HermanTag> tags) {
        String appName = definition.getAppName();
        Role role = getRole(client, appName);

        String assumePolicy;
        try {
            InputStream policyStream = getClass()
                .getResourceAsStream("/iam/assume-role-policy-" + pushType.name().toLowerCase() + ".json");
            assumePolicy = IOUtils.toString(policyStream);
        } catch (IOException e) {
            throw new AwsExecException("Error getting assume policy", e);
        }

        if (role == null) {
            buildLogger.addLogEntry("... Creating new role: " + definition.getAppName());

            CreateRoleRequest createRoleRequest = new CreateRoleRequest()
                .withPath("/aws-ecs/")
                .withRoleName(definition.getAppName())
                .withAssumeRolePolicyDocument(assumePolicy);
            client.createRole(createRoleRequest);

        } else {
            buildLogger.addLogEntry("... Using existing role: " + definition.getAppName());

            client.updateAssumeRolePolicy(new UpdateAssumeRolePolicyRequest().withRoleName(definition.getAppName())
                .withPolicyDocument(assumePolicy));
        }

        if (rolePolicy != null) {
            buildLogger.addLogEntry("... Updating the role policy");
            String fullPolicy = propertyHandler.mapInProperties(rolePolicy);
            PutRolePolicyRequest putRolePolicyRequest = new PutRolePolicyRequest()
                .withPolicyName(appName + POLICY_SUFFIX)
                .withRoleName(appName).withPolicyDocument(fullPolicy);
            client.putRolePolicy(putRolePolicyRequest);
        } else {
            try {
                client.getRolePolicy(
                    new GetRolePolicyRequest().withPolicyName(appName + POLICY_SUFFIX).withRoleName(appName));
                client.deleteRolePolicy(
                    new DeleteRolePolicyRequest().withPolicyName(appName + POLICY_SUFFIX).withRoleName(appName));
                buildLogger.addLogEntry("... No policy specified. The role policy was deleted.");
            } catch (NoSuchEntityException e) {
                LOGGER.debug("Role policy does not exist: " + appName + POLICY_SUFFIX, e);
            }
        }

        role = getRole(client, appName);
        List<Tag> existingTags = role.getTags();
        if (existingTags != null && existingTags.size() > 0 && tags != null && tags.size() > 0) {
            List<Tag> tagsToRemove = existingTags.stream().filter(existingTag ->
                tags.stream().noneMatch(newTag -> newTag.getKey().equals(existingTag.getKey()))
            ).collect(Collectors.toList());

            if (tagsToRemove.size() > 0) { // Remove tags that are on role but not new deploy
                buildLogger.addLogEntry("Clearing old IAM tags");
                client.untagRole(new UntagRoleRequest()
                    .withRoleName(appName)
                    .withTagKeys(tagsToRemove.stream().map(Tag::getKey).collect(Collectors.toList()))
                );
            }
        }
        else if (existingTags != null && existingTags.size() > 0) { // Just clear all tags, there are no new tags on the
            buildLogger.addLogEntry("Clearing old IAM tags");
            client.untagRole(new UntagRoleRequest()
                .withRoleName(appName)
                .withTagKeys(existingTags.stream().map(Tag::getKey).collect(Collectors.toList()))
            );
        }

        if (tags != null && tags.size() > 0) {
            buildLogger.addLogEntry("Updating IAM tags");
            List<Tag> iamTags = TagUtil.hermanToIamTags(tags);
            client.tagRole(new TagRoleRequest().withRoleName(appName).withTags(iamTags));
        }

        try {
            //Roles take a short bit to percolate in IAM, no real status
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AwsExecException(e);
        }
        buildLogger.addLogEntry("... App role ARN: " + role.getArn());
        return role;

    }

    public Role getRole(AmazonIdentityManagement client, String roleName) {
        try {
            return client.getRole(new GetRoleRequest().withRoleName(roleName)).getRole();
        } catch (AmazonServiceException ase) {
            if ("NoSuchEntity".equals(ase.getErrorCode())) {
                return null;
            }
            throw ase;
        }
    }

}
