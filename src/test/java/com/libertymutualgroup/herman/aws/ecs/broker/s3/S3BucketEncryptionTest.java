package com.libertymutualgroup.herman.aws.ecs.broker.s3;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class S3BucketEncryptionTest {

    @Test
    public void enablesEncryptionOnNewBuckets() {
        String bucketName = "my.bucket";
        boolean bucketExists = false;
        AmazonS3 s3Client = mock(AmazonS3.class);
        HermanLogger logger = mock(HermanLogger.class);

        S3BucketEncryption s3BucketEncryption = new S3BucketEncryption(bucketName, bucketExists, s3Client, logger);
        s3BucketEncryption.ensureEncryption();

        ArgumentCaptor<SetBucketEncryptionRequest> requestCaptor = ArgumentCaptor
            .forClass(SetBucketEncryptionRequest.class);
        verify(s3Client).setBucketEncryption(requestCaptor.capture());
        SetBucketEncryptionRequest request = requestCaptor.getValue();
        assertEquals(bucketName, request.getBucketName());
        assertNotNull(
            request.getServerSideEncryptionConfiguration().getRules().get(0).getApplyServerSideEncryptionByDefault());
        assertEquals("AES256",
            request.getServerSideEncryptionConfiguration().getRules().get(0).getApplyServerSideEncryptionByDefault()
                .getSSEAlgorithm());
    }

    @Test
    public void enablesEncryptionOnExistingBuckets() {
        String bucketName = "my.bucket";
        boolean bucketExists = true;
        AmazonS3 s3Client = mock(AmazonS3.class);
        HermanLogger logger = mock(HermanLogger.class);

        when(s3Client.getBucketEncryption(eq(bucketName)))
            .thenThrow(new AmazonS3Exception("The server side encryption configuration was not found"));

        S3BucketEncryption s3BucketEncryption = new S3BucketEncryption(bucketName, bucketExists, s3Client, logger);
        s3BucketEncryption.ensureEncryption();

        ArgumentCaptor<SetBucketEncryptionRequest> requestCaptor = ArgumentCaptor
            .forClass(SetBucketEncryptionRequest.class);
        verify(s3Client).getBucketEncryption(eq(bucketName));
        verify(s3Client).setBucketEncryption(requestCaptor.capture());
        SetBucketEncryptionRequest request = requestCaptor.getValue();
        assertEquals(bucketName, request.getBucketName());
        assertNotNull(
            request.getServerSideEncryptionConfiguration().getRules().get(0).getApplyServerSideEncryptionByDefault());
        assertEquals("AES256",
            request.getServerSideEncryptionConfiguration().getRules().get(0).getApplyServerSideEncryptionByDefault()
                .getSSEAlgorithm());
    }

    @Test
    public void skipsWhenEncryptionIsAlreadyEnabled() {
        String bucketName = "my.bucket";
        boolean bucketExists = true;
        AmazonS3 s3Client = mock(AmazonS3.class);
        HermanLogger logger = mock(HermanLogger.class);

        GetBucketEncryptionResult encryptionResult = new GetBucketEncryptionResult();
        encryptionResult.setServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration());
        encryptionResult.getServerSideEncryptionConfiguration()
            .setRules(Collections.singletonList(new ServerSideEncryptionRule()));
        encryptionResult.getServerSideEncryptionConfiguration().getRules().get(0)
            .setApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault());
        encryptionResult.getServerSideEncryptionConfiguration().getRules().get(0)
            .getApplyServerSideEncryptionByDefault().setSSEAlgorithm("AES256");
        when(s3Client.getBucketEncryption(eq(bucketName))).thenReturn(encryptionResult);

        S3BucketEncryption s3BucketEncryption = new S3BucketEncryption(bucketName, bucketExists, s3Client, logger);
        s3BucketEncryption.ensureEncryption();

        verify(s3Client).getBucketEncryption(eq(bucketName));
        verify(s3Client, times(0)).setBucketEncryption(any(SetBucketEncryptionRequest.class));
    }
}
