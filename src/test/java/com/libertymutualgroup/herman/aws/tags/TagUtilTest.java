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

import com.amazonaws.services.s3.model.TagSet;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class TagUtilTest {
    @Test
    public void testMergeTagsNoDuplicates() {
        ArrayList<HermanTag> list1 = new ArrayList<>();
        list1.add(new HermanTag("test", "test"));
        ArrayList<HermanTag> list2 = new ArrayList<>();
        list2.add(new HermanTag("test2", "test2"));

        List<HermanTag> mergedTags = TagUtil.mergeTags(list1, list2);

        assertEquals(2, mergedTags.size());
    }

    @Test
    public void testMergeTagsWithDuplicates() {
        ArrayList<HermanTag> list1 = new ArrayList<>();
        list1.add(new HermanTag("test", "test"));
        ArrayList<HermanTag> list2 = new ArrayList<>();
        list2.add(new HermanTag("test", "test2"));

        List<HermanTag> mergedTags = TagUtil.mergeTags(list1, list2);

        assertEquals(1, mergedTags.size());
    }

    @Test
    public void testMergeTagsWithDuplicatesOverride() {
        ArrayList<HermanTag> list1 = new ArrayList<>();
        list1.add(new HermanTag("test", "test"));
        ArrayList<HermanTag> list2 = new ArrayList<>();
        list2.add(new HermanTag("test", "test2"));

        List<HermanTag> mergedTags = TagUtil.mergeTags(list1, list2);

        assertEquals("test2", mergedTags.get(0).getValue());
    }

    @Test
    public void testMergeTagsWithNullReturnsOtherList() {
        ArrayList<HermanTag> list1 = new ArrayList<>();
        list1.add(new HermanTag("test", "test"));

        List<HermanTag> mergedTags = TagUtil.mergeTags(list1, null);
        assertEquals(1, mergedTags.size());
    }

    @Test
    public void testMergeTagsWithNullReturnsOtherListSecondParam() {
        ArrayList<HermanTag> list1 = new ArrayList<>();
        list1.add(new HermanTag("test", "test"));

        List<HermanTag> mergedTags = TagUtil.mergeTags(null, list1);
        assertEquals(1, mergedTags.size());
    }

    @Test
    public void testHermanToKmsTags() {
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        List<com.amazonaws.services.kms.model.Tag> tags = TagUtil.hermanToKmsTags(hermanTags);
        assertEquals(testKey, tags.get(0).getTagKey());
        assertEquals(testValue, tags.get(0).getTagValue());
    }

    @Test
    public void testHermanToRdsTags() {
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        List<com.amazonaws.services.rds.model.Tag> tags = TagUtil.hermanToRdsTags(hermanTags);
        assertEquals(testKey, tags.get(0).getKey());
        assertEquals(testValue, tags.get(0).getValue());
    }

    @Test
    public void testHermanToDynamoTags() {
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        List<com.amazonaws.services.dynamodbv2.model.Tag> tags = TagUtil.hermanToDynamoTags(hermanTags);
        assertEquals(testKey, tags.get(0).getKey());
        assertEquals(testValue, tags.get(0).getValue());
    }

    @Test
    public void testHermanToIamTags(){
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        List<com.amazonaws.services.identitymanagement.model.Tag> tags = TagUtil.hermanToIamTags(hermanTags);
        assertEquals(testKey, tags.get(0).getKey());
        assertEquals(testValue, tags.get(0).getValue());
    }

    @Test
    public void testHermanToMap() {
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        Map<String, String> tags = TagUtil.hermanToMap(hermanTags);
        assertTrue(tags.containsKey(testKey));
        assertEquals(testValue, tags.get(testKey));
    }

    @Test
    public void testMapToHermanTags() {
        String testKey = "testKey";
        String testValue = "testValue";
        HashMap<String, String> tagMap = new HashMap<>();
        tagMap.put(testKey, testValue);

        List<HermanTag> tags = TagUtil.mapToHermanTags(tagMap);
        assertEquals(testKey, tags.get(0).getKey());
        assertEquals(testValue, tags.get(0).getValue());
    }

    @Test
    public void testHermanToTagSet() {
        String testKey = "testKey";
        String testValue = "testValue";
        ArrayList<HermanTag> hermanTags = new ArrayList<>();
        hermanTags.add(new HermanTag(testKey, testValue));

        TagSet tags = TagUtil.hermanToTagSet(hermanTags);
        assertNotNull(tags.getTag(testKey));
        assertEquals(testValue, tags.getTag(testKey));
    }
}
