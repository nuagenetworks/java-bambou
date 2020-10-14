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
package net.nuagenetworks.bambou.service;

import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.HttpHost;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestClientTemplate extends RestTemplate {

    private static final int DEFAULT_SOCKET_TIMEOUT_IN_MS = 60 * 1000;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT_IN_MS;
    private int proxyPort = 0;
    private String proxyHost = null;
    
    private boolean certValidationDisabled = false;

    CloseableHttpClient httpClient;

    public RestClientTemplate() {
        super();

        this.httpClient = this.createClient(); 
        this.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT_IN_MS);
        ResponseErrorHandlerImpl responseErrorHandler = new ResponseErrorHandlerImpl();
        setErrorHandler(responseErrorHandler);
    }

    public HttpComponentsClientHttpRequestFactory getRequestFactory() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(this.httpClient);

        return requestFactory;
    }

    public void disableCertificateValidation() {
        this.certValidationDisabled = true;
        this.httpClient = this.createClient();
    }

    public void setSocketTimeout(int socketTimeout) {
        if (socketTimeout > 0) {
            // Debug
            logger.debug("Using socket timeout for REST connection: " + socketTimeout);
            this.socketTimeout = socketTimeout;
            this.httpClient = this.createClient();
        }
    }

    public void setHttpProxy(String host, int port) {
        if (host != null && !host.isEmpty()) {
            // Debug
            logger.debug("Using HTTP proxy for REST connection: " + host + ":" + port);
            
            this.proxyHost = host;
            this.proxyPort = port;

            this.httpClient = this.createClient();
        }
    }

    private CloseableHttpClient createClient() {
            SSLContextBuilder builder = new SSLContextBuilder();
            SSLConnectionSocketFactory factory;
            HttpClientBuilder clientBuilder = HttpClients.custom();

            try {
                if (this.certValidationDisabled) {
        
                    builder.loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            return true;
                        }
                    });
                }
                factory = new SSLConnectionSocketFactory(builder.build());

                RequestConfig config = RequestConfig.custom()
                  .setConnectTimeout(this.socketTimeout)
                  .setConnectionRequestTimeout(this.socketTimeout)
                  .setSocketTimeout(this.socketTimeout).build();


                if (this.proxyHost != null && this.proxyPort != 0) {
                    HttpHost httpProxy = new HttpHost(this.proxyHost, this.proxyPort);
                    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(httpProxy);
                    clientBuilder.setRoutePlanner(routePlanner);
                }
    
                return clientBuilder 
                  .setDefaultRequestConfig(config)
                  .setSSLSocketFactory(factory).build();
            } catch (NoSuchAlgorithmException e) {
                return null;
            } catch (KeyManagementException e) {
                return null;
            } catch (KeyStoreException e) {
                return null;
            }
    }

}
