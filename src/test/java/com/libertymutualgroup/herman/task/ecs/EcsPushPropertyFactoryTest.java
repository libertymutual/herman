package com.libertymutualgroup.herman.task.ecs;

import org.junit.Assert;
import org.junit.Test;

public class EcsPushPropertyFactoryTest {

    @Test
    public void getTaskProperties() {
        // WHEN
        ECSPushTaskProperties ecsPushTaskProperties = ECSPushPropertyFactory.getTaskProperties();

        // THEN
        Assert.assertNotNull(ecsPushTaskProperties);

        Assert.assertEquals("company-test-1", ecsPushTaskProperties.getCompany());
        Assert.assertEquals("sbu-test-1", ecsPushTaskProperties.getSbu());
        Assert.assertEquals("org-test-1", ecsPushTaskProperties.getOrg());
        Assert.assertEquals(
            "6628209.dkr.ecr.us-east-1.amazonaws.com/rds-credential-broker:BUILD7-Re7db3f67a688bd6f6077272fe33d8af6028e771e",
            ecsPushTaskProperties.getRdsCredentialBrokerImage());
        Assert.assertEquals(
            "https://aws-fed.lmb.lmig.com/token/%s/read-only?target=ecs/home?region=%s#/clusters/%s/services/%s/details",
            ecsPushTaskProperties.getEcsConsoleLinkPattern());

        Assert.assertEquals(Integer.valueOf(11223344),
            ecsPushTaskProperties.getNewRelic().getAccountId());
        Assert.assertEquals("herman-newrelic-broker",
            ecsPushTaskProperties.getNewRelic().getNrLambda());

        Assert.assertEquals(4,
            ecsPushTaskProperties.getSslCertificates().size());
    }
}