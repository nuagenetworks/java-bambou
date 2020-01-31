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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import net.nuagenetworks.bambou.jms.RestPushCenterJmsActiveMQ;
import net.nuagenetworks.bambou.jms.RestPushCenterJmsDirectActiveMQ;
import net.nuagenetworks.bambou.jms.RestPushCenterJmsDirectJBoss;
import net.nuagenetworks.bambou.jms.RestPushCenterJmsJBoss;
import net.nuagenetworks.bambou.operation.RestSessionOperations;
import net.nuagenetworks.bambou.service.RestClientService;
import net.nuagenetworks.bambou.annotation.RestEntity;
import net.nuagenetworks.bambou.util.BambouUtils;

public class RestSession<R extends RestRootObject> implements RestSessionOperations {

    private static final String ORGANIZATION_HEADER = "X-Nuage-Organization";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final Logger logger = LoggerFactory.getLogger(RestSession.class);
    private static final ThreadLocal<RestSession<?>> currentSession = new ThreadLocal<RestSession<?>>();

    @Autowired
    private RestClientService restClientService;

    private String username;
    private String password;
    private String enterprise;
    private String apiUrl;
    private String apiPrefix;
    private String certificate;
    private String privateKey;
    private double version;
    private String apiKey;
    private Class<R> restRootObjClass;
    private R restRootObj;

