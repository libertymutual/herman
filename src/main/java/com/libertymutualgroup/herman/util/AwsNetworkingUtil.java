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
package com.libertymutualgroup.herman.util;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import java.util.ArrayList;
import java.util.List;

public class AwsNetworkingUtil {

    private AmazonEC2 ec2Client;

    public AwsNetworkingUtil(AmazonEC2 ec2Client) {
        this.ec2Client = ec2Client;
    }

    public Vpc getVpc(String vpcId) {
        return this.ec2Client
            .describeVpcs()
            .getVpcs().stream()
            .filter(vpc -> vpc.getVpcId().equals(vpcId))
            .findAny()
            .orElse(null);
    }

    public List<Subnet> getSubnets() {
        return this.ec2Client.describeSubnets().getSubnets();
    }


    public List<SecurityGroup> findSecurityGroupsByClusterName(Vpc vpc, String clusterName) {
        List<SecurityGroup> securityGroupResult = this.ec2Client.describeSecurityGroups().getSecurityGroups();

        ArrayList<SecurityGroup> securityGroups = new ArrayList<>();
        for (SecurityGroup secGrp : securityGroupResult) {
            if (secGrp.getVpcId().equals(vpc.getVpcId()) && elementContainsTag(secGrp.getTags(), "Name", clusterName)) {
                securityGroups.add(secGrp);
            }
        }
        return securityGroups;
    }

    private boolean elementContainsTag(List<Tag> tags, String tagKey, String tagValue) {
        for (Tag tag : tags) {
            if (tag.getKey().equals(tagKey) && tag.getValue().equals(tagValue)) {
                return true;
            }
        }
        return false;
    }
}
