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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagUtil {
    public static List<HermanTag> mergeTags(List<HermanTag> list1, List<HermanTag> list2) {
        if (list1 == null) {
            return list2;
        }
        if (list2 == null) {
            return list1;
        }
        Map<String, String> list1map = hermanToMap(list1);
        list1map.putAll(hermanToMap(list2));

        return mapToHermanTags(list1map);
    }

    public static List<com.amazonaws.services.kms.model.Tag> hermanToKmsTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toKmsTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.rds.model.Tag> hermanToRdsTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toRdsTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.dynamodbv2.model.Tag> hermanToDynamoTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toDynamoTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.ecs.model.Tag> hermanToEcsTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toEcsTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.cloudformation.model.Tag> hermanToCftTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toCftTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.identitymanagement.model.Tag> hermanToIamTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toIamTag).collect(Collectors.toList());
    }

    public static List<com.amazonaws.services.cloudwatchevents.model.Tag> hermanToCloudWatchEventsTags(List<HermanTag> tags) {
        return tags.stream().map(HermanTag::toCloudWatchEventsTag).collect(Collectors.toList());
    }

    public static Map<String, String> hermanToMap(List<HermanTag> tags) {
        return tags.stream().collect(Collectors.toMap(HermanTag::getKey, HermanTag::getValue));
    }

    public static List<HermanTag> mapToHermanTags(Map<String, String> tagMap) {
        ArrayList<HermanTag> tags = new ArrayList<>();
        for (String tagKey : tagMap.keySet()) {
            String tagValue = tagMap.get(tagKey);
            tags.add(new HermanTag(tagKey, tagValue));
        }
        return tags;
    }

    public static TagSet hermanToTagSet(List<HermanTag> tags) {
        TagSet tagSet = new TagSet();
        for (HermanTag tag : tags) {
            tagSet.setTag(tag.getKey(), tag.getValue());
        }
        return tagSet;
    }
}