    public RestSession(Class<R> restRootObjClass) {
        this.restRootObjClass = restRootObjClass;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(String enterprise) {
        this.enterprise = enterprise;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    protected void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    protected static RestSession<?> getCurrentSession() {
        return currentSession.get();
    }

    public R getRootObject() {
        return restRootObj;
    }

    public String getVSDVersion() {
        try {
            ResponseEntity<JsonNode> resp = restClientService.sendRequest(HttpMethod.GET,this.apiUrl+"/nuage",null,null,JsonNode.class);
            JsonNode node = resp.getBody();
            return node.get("vsdBuild").textValue();
        } catch (Exception e) {
            logger.error("Error: ",e);
        }
        return null;
    }

    @Override
    public void start() throws RestException {
        currentSession.set(this);
        restClientService.prepareSSLAuthentication(certificate, privateKey);
        authenticate();
    }

    @Override
    public void reset() {
        restRootObj = null;
        apiKey = null;
        currentSession.set(null);
    }

    /**
     * @deprecated use {@link #createPushCenter() or @link
     *             #createPushCenter(RestPushCenterType)} instead.
     */
    @Deprecated
    public RestPushCenter getPushCenter() {
        return createPushCenter(RestPushCenterType.LONG_POLL);
    }

    public RestPushCenter createPushCenter() {
        // PushCenter implementation defaults to JMS from now on
        return createPushCenter(RestPushCenterType.JMS);
    }

    public RestPushCenter createPushCenter(RestPushCenterType pushCenterType) {
        RestPushCenter pushCenter;

        if (pushCenterType == RestPushCenterType.JMS) {
            if (version >= 5.0) {
                // VSD version 5.0.x uses a different JMS client than previous
                // releases
                RestPushCenterJmsActiveMQ pushCenterJmsActiveMQ = new RestPushCenterJmsActiveMQ();
                if (username != null && password != null && enterprise != null) {
                    String jmsUser = username + "@" + enterprise;
                    String jmsPassword = password;
                    pushCenterJmsActiveMQ.setUser(jmsUser);
                    pushCenterJmsActiveMQ.setPassword(jmsPassword);
                }
                pushCenter = pushCenterJmsActiveMQ;
            } else {
                pushCenter = new RestPushCenterJmsJBoss();
            }
        } else if (pushCenterType == RestPushCenterType.JMS_NO_JNDI) {
            if (version >= 5.0) {
                // VSD version 5.0.x uses a different JMS client than previous
                // releases
                RestPushCenterJmsDirectActiveMQ pushCenterJmsActiveMQ = new RestPushCenterJmsDirectActiveMQ();
                if (username != null && password != null && enterprise != null) {
                    String jmsUser = username + "@" + enterprise;
                    String jmsPassword = password;
                    pushCenterJmsActiveMQ.setUser(jmsUser);
                    pushCenterJmsActiveMQ.setPassword(jmsPassword);
                }
                pushCenter = pushCenterJmsActiveMQ;
            } else {
                pushCenter = new RestPushCenterJmsDirectJBoss();
            }
        } else {
            pushCenter = new RestPushCenterLongPoll(this);
        }

        String url = getRestBaseUrl();
        pushCenter.setUrl(url);

        return pushCenter;
    }

    @Override
    public <T extends RestObject> BulkResponse<T> bulkSave(List<T> objList) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(1);
   
        ResponseEntity<BulkResponse> response = this.sendRequestWithRetry(HttpMethod.PUT, getResourceUrlForParentType(objList.get(0).getClass()), params, null, objList, BulkResponse.class);
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            return response.getBody();
            // Success
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public <T extends RestObject> BulkResponse<T> bulkDelete(List<T> objList) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(1);
  
        for (T item : objList) {
            params += "&id="+item.getId();
        } 
        ResponseEntity<BulkResponse> response = this.sendRequestWithRetry(HttpMethod.DELETE, getResourceUrlForParentType(objList.get(0).getClass()), params, null, null, BulkResponse.class);
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            return response.getBody();
            // Success
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public <T extends RestObject> BulkResponse<T> createChildren(RestObject parent, List<T> children) throws RestException {
        return parent.createChildren(this,children);
    }

    @Override
    public void fetch(RestObject restObj) throws RestException {
        restObj.fetch(this);
    }

    @Override
    public void save(RestObject restObj) throws RestException {
        restObj.save(this);
    }

    @Override
    public void save(RestObject restObj, Integer responseChoice) throws RestException {
        restObj.save(this, responseChoice);
    }

    @Override
    public void delete(RestObject restObj) throws RestException {
        restObj.delete(this);
    }

    @Override
    public void delete(RestObject restObj, Integer responseChoice) throws RestException {
        restObj.delete(this, responseChoice);
    }

    @Override
    public void createChild(RestObject restObj, RestObject childRestObj) throws RestException {
        restObj.createChild(this, childRestObj);
    }

    @Override
    public void createChild(RestObject restObj, RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException {
        restObj.createChild(this, childRestObj, responseChoice, commit);
    }

    @Override
    public void instantiateChild(RestObject restObj, RestObject childRestObj, RestObject fromTemplate) throws RestException {
        restObj.instantiateChild(this, childRestObj, fromTemplate);
    }

    @Override
    public void instantiateChild(RestObject restObj, RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit)
            throws RestException {
        restObj.instantiateChild(this, childRestObj, fromTemplate, responseChoice, commit);
    }

    @Override
    public void assign(RestObject restObj, List<? extends RestObject> childRestObjs) throws RestException {
        restObj.assign(this, childRestObjs);
    }

    @Override
    public void assign(RestObject restObj, List<? extends RestObject> childRestObjs, boolean commit) throws RestException {
        restObj.assign(this, childRestObjs, commit);
    }
    @Override
    public void assign(RestObject restObj, List<? extends RestObject> childRestObjs,Integer responseChoice, boolean commit) throws RestException {
        restObj.assign(this, childRestObjs, responseChoice, commit);
    }

    @Override
    public <T extends RestObject> List<T> get(RestFetcher<T> fetcher) throws RestException {
        return get(fetcher, null, null, null, null, null, null, true);
    }

    @Override
    public <T extends RestObject> List<T> fetch(RestFetcher<T> fetcher) throws RestException {
        return fetch(fetcher, null, null, null, null, null, null, true);
    }

    @Override
    public <T extends RestObject> T getFirst(RestFetcher<T> fetcher) throws RestException {
        return getFirst(fetcher, null, null, null, null, null, null, true);
    }

    @Override
    public <T extends RestObject> int count(RestFetcher<T> fetcher) throws RestException {
        return count(fetcher, null, null, null, null, null, null, true);
    }

    @Override
    public <T extends RestObject> List<T> get(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException {
        return fetcher.get(this, filter, orderBy, groupBy, page, pageSize, queryParameters, commit);
    }

    @Override
    public <T extends RestObject> List<T> fetch(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException {
        return fetcher.fetch(this, filter, orderBy, groupBy, page, pageSize, queryParameters, commit);
    }

    @Override
    public <T extends RestObject> T getFirst(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException {
        return fetcher.getFirst(this, filter, orderBy, groupBy, page, pageSize, queryParameters, commit);
    }

    @Override
    public <T extends RestObject> int count(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException {
        return fetcher.count(this, filter, orderBy, groupBy, page, pageSize, queryParameters, commit);
    }

    protected <T, U> ResponseEntity<T> sendRequestWithRetry(HttpMethod method, String url, String params, HttpHeaders headers, U requestObj,
            Class<T> responseType) throws RestException {
        if (params != null) {
            url += (url.indexOf('?') >= 0) ? ";" + params : "?" + params;
        }

        if (headers == null) {
            headers = new HttpHeaders();
        }

        headers.set(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
        headers.set(ORGANIZATION_HEADER, getEnterprise());
        headers.set(HttpHeaders.AUTHORIZATION, getAuthenticationHeader());

        logger.info(url);
        try {
            return restClientService.sendRequest(method, url, headers, requestObj, responseType);
        } catch (RestStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Debug
                logger.info("HTTP 401/Unauthorized response received");

                // Make sure we are not already re-authenticating
                // in order to avoid infinite recursion
                if (!(method == HttpMethod.GET && url.equals(restRootObj.getResourceUrl(this)))) {
                    // Re-authenticate the session and try to send the same
                    // request again. A new API key might get issued as a result
                    reset();
                    authenticate();
        	    currentSession.set(this);

                    // Update authorization header with new API key
                    headers.set(HttpHeaders.AUTHORIZATION, getAuthenticationHeader());

                    return restClientService.sendRequest(method, url, headers, requestObj, responseType);
                } else {
                    throw ex;
                }
            } else {
                throw ex;
            }
        }
    }

    protected String getRestBaseUrl() {
        int roundedVersion = (int)version;
        return String.format("%s/%s/v%s", apiUrl, apiPrefix, String.valueOf(roundedVersion));
    }

    protected String getResourceUrlForParentType(Class<?> parentRestObjClass) {
        RestEntity annotation = parentRestObjClass.getAnnotation(RestEntity.class);
        String parentResourceName = annotation.resourceName();

        return String.format("%s/%s", getRestBaseUrl(), parentResourceName);
    }

    private synchronized void authenticate() throws RestException {
        // Create the root object if needed
        if (restRootObj == null) {
            restRootObj = createRootObject();
            fetch(restRootObj);
        }

        // Copy the API key from the root object
        apiKey = restRootObj.getApiKey();

        // Debug
        logger.debug("Started session with username: " + username + " in enterprise: " + enterprise);
    }

    private R createRootObject() throws RestException {
        try {
            return restRootObjClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RestException(ex);
        }
    }

    private String getAuthenticationHeader() {
        if (apiKey != null) {
            return String.format("XREST %s", Base64.encodeBase64String(String.format("%s:%s", username, apiKey).getBytes()));
        } else {
            return String.format("XREST %s", Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes()));
        }
    }

    @Override
    public String toString() {
        return "RestSession [restClientService=" + restClientService + ", username=" + username + ", password=" + password + ", enterprise=" + enterprise
                + ", apiUrl=" + apiUrl + ", apiPrefix=" + apiPrefix + ", certificate=" + certificate + ", privateKey=" + privateKey + ", version=" + version
                + ", apiKey=" + apiKey + ", restRootObjClass=" + restRootObjClass + ", restRootObj=" + restRootObj + "]";
    }
}
