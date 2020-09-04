/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class TokenGenerator {

    private final static Logger LOGGER = LoggerFactory.getLogger(TokenGenerator.class);

    /**
     * RSA key size for JWT token gneration
     */
    private final static int RSA_KEY_SIZE = 2048;

    /**
     * JWT token issuer
     */
    private static final String TOKEN_ISSUER = "opensilex";

    public static String getValidationToken(long duration, TemporalUnit durantionUnit, Map<String, Object> additionalClaims) {
        if (algoRSA == null) {
            return null;
        }
        // Set issue date a now
        Date issuedDate = new Date();

        // Compute expiration date
        Date expirationDate = Date.from(issuedDate.toInstant().plus(duration, durantionUnit));

        // Create token
        JWTCreator.Builder tokenBuilder = JWT.create()
                // Standardized claims
                .withIssuer(TOKEN_ISSUER)
                .withIssuedAt(issuedDate)
                .withExpiresAt(expirationDate);

        additionalClaims.forEach((claimKey, claimValue) -> {
            tokenBuilder.withClaim(claimKey, claimValue.toString());
        });

        // Return signed token
        return tokenBuilder.sign(algoRSA);
    }

    public static String generateFileHash(InputStream fis) throws NoSuchAlgorithmException, IOException {
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
        return getFileChecksum(shaDigest, fis);
    }

    private static String getFileChecksum(MessageDigest digest, InputStream fis) throws IOException {
        //Get file input stream for reading the file content

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    private static Algorithm algoRSA = null;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(RSA_KEY_SIZE);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
            algoRSA = Algorithm.RSA512(publicKey, privateKey);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.warn("Impossible to initialize token algorithm", ex);
        }

    }
}