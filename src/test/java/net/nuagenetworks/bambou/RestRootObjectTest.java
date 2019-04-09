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

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.bambou.RestRootObject;
import net.nuagenetworks.bambou.RestSession;
import net.nuagenetworks.bambou.spring.TestSpringConfig;
import net.nuagenetworks.bambou.testobj.TestRootObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestRootObjectTest {

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
    public void testNewRootObject() throws RestException {
        String username = "martin";
        String password = "martin";
        String apiKey = "12345";

        RestRootObject rootObject = new TestRootObject();
        rootObject.setUserName(username);
        rootObject.setPassword(password);
        rootObject.setApiKey(apiKey);

        Assert.assertEquals(apiKey, rootObject.getApiKey());
        Assert.assertEquals(username, rootObject.getUserName());
        Assert.assertEquals(password, rootObject.getPassword());
    }

    @Test
    public void testFetchObject() throws RestException, RestClientException, JsonProcessingException {
        String id = "12345";

        // Create response object to fetch REST call
        TestRootObject refRootObject = new TestRootObject();
        refRootObject.setId(id);
        refRootObject.setApiKey("12345");

        // Start session
        startSession(restOperations, "root", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(Arrays.asList(refRootObject)));

        // Fetch root object
        RestRootObject rootObject = new TestRootObject();
        rootObject.fetch();

        // Expect some object properties to be set
        Assert.assertEquals(id, rootObject.getId());
        Assert.assertEquals(refRootObject.getApiKey(), rootObject.getApiKey());

        // Verify mock calls
        EasyMock.verify(restOperations);
    }

    @Test
    public void testSaveObject() throws RestException, RestClientException, JsonProcessingException {
        // Start session
        startSession(restOperations, "root", HttpMethod.PUT, HttpStatus.NO_CONTENT, "[]");

        // Save root object
        RestRootObject rootObject = new TestRootObject();
        rootObject.save();

        // Verify mock calls
        EasyMock.verify(restOperations);
    }

    private RestSession<TestRootObject> startSession(RestOperations restOperations, String urlSuffix, HttpMethod method, HttpStatus responseStatus,
            String responseString) throws RestException {
        String username = "martin";
        String password = "martin";
        String enterprise = "martin";
        String apiUrl = "http://vsd";
        String apiPrefix = "api";
        double version = 2.1;

        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();

        // Expected REST calls
        EasyMock.reset(restOperations);
        EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/root"), EasyMock.eq(HttpMethod.GET),
                EasyMock.anyObject(HttpEntity.class), EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>("[{}]".getBytes(), HttpStatus.OK));
        EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/" + urlSuffix), EasyMock.eq(method),
                EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(responseString.getBytes(), responseStatus));
        EasyMock.replay(restOperations);

        // Start REST session
        session.setUsername(username);
        session.setPassword(password);
        session.setEnterprise(enterprise);
        session.setApiUrl(apiUrl);
        session.setApiPrefix(apiPrefix);
        session.setVersion(version);
        session.start();

        return session;
    }
}
