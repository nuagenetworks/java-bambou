package net.nuagenetworks.bambou.ssl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicKeystoreGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DynamicKeystoreGenerator.class);

    private static final char[] KEY_PASSWORD = "changeit".toCharArray();
    public static final String KEYSTORE_ALIAS = "somealias";
    public static final String KEY_FACTORY_INSTANCE_TYPE = "RSA";
    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "SunX509";
    private static final String KEYSTORE_INSTANCE_TYPE = "JKS";
    private static final String CERTIFICATE_FACTORY_INSTANCE_TYPE = "X.509";
    private static final String KEY_CHARSET = "UTF-8";
    private static final String PRIVATE_KEY_HEADER_TRAILER_REGEX = "(-+BEGIN PRIVATE KEY-+\\r?\\n|-+END PRIVATE KEY-+\\r?\\n?)";

    private DynamicKeystoreGenerator() {
    }

    public static KeyManager[] generateKeyManagersForCertificates(String certificateContent, String privateKeyContent) throws KeyManagementException {
        logger.debug("Creating a key manager for certificate : " + certificateContent + " and private key : " + privateKeyContent);
        try {
            KeyManager[] keyManagers = {};
            Certificate certificate = getCertificate(certificateContent);

            byte[] decodedPrivateKey = getDecodedPrivateKey(privateKeyContent);

            RSAPrivateKey rsaPrivateKey = generateRSAPrivateKey(decodedPrivateKey);

            KeyStore keystore = createAndLoadDynamicKeystore(rsaPrivateKey, certificate);
            keyManagers = generateKeyManagersForDynamicKeystore(keystore);
            return keyManagers;
        } catch (Exception ex) {
            throw new KeyManagementException(ex);
        }
    }

    public static Certificate getCertificate(String certificateContent) throws KeyManagementException {
        logger.debug("Creating a Certificate for : " + certificateContent);
        Certificate certificate = null;
        try (InputStream bais = new ByteArrayInputStream(certificateContent.getBytes())) {
            certificate = generateX509Certificate(bais);
        } catch (IOException ex) {
            throw new KeyManagementException(ex);
        }
        return certificate;
    }

    public static Certificate generateX509Certificate(InputStream is) throws KeyManagementException {
        logger.debug("Generating X509 Certificate");
        Certificate certificate = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance(CERTIFICATE_FACTORY_INSTANCE_TYPE);
            certificate = cf.generateCertificate(is);
        } catch (CertificateException ex) {
            throw new KeyManagementException(ex);
        }
        return certificate;
    }

    public static String getContentsOfPEMFile(File pemFile) throws KeyManagementException {
        logger.debug("Reading key contents for file : " + pemFile);
        byte[] keyFileBytes = {};
        String pemFileContents = "";
        try {
            Path path = Paths.get(pemFile.getPath());
            keyFileBytes = Files.readAllBytes(path);
            pemFileContents = new String(keyFileBytes, KEY_CHARSET);
        } catch (IOException ex) {
            throw new KeyManagementException(ex);
        }
        return pemFileContents;
    }

    public static byte[] getDecodedPrivateKey(String keyFileContents) {
        logger.debug("Decoding private key : " + keyFileContents);
        byte[] decodedPrivateKey = {};
        Base64 base64Decoder = new Base64();
        String privateKey = keyFileContents.replaceAll(PRIVATE_KEY_HEADER_TRAILER_REGEX, "");
        decodedPrivateKey = base64Decoder.decode(privateKey);
        return decodedPrivateKey;
    }

    public static RSAPrivateKey generateRSAPrivateKey(byte[] decodedPrivateKey) throws KeyManagementException {
        logger.debug("Generating RSA private key");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedPrivateKey);
        KeyFactory keyFactory;
        RSAPrivateKey rsaPrivateKey = null;
        try {
            keyFactory = KeyFactory.getInstance(KEY_FACTORY_INSTANCE_TYPE);
            rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new KeyManagementException(ex);
        }
        return rsaPrivateKey;
    }

    public static KeyStore createAndLoadDynamicKeystore(RSAPrivateKey rsaPrivateKey, Certificate certificate) throws KeyManagementException {
        logger.debug("Generating Keystore from RSA private key and X509 certificate");
        Certificate[] certificateChain = { certificate };
        PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(rsaPrivateKey, certificateChain);
        ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(KEY_PASSWORD);
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(KEYSTORE_INSTANCE_TYPE);
            keystore.load(null, null);
            keystore.setEntry(KEYSTORE_ALIAS, privateKeyEntry, protectionParameter);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException ex) {
            throw new KeyManagementException(ex);
        }
        return keystore;
    }

    public static KeyManager[] generateKeyManagersForDynamicKeystore(KeyStore keystore) throws KeyManagementException {
        logger.debug("Loading KeyManager from Keystore");
        KeyManager[] keyManagers = {};
        KeyManagerFactory keyManagerFactory = null;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
            keyManagerFactory.init(keystore, KEY_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            throw new KeyManagementException(ex);
        }
        return keyManagers;
    }

}
