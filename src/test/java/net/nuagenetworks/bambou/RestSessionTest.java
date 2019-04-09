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

import java.util.Arrays;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.spring.TestSpringConfig;
import net.nuagenetworks.bambou.testobj.TestRootObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestSessionTest {

    @Autowired
    private RestOperations restOperations;

    @Autowired
    private RestSession<TestRootObject> session;

    private ObjectMapper mapper = new ObjectMapper();

    @After
    public void resetTest() {
        session.reset();
    }

    @Test
    public void testNoSessionAvailable() {
        Assert.assertNull(RestSession.getCurrentSession());
    }

    @Test
    public void testStartSession() throws RestException, RestClientException, JsonProcessingException {
        RestSession<TestRootObject> session = startSession();
        Assert.assertEquals(session, RestSession.getCurrentSession());
        Assert.assertEquals("12345", session.getRootObject().getApiKey());
        Assert.assertEquals("TestValue", session.getRootObject().getTestAttr());
    }

    @Test
    public void testStartSessionWithWrongCredentials() throws RestException, RestClientException, JsonProcessingException {
        try {
            // Simulate a login with wrong credentials by sending a
            // 401/Unauthorized response back to the client
            startSession(HttpStatus.UNAUTHORIZED, "");

            // We should get an exception
            Assert.assertTrue(false);
        } catch (RestStatusCodeException ex) {
            // Check for expected exception message
            Assert.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Test
    public void testResetSession() throws RestException, RestClientException, JsonProcessingException {
        RestSession<?> session = startSession();
        Assert.assertEquals(session, RestSession.getCurrentSession());

        session.reset();
        Assert.assertEquals(null, RestSession.getCurrentSession());
    }

    @Test
    public void testSendRequest() throws RestException {
        String username = "martin";
        String password = "martin";
        String enterprise = "martin";
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        session.setUsername(username);
        session.setPassword(password);
        session.setEnterprise(enterprise);

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>(HttpStatus.OK));
        EasyMock.replay(restOperations);

        ResponseEntity<String> response = session.sendRequestWithRetry(method, url, null, null, content, String.class);

        HttpHeaders requestHeaders = capturedHttpEntity.getValue().getHeaders();
        Assert.assertEquals(3, requestHeaders.size());
        Assert.assertEquals("application/json", requestHeaders.getFirst("Content-Type"));
        Assert.assertEquals(enterprise, requestHeaders.getFirst("X-Nuage-Organization"));
        Assert.assertEquals("XREST bWFydGluOm1hcnRpbg==", requestHeaders.getFirst("Authorization"));

        Assert.assertNotNull(response);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        EasyMock.verify(restOperations);
    }

    private RestSession<TestRootObject> startSession() throws RestException, RestClientException, JsonProcessingException {
        TestRootObject rootObject = new TestRootObject();
        rootObject.setApiKey("12345");
        rootObject.setTestAttr("TestValue");

        return startSession(HttpStatus.OK, mapper.writeValueAsString(Arrays.asList(rootObject)));
    }

    private RestSession<TestRootObject> startSession(HttpStatus responseStatusCode, String responseContent)
            throws RestException, RestClientException, JsonProcessingException {
        String username = "martin";
        String password = "martin";
        String enterprise = "martin";
        String apiUrl = "http://vsd";
        String apiPrefix = "api";
        double version = 2.1;

        EasyMock.reset(restOperations);
        EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/root"), EasyMock.eq(HttpMethod.GET),
                EasyMock.anyObject(HttpEntity.class), EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(responseContent.getBytes(), responseStatusCode));
        EasyMock.replay(restOperations);

        session.setUsername(username);
        session.setPassword(password);
        session.setEnterprise(enterprise);
        session.setApiUrl(apiUrl);
        session.setApiPrefix(apiPrefix);
        session.setVersion(version);
        session.start();

        EasyMock.verify(restOperations);

        return session;
    }
}
