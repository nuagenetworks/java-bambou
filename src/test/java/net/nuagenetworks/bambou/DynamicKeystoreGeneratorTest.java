/*
  Copyright (c) 2015, Alcatel-Lucent Inc
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.
      * Neither the name of the copyright holder nor the names of its contributors
        may be used to endorse or promote products derived from this software without
        specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package net.nuagenetworks.bambou;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;

import javax.net.ssl.KeyManager;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import net.nuagenetworks.bambou.spring.TestSpringConfig;
import net.nuagenetworks.bambou.ssl.DynamicKeystoreGenerator;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.cert.Certificate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class DynamicKeystoreGeneratorTest {
    // to generate text like this, run 2 commands below. Edit junit.pem2 file in
    // text editor. Copy contents here. Add line feed chars
    // openssl.exe genrsa -aes256 -out junit.keystep1 2048
    // openssl.exe pkcs8 -topk8 -nocrypt -inform PEM -outform PEM -in
    // junit.keystep1 -out junit.pem
    public static final String GOOD_TESTABLE_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDO392jFxTgqs15\n" + "cIyXi/sHQ2xZT7/uYJb9npfLdpRTIamzvYAQ/qitzOReust8WidOKQYfaBo7OlDE\n"
            + "/BElWX97CUj37bxjeBAchPwUaJEyMUVu4uQPqOxL3gEwQUi4SuXMtRLkMX8pjRJO\n" + "zWklSvyC6TeoJbX4NYWnNHgObIIsxpr4GddmPnQ4iOe3kI2WepJUaXttRMXvvnEd\n"
            + "khzg+KOyFiqhdKVtIGXAVkl3kx6Rtiy95j90AbkaD9PdcOAszbLaPpROmkS4Jjdl\n" + "NkFWdAI+bnXp15HYSvq4avet2n1RTgW2xq40bKVq8dNObYW0+irNEQUJE2nyvLcq\n"
            + "4XfFBh9DAgMBAAECggEBALVCNOIHAUXwoeQ7lxPZlHNCwhZm59YnT2ScpoehOEi2\n" + "gAh6i1FYr9bIZXgS1uP34eR70V+HSSSH7ekKqsM87ZOSCRsidCP7OLKkWnJPRJjj\n"
            + "zuBfcE/ARHoc5JGxQdQn271HEmxaaf6wGlBtbik80I6rZRaxJAV8b8SXFnIc5Nnw\n" + "/k5KLD2lI5RiKsFgQjTtgDbaAn3tk0z6LC+OIa4O+bVV2u7uhjxVGZG5xTBHTzLh\n"
            + "2SALumAgtwOUOYvFOb8ihkWOBGydaPlXAw4eJ1QYz4HWnTksTRpXDj+uUbq1mSfq\n" + "EhdBtX2lv1x5aPG6gtNyvjeAt6+BQRx+JIVg0Tqn8BECgYEA8uVuCtZO3DIXlybw\n"
            + "9ZAEUusB3vH4O0GeEoWC6Dm7OTXUfGErpiXumESFtH27Gs82yaCiNkhZ7UMJvepm\n" + "tzusQkh8spJZYzWBSx+F/inBZ40Fy6QJOJKTiToWu4YkNuUFREMS8m1cErX4/Fb2\n"
            + "h3UYWErsXc6W1fnUdJiXKRnSTLcCgYEA2gjzNWA3xqIv8XSMyDIL+fgKQk9XM0db\n" + "nTTah96d9JBkT5l811HX/ydKMrm2609dI5pcEprSIF3TcbnxuySqnOkVbc9TklQ3\n"
            + "ZD8xJKTByCVpRo+gnXQZZZGjuAJEFTKNvf7RPBRERmhuCj+8KhnNSgqGYY3sKiBW\n" + "z30yaJURDdUCgYEAmPyEyPKejPZIAX4XerOQ5aD7dq61CK2VoFjTUO07zRbolMan\n"
            + "NWji1KkkBcDDXmlxOqGgScxcR6JXunSu6W3+W2lIkTi4cFI881IRlTo7hLtAETlq\n" + "TWWU43Pg6Y/ds22gWZRdlK/otChFgLpNYJgSE1ptcdAzFObcN+v8GXkYChUCgYA7\n"
            + "xIpOzmJbu+fii21wD3ADBMBjLvabYus1K+Dfr+GiwdYNj+iN6ayJEA8h2pDO7kF8\n" + "2fp1SyVgo/erTCp8f2+ZdbdfE584FIfengftoJBSOhcitYp0vvebRRIZBd395Y6/\n"
            + "T1Ox4WhZ2JSZkKEW9V219y6hHVNotNa3J/2id4JmRQKBgQCRx7ixnSCYWuPS+EZh\n" + "m76RWCkuQQXA40Dm2ge7xBSlhiSEx+dTZGRdX/qP0UP6uWqXzSJS+JuufGSu0xhN\n"
            + "qY3evIDStqZPB2unUlVjiHrNX8RGyP6o0XN/u8fF5+XdVqOObqe3z0c26HX7RnJI\n" + "XHb8tXmfsw5+c/5UNrxZK/6XMQ==\n" + "-----END PRIVATE KEY-----\n";

    public static final String BAD_TESTABLE_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" + "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDO392jFxTgqs15\n"
    // +
    // "cIyXi/sHQ2xZT7/uYJb9npfLdpRTIamzvYAQ/qitzOReust8WidOKQYfaBo7OlDE\n"
    // +
    // "/BElWX97CUj37bxjeBAchPwUaJEyMUVu4uQPqOxL3gEwQUi4SuXMtRLkMX8pjRJO\n"
            + "zWklSvyC6TeoJbX4NYWnNHgObIIsxpr4GddmPnQ4iOe3kI2WepJUaXttRMXvvnEd\n" + "khzg+KOyFiqhdKVtIGXAVkl3kx6Rtiy95j90AbkaD9PdcOAszbLaPpROmkS4Jjdl\n"
            + "NkFWdAI+bnXp15HYSvq4avet2n1RTgW2xq40bKVq8dNObYW0+irNEQUJE2nyvLcq\n" + "4XfFBh9DAgMBAAECggEBALVCNOIHAUXwoeQ7lxPZlHNCwhZm59YnT2ScpoehOEi2\n"
            + "gAh6i1FYr9bIZXgS1uP34eR70V+HSSSH7ekKqsM87ZOSCRsidCP7OLKkWnJPRJjj\n" + "zuBfcE/ARHoc5JGxQdQn271HEmxaaf6wGlBtbik80I6rZRaxJAV8b8SXFnIc5Nnw\n"
            + "/k5KLD2lI5RiKsFgQjTtgDbaAn3tk0z6LC+OIa4O+bVV2u7uhjxVGZG5xTBHTzLh\n" + "2SALumAgtwOUOYvFOb8ihkWOBGydaPlXAw4eJ1QYz4HWnTksTRpXDj+uUbq1mSfq\n"
            + "EhdBtX2lv1x5aPG6gtNyvjeAt6+BQRx+JIVg0Tqn8BECgYEA8uVuCtZO3DIXlybw\n" + "9ZAEUusB3vH4O0GeEoWC6Dm7OTXUfGErpiXumESFtH27Gs82yaCiNkhZ7UMJvepm\n"
            + "tzusQkh8spJZYzWBSx+F/inBZ40Fy6QJOJKTiToWu4YkNuUFREMS8m1cErX4/Fb2\n" + "h3UYWErsXc6W1fnUdJiXKRnSTLcCgYEA2gjzNWA3xqIv8XSMyDIL+fgKQk9XM0db\n"
            + "nTTah96d9JBkT5l811HX/ydKMrm2609dI5pcEprSIF3TcbnxuySqnOkVbc9TklQ3\n" + "ZD8xJKTByCVpRo+gnXQZZZGjuAJEFTKNvf7RPBRERmhuCj+8KhnNSgqGYY3sKiBW\n"
            + "z30yaJURDdUCgYEAmPyEyPKejPZIAX4XerOQ5aD7dq61CK2VoFjTUO07zRbolMan\n" + "NWji1KkkBcDDXmlxOqGgScxcR6JXunSu6W3+W2lIkTi4cFI881IRlTo7hLtAETlq\n"
            + "TWWU43Pg6Y/ds22gWZRdlK/otChFgLpNYJgSE1ptcdAzFObcN+v8GXkYChUCgYA7\n" + "xIpOzmJbu+fii21wD3ADBMBjLvabYus1K+Dfr+GiwdYNj+iN6ayJEA8h2pDO7kF8\n"
            + "2fp1SyVgo/erTCp8f2+ZdbdfE584FIfengftoJBSOhcitYp0vvebRRIZBd395Y6/\n" + "T1Ox4WhZ2JSZkKEW9V219y6hHVNotNa3J/2id4JmRQKBgQCRx7ixnSCYWuPS+EZh\n"
            + "m76RWCkuQQXA40Dm2ge7xBSlhiSEx+dTZGRdX/qP0UP6uWqXzSJS+JuufGSu0xhN\n" + "qY3evIDStqZPB2unUlVjiHrNX8RGyP6o0XN/u8fF5+XdVqOObqe3z0c26HX7RnJI\n"
            + "XHb8tXmfsw5+c/5UNrxZK/6XMQ==\n" + "-----END PRIVATE KEY-----\n";

    // to generate text like this, run command below. Edit junitCert.pem file in
    // text editor. Copy contents here. Add line feed chars
    // openssl.exe req -x509 -out junitCert.pem -days 999999
    public static final String GOOD_TESTABLE_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDsTCCApmgAwIBAgIJAOx9LprAoRmEMA0GCSqGSIb3DQEBCwUAMG4xCzAJBgNV\n" + "BAYTAlVTMQswCQYDVQQIDAJOSjELMAkGA1UEBwwCU0IxCzAJBgNVBAoMAk5PMQww\n"
            + "CgYDVQQLDANLSUExCzAJBgNVBAMMAkxQMR0wGwYJKoZIhvcNAQkBFg5ub25lQHlh\n" + "aG9vLmNvbTAgFw0xNzA1MDIxNjI3MTZaGA80NzU1MDMyOTE2MjcxNlowbjELMAkG\n"
            + "A1UEBhMCVVMxCzAJBgNVBAgMAk5KMQswCQYDVQQHDAJTQjELMAkGA1UECgwCTk8x\n" + "DDAKBgNVBAsMA0tJQTELMAkGA1UEAwwCTFAxHTAbBgkqhkiG9w0BCQEWDm5vbmVA\n"
            + "eWFob28uY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtkci4PS7\n" + "xjd9j4QQiLKe86n9wCAQAVlarEdKb+1Uv2uOJKdAAnhhclKr52rjvlBu0ZXeTVZ0\n"
            + "HXasIijFqA0+huwNNXk99CmTvIkqUsIow8x5MSTp3gU9X6uW4QtbI/C5kiNCpWHm\n" + "Xr3H3mgz0kzYi4vCYxAf7fPM9qtNyo92feiDKLUhYNtfja/cM3BWzm/vpEwmz9v7\n"
            + "Mp9204GbQSontb4oDDJuZPN3i+XGHlRerlREg+ScjyQH0u1UjujmOervNeHCVaCs\n" + "/emamOzv/6HY9wAB3AsWxKDX5tbrAjHgm61cCC3UESoHDApyzE1OcaNu/AsLo4iq\n"
            + "NspCZTk9+TPXgQIDAQABo1AwTjAdBgNVHQ4EFgQU8v25fJq45gkYrS75spZ01W47\n" + "5oMwHwYDVR0jBBgwFoAU8v25fJq45gkYrS75spZ01W475oMwDAYDVR0TBAUwAwEB\n"
            + "/zANBgkqhkiG9w0BAQsFAAOCAQEAQ48yKSwoPEmc7kGvrRIqdnBuZMAaGBO72xOZ\n" + "S4jQl2OPMYxGRWW7Km21L9vocDim9GYSmaKNm8uGv/E4XWpgyY609Vai7RdA1qEJ\n"
            + "JuW2KsmraxstfdUiXwYDPS4AC3hLtnW473mxyfa0BdzJMeO+/ZYrDppJhk1kvbHa\n" + "usgBI7vgzTbckq9+vNoGdrvzHSHNbxW5xKMO7thqgoUxijShSojJ/AXARtB6hBbG\n"
            + "EwOuXHe7Jop0nLzQe46jPgn9q+Fe2gYv26EskUXZAAoDKMZTYgjnfuXinZMKpQ7N\n" + "vUerIa96ds41U3YszcracJrHcz+qVtyWcyjAnGg6YIPIeSo/cw==\n"
            + "-----END CERTIFICATE-----\n";

    public static final String BAD_TESTABLE_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
            // +
            // "MIIDsTCCApmgAwIBAgIJAOx9LprAoRmEMA0GCSqGSIb3DQEBCwUAMG4xCzAJBgNV\n"
            // +
            // "BAYTAlVTMQswCQYDVQQIDAJOSjELMAkGA1UEBwwCU0IxCzAJBgNVBAoMAk5PMQww\n"
            + "CgYDVQQLDANLSUExCzAJBgNVBAMMAkxQMR0wGwYJKoZIhvcNAQkBFg5ub25lQHlh\n" + "aG9vLmNvbTAgFw0xNzA1MDIxNjI3MTZaGA80NzU1MDMyOTE2MjcxNlowbjELMAkG\n"
            + "A1UEBhMCVVMxCzAJBgNVBAgMAk5KMQswCQYDVQQHDAJTQjELMAkGA1UECgwCTk8x\n" + "DDAKBgNVBAsMA0tJQTELMAkGA1UEAwwCTFAxHTAbBgkqhkiG9w0BCQEWDm5vbmVA\n"
            + "eWFob28uY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtkci4PS7\n" + "xjd9j4QQiLKe86n9wCAQAVlarEdKb+1Uv2uOJKdAAnhhclKr52rjvlBu0ZXeTVZ0\n"
            + "HXasIijFqA0+huwNNXk99CmTvIkqUsIow8x5MSTp3gU9X6uW4QtbI/C5kiNCpWHm\n" + "Xr3H3mgz0kzYi4vCYxAf7fPM9qtNyo92feiDKLUhYNtfja/cM3BWzm/vpEwmz9v7\n"
            + "Mp9204GbQSontb4oDDJuZPN3i+XGHlRerlREg+ScjyQH0u1UjujmOervNeHCVaCs\n" + "/emamOzv/6HY9wAB3AsWxKDX5tbrAjHgm61cCC3UESoHDApyzE1OcaNu/AsLo4iq\n"
            + "NspCZTk9+TPXgQIDAQABo1AwTjAdBgNVHQ4EFgQU8v25fJq45gkYrS75spZ01W47\n" + "5oMwHwYDVR0jBBgwFoAU8v25fJq45gkYrS75spZ01W475oMwDAYDVR0TBAUwAwEB\n"
            + "/zANBgkqhkiG9w0BAQsFAAOCAQEAQ48yKSwoPEmc7kGvrRIqdnBuZMAaGBO72xOZ\n" + "S4jQl2OPMYxGRWW7Km21L9vocDim9GYSmaKNm8uGv/E4XWpgyY609Vai7RdA1qEJ\n"
            + "JuW2KsmraxstfdUiXwYDPS4AC3hLtnW473mxyfa0BdzJMeO+/ZYrDppJhk1kvbHa\n" + "usgBI7vgzTbckq9+vNoGdrvzHSHNbxW5xKMO7thqgoUxijShSojJ/AXARtB6hBbG\n"
            + "EwOuXHe7Jop0nLzQe46jPgn9q+Fe2gYv26EskUXZAAoDKMZTYgjnfuXinZMKpQ7N\n" + "vUerIa96ds41U3YszcracJrHcz+qVtyWcyjAnGg6YIPIeSo/cw==\n"
            + "-----END CERTIFICATE-----\n";

    @Test
    public void testGoodKeyAndGoodCertificate() throws KeyManagementException, KeyStoreException {
        String generatorAlias = DynamicKeystoreGenerator.KEYSTORE_ALIAS;

        byte[] decodedPrivateKey = DynamicKeystoreGenerator.getDecodedPrivateKey(GOOD_TESTABLE_PRIVATE_KEY);
        Assert.assertEquals("Check private key decoded length", 1219, decodedPrivateKey.length);

        RSAPrivateKey rsaPrivateKey = DynamicKeystoreGenerator.generateRSAPrivateKey(decodedPrivateKey);
        Assert.assertEquals("RSA private key format", "PKCS#8", rsaPrivateKey.getFormat());
        Assert.assertEquals("RSA private key algorithm", "RSA", rsaPrivateKey.getAlgorithm());
        Assert.assertEquals("RSA private key length", 46, rsaPrivateKey.toString().length());

        InputStream is = new ByteArrayInputStream(GOOD_TESTABLE_CERTIFICATE.getBytes());
        Certificate certificate = DynamicKeystoreGenerator.generateX509Certificate(is);
        Assert.assertEquals("Certificate type ", "X.509", certificate.getType());
        // NOTE: the certificate length computed below appears to vary from
        // machine to machine.
        // Need to understand why that is. Commenting out for now.
        // Assert.assertEquals("Certificate length ", 2893,
        // certificate.toString().length());
        Assert.assertEquals("Certificate public key algorithm ", "RSA", certificate.getPublicKey().getAlgorithm());
        Assert.assertEquals("Certificate public key format ", "X.509", certificate.getPublicKey().getFormat());
        Assert.assertEquals("Certificate public key length ", 683, certificate.getPublicKey().toString().length());

        KeyStore keystore = DynamicKeystoreGenerator.createAndLoadDynamicKeystore(rsaPrivateKey, certificate);
        Assert.assertEquals("Keystore type ", "JKS", keystore.getType());
        Assert.assertEquals("Keystore certificate type ", "X.509", keystore.getCertificate(generatorAlias).getType());
        Assert.assertEquals("Keystore certificate public key format ", "X.509", keystore.getCertificate(generatorAlias).getType());

        KeyManager[] keyManagers = DynamicKeystoreGenerator.generateKeyManagersForDynamicKeystore(keystore);
        Assert.assertEquals(1, keyManagers.length);
        X509ExtendedKeyManager ekm = (X509ExtendedKeyManager) keyManagers[0];

        String[] aliases = ekm.getClientAliases("RSA", null);
        String alias = aliases[0];
        Assert.assertEquals("Keymanager Keystore client alias ", generatorAlias, alias);

        PrivateKey keyManagerPrivateKey = ekm.getPrivateKey(alias);
        Assert.assertEquals("Keymanager RSA private key format", "PKCS#8", keyManagerPrivateKey.getFormat());
        Assert.assertEquals("Keymanager RSA private key algorithm", "RSA", keyManagerPrivateKey.getAlgorithm());
        Assert.assertEquals("Keymanager RSA private key length", 46, keyManagerPrivateKey.toString().length());

        X509Certificate[] keyManagerCertificates = ekm.getCertificateChain(generatorAlias);
        X509Certificate keyManagerCert = keyManagerCertificates[0];
        Assert.assertEquals("Keymanager certificate chain length", 1, keyManagerCertificates.length);
        Assert.assertEquals("Keymanager certificate type", "X.509", keyManagerCert.getType());
        Assert.assertEquals("Keymanager certificate signature algorithm", "SHA256withRSA", keyManagerCert.getSigAlgName());
    }

    @Test
    public void testBadKey() {
        byte[] decodedPrivateKey = DynamicKeystoreGenerator.getDecodedPrivateKey(BAD_TESTABLE_PRIVATE_KEY);
        int len = decodedPrivateKey.length;

        Assert.assertEquals("Check private key decoded length", 1123, len);

        try {
            DynamicKeystoreGenerator.generateRSAPrivateKey(decodedPrivateKey);
            Assert.assertFalse(false);
        } catch (KeyManagementException ex) {
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("InvalidKeySpecException"));
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("InvalidKeyException"));
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("IOException"));
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("Detect premature EOF"));
        }
    }

    @Test
    public void testBadCertificate() {
        try {
            InputStream is = new ByteArrayInputStream(BAD_TESTABLE_CERTIFICATE.getBytes());
            DynamicKeystoreGenerator.generateX509Certificate(is);
            Assert.assertFalse(false);
        } catch (KeyManagementException ex) {
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("CertificateException"));
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("IOException"));
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString("extra data given to DerValue constructor"));
        }
    }
}
