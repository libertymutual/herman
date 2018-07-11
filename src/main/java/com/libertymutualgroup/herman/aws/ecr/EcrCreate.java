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
import com.amazonaws.services.ecr.model.CreateRepositoryRequest;
import com.amazonaws.services.ecr.model.CreateRepositoryResult;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.Repository;
import com.amazonaws.services.ecr.model.RepositoryAlreadyExistsException;
import com.amazonaws.services.ecr.model.SetRepositoryPolicyRequest;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcrCreate {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcrCreate.class);

    private HermanLogger buildLogger;
    private AmazonECR client;
    private Regions region;
    private AWSCredentials sessionCredentials;
    private ClientConfiguration config;

    public EcrCreate(HermanLogger buildLogger, AWSCredentials sessionCredentials, ClientConfiguration config,
        Regions region) {
        client = AmazonECRClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
            .withClientConfiguration(config).withRegion(region).build();
        this.region = region;
        this.buildLogger = buildLogger;
        this.sessionCredentials = sessionCredentials;
        this.config = config;
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

        String ecrPolicy = ConfigurationUtil.getECRPolicyAsString(sessionCredentials, buildLogger, null, this.region);
        SetRepositoryPolicyRequest setRepositoryPolicyRequest = new SetRepositoryPolicyRequest()
            .withPolicyText(ecrPolicy).withRegistryId(repo.getRegistryId())
            .withRepositoryName(repo.getRepositoryName());
        client.setRepositoryPolicy(setRepositoryPolicyRequest);
        String result = repo.getRegistryId() + ".dkr.ecr." + region.getName() + ".amazonaws.com/" + name;

        try {
            EcrTrim trimmer = new EcrTrim(buildLogger, sessionCredentials, config, region);
            trimmer.trimRepo(name);
        } catch (Exception e) {
            LOGGER.debug("Error trimming repo: " + name, e);
            buildLogger.addLogEntry("Error trimming repo: " + e.getMessage());
        }
        return result;

    }
}
