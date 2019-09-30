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
package com.libertymutualgroup.herman.aws.tags;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class HermanTagTest {
    private HermanTag testTag = new HermanTag("test", "test");

    @Test
    public void testTagToEc2() {
        assertThat(testTag.toEc2Tag(), instanceOf(com.amazonaws.services.ec2.model.Tag.class));
    }

    @Test
    public void testTagToRds() {
        assertThat(testTag.toRdsTag(), instanceOf(com.amazonaws.services.rds.model.Tag.class));
    }

    @Test
    public void testTagToKms() {
        assertThat(testTag.toKmsTag(), instanceOf(com.amazonaws.services.kms.model.Tag.class));
    }

    @Test
    public void testTagToDynamo() {
        assertThat(testTag.toDynamoTag(), instanceOf(com.amazonaws.services.dynamodbv2.model.Tag.class));
    }

    @Test
    public void testTagToIam(){
        assertThat(testTag.toIamTag(), instanceOf(com.amazonaws.services.identitymanagement.model.Tag.class));
    }
}
