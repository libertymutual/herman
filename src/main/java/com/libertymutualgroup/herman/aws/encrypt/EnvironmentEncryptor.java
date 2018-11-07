package com.libertymutualgroup.herman.aws.encrypt;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EnvironmentEncryptor {

    private static final String CIPHER_PREFIX="{cipher}";
    private final AWSKMS kmsClient;

    public EnvironmentEncryptor(AWSKMS kmsClient) {
        this.kmsClient = kmsClient;
    }

    public String encrypt(String plaintext, String kmsKeyId) {
        final EncryptResult encryptResult = kmsClient.encrypt(new EncryptRequest()
                .withKeyId(kmsKeyId)
                .withPlaintext(ByteBuffer.wrap(plaintext.getBytes())));
        final ByteBuffer encryptedBytes = encryptResult.getCiphertextBlob();
        final ByteBuffer encryptedBytesBase64Encoded = ByteBuffer.wrap(Base64.getEncoder().encode(encryptedBytes.array()));
        return CIPHER_PREFIX + StandardCharsets.UTF_8.decode(encryptedBytesBase64Encoded).toString();
    }
}
