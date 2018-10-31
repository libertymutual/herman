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
package com.libertymutualgroup.herman.aws.ecs.loadbalancing;

import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.List;
import org.springframework.util.Assert;

public class CertHandler {

    private static final String HTTPS = "HTTPS";

    private final HermanLogger buildLogger;
    private final List<SSLCertificate> sslCertificates;

    public CertHandler(HermanLogger buildLogger, List<SSLCertificate> sslCertificates) {
        this.buildLogger = buildLogger;
        this.sslCertificates = sslCertificates;
    }

    public SSLCertificate deriveCert(String protocol, String urlSuffix, String urlPrefix) {
        SSLCertificate sslCertificate = null;
        if (HTTPS.equals(protocol)) {
            sslCertificate = getSSLCertificateByUrl(urlPrefix, urlSuffix);
            buildLogger.addLogEntry("SSL cert found: " + sslCertificate.getArn());
        }
        return sslCertificate;
    }

    public SSLCertificate getSSLCertificateByUrl(String prefix, String suffix) {
        //Look for a wildcard first
        for (SSLCertificate cert : this.sslCertificates) {
            if (cert.getUrlSuffix().equals(suffix) && "*".equals(cert.getUrlPrefix())) {
                return cert;
            }
        }

        //Look for a full match
        for (SSLCertificate cert : this.sslCertificates) {
            if (cert.getUrlSuffix().equals(suffix) && prefix.equals(cert.getUrlPrefix())) {
                return cert;
            }
        }
        throw new AwsExecException("No cert match found for " + suffix);
    }

    public boolean isInternetFacingUrlScheme(SSLCertificate sslCertificate, String urlSchemeOverride) {
        boolean isInternetFacingUrlScheme = false;
        if (!"internal".equals(urlSchemeOverride)) {
            Assert.notNull(sslCertificate, "SSL cert not found");
            if (Boolean.TRUE.equals(sslCertificate.isInternetFacingUrl())) {
                isInternetFacingUrlScheme = true;
            }
        }
        return isInternetFacingUrlScheme;
    }
}