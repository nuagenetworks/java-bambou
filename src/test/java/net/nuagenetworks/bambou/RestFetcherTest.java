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

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.bambou.RestObject;
import net.nuagenetworks.bambou.RestSession;
import net.nuagenetworks.bambou.spring.TestSpringConfig;
import net.nuagenetworks.bambou.testobj.TestChildObject;
import net.nuagenetworks.bambou.testobj.TestChildObjectFetcher;
import net.nuagenetworks.bambou.testobj.TestObject;
import net.nuagenetworks.bambou.testobj.TestRootObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestFetcherTest {

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
    public void testGet() throws RestException, JsonProcessingException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.get();
        Assert.assertEquals(2, childObjects.size());
    }

    @Test
    public void testGet2() throws RestException, JsonProcessingException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        List<TestChildObject> childObjects = object.getChildObjectFetcher().get();
        Assert.assertEquals(2, childObjects.size());
        Assert.assertEquals(refChildObjects, childObjects);
        Assert.assertEquals(2, object.getChildObjectFetcher().size());
        Assert.assertEquals(refChildObjects, object.getChildObjectFetcher());
    }

    @Test
    public void testGetFirst() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        TestChildObject childObject = fetcher.getFirst();
        Assert.assertEquals("1", childObject.getId());
    }

    @Test
    public void testFetch() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.fetch();
        Assert.assertEquals(3, childObjects.size());
    }

    @Test
    public void testFetchAddedChild() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        fetcher.add(childObject1);
        fetcher.add(childObject2);
        List<TestChildObject> childObjects = fetcher.fetch();

        // Make sure the new child appears in both the fetcher and the return
        // list
        Assert.assertEquals(3, fetcher.size());
        Assert.assertEquals(3, childObjects.size());
        Assert.assertEquals(childObject1, fetcher.get(0));
        Assert.assertEquals(childObject2, fetcher.get(1));
        Assert.assertEquals(childObject3, fetcher.get(2));
    }

    @Test
    public void testFetchRemovedChild() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        fetcher.add(childObject1);
        fetcher.add(childObject2);
        fetcher.add(childObject3);
        List<TestChildObject> childObjects = fetcher.fetch();

        // Make sure the child is removed from both the fetcher and the return
        // list
        Assert.assertEquals(2, fetcher.size());
        Assert.assertEquals(2, childObjects.size());
        Assert.assertEquals(childObject1, fetcher.get(0));
        Assert.assertEquals(childObject2, fetcher.get(1));
    }

    @Test
    public void testFetchWithNoSessionAvailable() {
        try {
            TestObject object = new TestObject();
            TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);

            // Fetch should fail with a specific exception message
            fetcher.fetch();
            Assert.assertTrue(false);
        } catch (RestException ex) {
            Assert.assertEquals("Session not available in current thread", ex.getMessage());
        }
    }

    @Test
    public void testFetchNullContent() throws JsonProcessingException, RestException {
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        refChildObjects.add(null);

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.fetch();
        Assert.assertEquals(0, childObjects.size());
    }

    @Test
    public void testFetchNoContent() throws JsonProcessingException, RestException {
        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, null, null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.fetch();

        // Make sure both the fetcher and return list are empty
        Assert.assertEquals(0, fetcher.size());
        Assert.assertEquals(0, childObjects.size());
    }

    @Test
    public void testFetchNoContent2() throws JsonProcessingException, RestException {
        // Create child objects
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");

        // Start session
        startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK, null, null);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        fetcher.add(childObject1);
        fetcher.add(childObject2);
        fetcher.add(childObject3);
        List<TestChildObject> childObjects = fetcher.fetch();

        // Make sure all the existing objects in the fetcher get removed and
        // that the return list is empty
        Assert.assertEquals(0, fetcher.size());
        Assert.assertEquals(0, childObjects.size());
    }

    @Test
    public void testCount() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);
        TestChildObject childObject4 = new TestChildObject();
        childObject3.setId("4");
        refChildObjects.add(childObject4);

        // Start session
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Nuage-Count", String.valueOf(refChildObjects.size()));
        startSession(restOperations, "object/childobject", HttpMethod.HEAD, HttpStatus.OK, mapper.writeValueAsString(refChildObjects), responseHeaders);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        int count = fetcher.count();
        Assert.assertEquals(refChildObjects.size(), count);
    }

    @Test
    public void testFetchWithHeaders() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        Capture<HttpEntity<?>> capturedHttpEntity = startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(refChildObjects), null);

        String filter = "filter";
        String orderBy = "orderBy";
        String[] groupBy = { "groupBy1", "groupBy2" };
        Integer pageSize = 2;
        Integer page = 1;

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.fetch(filter, orderBy, groupBy, page, pageSize, null, true);

        Assert.assertEquals(3, childObjects.size());
        HttpHeaders headers = capturedHttpEntity.getValue().getHeaders();
        Assert.assertEquals(filter, headers.get("X-Nuage-Filter").get(0));
        Assert.assertEquals(orderBy, headers.get("X-Nuage-OrderBy").get(0));
        Assert.assertEquals(String.valueOf(page), headers.get("X-Nuage-Page").get(0));
        Assert.assertEquals(String.valueOf(pageSize), headers.get("X-Nuage-PageSize").get(0));
        Assert.assertEquals(String.valueOf(true), headers.get("X-Nuage-GroupBy").get(0));
        Assert.assertEquals(groupBy[0] + "," + groupBy[1], headers.get("X-Nuage-Attributes").get(0));
    }

    @Test
    public void testGetWithHeaders() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        Capture<HttpEntity<?>> capturedHttpEntity = startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(refChildObjects), null);

        String filter = "filter";
        String orderBy = "orderBy";
        String[] groupBy = { "groupBy1", "groupBy2" };
        Integer pageSize = 2;
        Integer page = 1;

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.get(filter, orderBy, groupBy, page, pageSize, null, true);

        Assert.assertEquals(3, childObjects.size());
        HttpHeaders headers = capturedHttpEntity.getValue().getHeaders();
        Assert.assertEquals(filter, headers.get("X-Nuage-Filter").get(0));
        Assert.assertEquals(orderBy, headers.get("X-Nuage-OrderBy").get(0));
        Assert.assertEquals(String.valueOf(page), headers.get("X-Nuage-Page").get(0));
        Assert.assertEquals(String.valueOf(pageSize), headers.get("X-Nuage-PageSize").get(0));
        Assert.assertEquals(String.valueOf(true), headers.get("X-Nuage-GroupBy").get(0));
        Assert.assertEquals(groupBy[0] + "," + groupBy[1], headers.get("X-Nuage-Attributes").get(0));
    }

    @Test
    public void testGetFirstWithHeaders() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        Capture<HttpEntity<?>> capturedHttpEntity = startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(refChildObjects), null);

        String filter = "filter";
        String orderBy = "orderBy";
        String[] groupBy = { "groupBy1", "groupBy2" };
        Integer pageSize = 2;
        Integer page = 1;

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        TestChildObject childObject = fetcher.getFirst(filter, orderBy, groupBy, page, pageSize, null, true);

        Assert.assertEquals("1", childObject.getId());
        HttpHeaders headers = capturedHttpEntity.getValue().getHeaders();
        Assert.assertEquals(filter, headers.get("X-Nuage-Filter").get(0));
        Assert.assertEquals(orderBy, headers.get("X-Nuage-OrderBy").get(0));
        Assert.assertEquals(String.valueOf(page), headers.get("X-Nuage-Page").get(0));
        Assert.assertEquals(String.valueOf(pageSize), headers.get("X-Nuage-PageSize").get(0));
        Assert.assertEquals(String.valueOf(true), headers.get("X-Nuage-GroupBy").get(0));
        Assert.assertEquals(groupBy[0] + "," + groupBy[1], headers.get("X-Nuage-Attributes").get(0));
    }

    @Test
    public void testCountWithHeaders() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);
        TestChildObject childObject4 = new TestChildObject();
        childObject3.setId("4");
        refChildObjects.add(childObject4);

        // Start session
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Nuage-Count", String.valueOf(refChildObjects.size()));
        Capture<HttpEntity<?>> capturedHttpEntity = startSession(restOperations, "object/childobject", HttpMethod.HEAD, HttpStatus.OK,
                mapper.writeValueAsString(refChildObjects), responseHeaders);

        String filter = "filter";
        String orderBy = "orderBy";
        String[] groupBy = { "groupBy1", "groupBy2" };
        Integer pageSize = 2;
        Integer page = 1;

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        int count = fetcher.count(filter, orderBy, groupBy, page, pageSize, null, true);

        Assert.assertEquals(refChildObjects.size(), count);
        HttpHeaders headers = capturedHttpEntity.getValue().getHeaders();
        Assert.assertEquals(filter, headers.get("X-Nuage-Filter").get(0));
        Assert.assertEquals(orderBy, headers.get("X-Nuage-OrderBy").get(0));
        Assert.assertEquals(String.valueOf(page), headers.get("X-Nuage-Page").get(0));
        Assert.assertEquals(String.valueOf(pageSize), headers.get("X-Nuage-PageSize").get(0));
        Assert.assertEquals(String.valueOf(true), headers.get("X-Nuage-GroupBy").get(0));
        Assert.assertEquals(groupBy[0] + "," + groupBy[1], headers.get("X-Nuage-Attributes").get(0));
    }

    @Test
    public void testFetchWith401Response() throws JsonProcessingException, RestException {
        // Create child objects
        List<RestObject> refChildObjects = new ArrayList<RestObject>();
        TestChildObject childObject1 = new TestChildObject();
        childObject1.setId("1");
        refChildObjects.add(childObject1);
        TestChildObject childObject2 = new TestChildObject();
        childObject2.setId("2");
        refChildObjects.add(childObject2);
        TestChildObject childObject3 = new TestChildObject();
        childObject3.setId("3");
        refChildObjects.add(childObject3);

        // Start session
        Capture<HttpEntity<?>> capturedHttpEntity = startSession(restOperations, "object/childobject", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(refChildObjects), null, true);

        TestObject object = new TestObject();
        TestChildObjectFetcher fetcher = new TestChildObjectFetcher(object);
        List<TestChildObject> childObjects = fetcher.fetch();
        Assert.assertEquals("XREST bWFydGluOjI=", capturedHttpEntity.getValue().getHeaders().get("Authorization").get(0));
        Assert.assertEquals(3, childObjects.size());
    }

    private Capture<HttpEntity<?>> startSession(RestOperations restOperations, String urlSuffix, HttpMethod method, HttpStatus responseStatus,
            String responseString, HttpHeaders responseHeaders) throws RestException {
        return startSession(restOperations, urlSuffix, method, responseStatus, responseString, responseHeaders, false);
    }

    private Capture<HttpEntity<?>> startSession(RestOperations restOperations, String urlSuffix, HttpMethod method, HttpStatus responseStatus,
            String responseString, HttpHeaders responseHeaders, boolean simulate401Response) throws RestException {
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
                EasyMock.anyObject(HttpEntity.class), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>("[{ \"APIKey\": \"1\" }]".getBytes(), HttpStatus.OK));
        if (simulate401Response) {
            EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/" + urlSuffix), EasyMock.eq(method),
                    EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>("".getBytes(), HttpStatus.UNAUTHORIZED));
            EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/root"), EasyMock.eq(HttpMethod.GET),
                    EasyMock.anyObject(HttpEntity.class), EasyMock.eq(byte[].class)))
                    .andReturn(new ResponseEntity<byte[]>("[{ \"APIKey\": \"2\" }]".getBytes(), HttpStatus.OK));
        }
        EasyMock.expect(restOperations.exchange(EasyMock.eq(apiUrl + '/' + apiPrefix + "/v2_1/" + urlSuffix), EasyMock.eq(method),
                EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>(responseString==null?null:responseString.getBytes(), responseHeaders, responseStatus));
        EasyMock.replay(restOperations);

        // Start REST session
        session.setUsername(username);
        session.setPassword(password);
        session.setEnterprise(enterprise);
        session.setApiUrl(apiUrl);
        session.setApiPrefix(apiPrefix);
        session.setVersion(version);
        session.start();

        return capturedHttpEntity;
    }
}
