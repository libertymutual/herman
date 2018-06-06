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
package com.libertymutualgroup.herman.aws.ecr;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.BatchDeleteImageRequest;
import com.amazonaws.services.ecr.model.BatchDeleteImageResult;
import com.amazonaws.services.ecr.model.CreateRepositoryRequest;
import com.amazonaws.services.ecr.model.CreateRepositoryResult;
import com.amazonaws.services.ecr.model.DescribeImagesRequest;
import com.amazonaws.services.ecr.model.DescribeImagesResult;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ImageDetail;
import com.amazonaws.services.ecr.model.ImageIdentifier;
import com.amazonaws.services.ecr.model.Repository;
import com.amazonaws.services.ecr.model.RepositoryAlreadyExistsException;
import com.amazonaws.services.ecr.model.SetRepositoryPolicyRequest;
import com.amazonaws.util.IOUtils;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.libertymutualgroup.herman.aws.AwsExecException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcrCreate {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcrCreate.class);

    private BuildLogger buildLogger;
    private AmazonECR client;
    private Regions region;

    public EcrCreate(BuildLogger buildLogger, AWSCredentials sessionCredentials, ClientConfiguration config,
        Regions region) {
        client = AmazonECRClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
            .withClientConfiguration(config).withRegion(region).build();
        this.region = region;
        this.buildLogger = buildLogger;
    }

    public String createRepo(String name) {

        CreateRepositoryRequest req = new CreateRepositoryRequest();
        req.setRepositoryName(name);
        Repository repo;
        try {
            CreateRepositoryResult result = client.createRepository(req);
            repo = result.getRepository();
        } catch (RepositoryAlreadyExistsException e) {
            LOGGER.debug("Repo already exists: " + name, e);

            DescribeRepositoriesRequest search = new DescribeRepositoriesRequest();
            search.withRepositoryNames(name);
            DescribeRepositoriesResult searchRes = client.describeRepositories(search);
            repo = searchRes.getRepositories().get(0);
        }

        try {
            InputStream policyStream = getClass().getResourceAsStream("/iam/ecr-policy.json");
            String policy = IOUtils.toString(policyStream);

            SetRepositoryPolicyRequest setRepositoryPolicyRequest = new SetRepositoryPolicyRequest()
                .withPolicyText(policy).withRegistryId(repo.getRegistryId())
                .withRepositoryName(repo.getRepositoryName());
            client.setRepositoryPolicy(setRepositoryPolicyRequest);
        } catch (IOException e) {
            throw new AwsExecException(e);
        }
        String result = repo.getRegistryId() + ".dkr.ecr." + region.getName() + ".amazonaws.com/" + name;

        try {
            trimRepo(client, name);
        } catch (Exception e) {
            LOGGER.debug("Error trimming repo: " + name, e);
            buildLogger.addBuildLogEntry("Error trimming repo: " + e.getMessage());
        }
        return result;

    }

    private void trimRepo(AmazonECR client, String name) {
        DescribeImagesRequest request = new DescribeImagesRequest().withRepositoryName(name);
        DescribeImagesResult result = client.describeImages(request);
        List<ImageDetail> images = new ArrayList<>();

        images.addAll(result.getImageDetails());
        String nextToken = result.getNextToken();
        while (nextToken != null) {
            request = new DescribeImagesRequest().withRepositoryName(name).withNextToken(nextToken);
            result = client.describeImages(request);
            images.addAll(result.getImageDetails());
            nextToken = result.getNextToken();
        }
        Collections.sort(images, new ImageDateComparator());
        Collections.reverse(images);

        // Keep newest 750 images - 1000 cap
        List<ImageIdentifier> toDelete = new ArrayList<>();
        for (int i = 750; i < images.size(); i++) {
            ImageDetail deleteMe = images.get(i);
            if (deleteMe.getImageTags() != null && !deleteMe.getImageTags().isEmpty()) {
                toDelete.add(new ImageIdentifier().withImageDigest(deleteMe.getImageDigest())
                    .withImageTag(deleteMe.getImageTags().get(0)));
            }
        }
        // if over 100, delete oldest 100 in this batch
        Collections.reverse(toDelete);
        if (toDelete.size() > 100) {
            toDelete = toDelete.subList(0, 99);
        }

        for (ImageIdentifier detail : toDelete) {
            buildLogger.addBuildLogEntry("DELETING: " + detail.getImageTag());
        }
        if (!toDelete.isEmpty()) {
            BatchDeleteImageRequest delReq = new BatchDeleteImageRequest().withRepositoryName(name)
                .withImageIds(toDelete);
            BatchDeleteImageResult res = client.batchDeleteImage(delReq);
            buildLogger.addBuildLogEntry("DELETE called with " + res.getFailures().size() + " failed images.");
        }

    }

    private class ImageDateComparator implements Comparator<ImageDetail> {

        @Override
        public int compare(ImageDetail o1, ImageDetail o2) {
            return o1.getImagePushedAt().compareTo(o2.getImagePushedAt());
        }

    }

}
