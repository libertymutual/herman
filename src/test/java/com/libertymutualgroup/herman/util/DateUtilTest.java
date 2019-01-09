package com.libertymutualgroup.herman.util;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

public class DateUtilTest {

    @Test
    public void shouldGenerateDateString() {
        DateTime dateTime = new DateTime("2018-05-25T09:31:43.086-04:00");
        String date = DateUtil.getDateAsString(dateTime);
        Assert.assertEquals("05-25-18-01-31-43", date);
    }
}
