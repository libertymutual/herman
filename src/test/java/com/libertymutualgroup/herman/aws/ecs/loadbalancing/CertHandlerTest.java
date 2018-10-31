package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import static org.junit.Assert.*;

import org.junit.Test;

public class CertHandlerTest {

    @Test
    public void isInternetFacingUrlScheme_true() {
        // GIVEN
        SSLCertificate sslCertificate = new SSLCertificate().withInternetFacingUrl(true);
        CertHandler certHandler = new CertHandler(null, null);

        // WHEN
        boolean isInternetFacingUrlScheme = certHandler.isInternetFacingUrlScheme(sslCertificate, "internet-facing");

        // THEN
        assertTrue(isInternetFacingUrlScheme);
    }

    @Test
    public void isInternetFacingUrlScheme_falseOverride() {
        // GIVEN
        SSLCertificate sslCertificate = new SSLCertificate().withInternetFacingUrl(true);
        CertHandler certHandler = new CertHandler(null, null);

        // WHEN
        boolean isInternetFacingUrlScheme = certHandler.isInternetFacingUrlScheme(sslCertificate, "internal");

        // THEN
        assertFalse(isInternetFacingUrlScheme);
    }

    @Test
    public void isInternetFacingUrlScheme_false() {
        // GIVEN
        SSLCertificate sslCertificate = new SSLCertificate().withInternetFacingUrl(false);
        CertHandler certHandler = new CertHandler(null, null);

        // WHEN
        boolean isInternetFacingUrlScheme = certHandler.isInternetFacingUrlScheme(sslCertificate, null);

        // THEN
        assertFalse(isInternetFacingUrlScheme);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isInternetFacingUrlScheme_certNotFound() {
        // GIVEN
        CertHandler certHandler = new CertHandler(null, null);

        // WHEN
        boolean isInternetFacingUrlScheme = certHandler.isInternetFacingUrlScheme(null, null);

        // THEN
        // expect exception
    }
}