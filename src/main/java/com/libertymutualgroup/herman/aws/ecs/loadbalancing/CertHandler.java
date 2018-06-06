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

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.libertymutualgroup.herman.aws.AwsExecException;
import java.util.List;

public class CertHandler {

    private static final String HTTPS = "HTTPS";


    private final AmazonIdentityManagement iamClient;
    private final BuildLogger buildLogger;
    private final List<SSLCertificate> sslCertificates;

    public CertHandler(AmazonIdentityManagement iamClient, BuildLogger buildLogger,
        List<SSLCertificate> sslCertificates) {
        this.iamClient = iamClient;
        this.buildLogger = buildLogger;
        this.sslCertificates = sslCertificates;
    }

    public DeriveCertResult deriveCert(String protocol, String urlSuffix, String urlPrefix) {
        DeriveCertResult deriveCertResult = new DeriveCertResult();
        if (HTTPS.equals(protocol)) {
            SSLCertificate sslCertificate = getSSLCertificateByUrl(urlPrefix, urlSuffix);
            deriveCertResult.setSslCertificate(sslCertificate);

            ListServerCertificatesResult certResult = iamClient.listServerCertificates();
            for (ServerCertificateMetadata meta : certResult.getServerCertificateMetadataList()) {
                if (meta.getArn().endsWith(sslCertificate.getPathSuffix())) {
                    deriveCertResult.setCertArn(meta.getArn());
                    break;
                }
            }
            buildLogger.addBuildLogEntry("SSL cert found: " + deriveCertResult.getCertArn());
        }
        return deriveCertResult;
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
        return sslCertificate.isInternetFacingUrl() && !"internal".equals(urlSchemeOverride);

    }
}