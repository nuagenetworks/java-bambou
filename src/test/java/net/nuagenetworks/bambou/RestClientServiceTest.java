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

import org.easymock.Capture;
import org.easymock.EasyMock;
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
import org.springframework.web.client.RestOperations;

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.bambou.service.RestClientService;
import net.nuagenetworks.bambou.spring.TestSpringConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestClientServiceTest {

    @Autowired
    private RestClientService restService;

    @Autowired
    private RestOperations restOperations;

    @Test
    public void testSendRequest() throws RestException {
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>(HttpStatus.OK));
        EasyMock.replay(restOperations);

        ResponseEntity<String> response = restService.sendRequest(method, url, null, content, String.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        EasyMock.verify(restOperations);
    }

    @Test
    public void testErrorResponseNoContent() throws RestException {
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>("".getBytes(), HttpStatus.NOT_FOUND));
        EasyMock.replay(restOperations);

        try {
            restService.sendRequest(method, url, null, content, String.class);
            Assert.assertFalse(false);
        } catch (RestStatusCodeException ex) {
            // Expect exception
            Assert.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            Assert.assertEquals("404/Not Found", ex.getMessage());
            Assert.assertNull(ex.getInternalErrorCode());
        }

        EasyMock.verify(restOperations);
    }

    @Test
    public void testErrorResponseWithInternalErrorCode() throws RestException {
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>("{ \"internalErrorCode\": \"1001\" }".getBytes(), HttpStatus.NOT_FOUND));
        EasyMock.replay(restOperations);

        try {
            restService.sendRequest(method, url, null, content, String.class);
            Assert.assertFalse(false);
        } catch (RestStatusCodeException ex) {
            // Expect exception
            Assert.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            Assert.assertEquals("404 Not Found", ex.getMessage());
            Assert.assertEquals("1001", ex.getInternalErrorCode());
        }

        EasyMock.verify(restOperations);
    }

    @Test
    public void testErrorResponseWithErrorMessage() throws RestException {
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>("{\"errors\": [ { \"descriptions\": [ { \"description\": \"Error message\" } ] } ] }".getBytes(),
                        HttpStatus.NOT_FOUND));
        EasyMock.replay(restOperations);

        try {
            restService.sendRequest(method, url, null, content, String.class);
            Assert.assertFalse(false);
        } catch (RestStatusCodeException ex) {
            // Expect exception
            Assert.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            Assert.assertEquals("Error message", ex.getMessage());
            Assert.assertNull(ex.getInternalErrorCode());
        }

        EasyMock.verify(restOperations);
    }

    @Test
    public void testErrorResponseWithErrorMessageAndProperty() throws RestException {
        HttpMethod method = HttpMethod.GET;
        String url = "http://vsd";
        String content = "test";

        EasyMock.reset(restOperations);
        Capture<HttpEntity<?>> capturedHttpEntity = EasyMock.newCapture();
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url), EasyMock.eq(method), EasyMock.capture(capturedHttpEntity), EasyMock.eq(byte[].class)))
                .andReturn(new ResponseEntity<byte[]>(
                        "{\"errors\": [ { \"property\": \"My Property\", \"descriptions\": [ { \"description\": \"Error message\" } ] } ] }".getBytes(),
                        HttpStatus.NOT_FOUND));
        EasyMock.replay(restOperations);

        try {
            restService.sendRequest(method, url, null, content, String.class);
            Assert.assertFalse(false);
        } catch (RestStatusCodeException ex) {
            // Expect exception
            Assert.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            Assert.assertEquals("My Property: Error message", ex.getMessage());
            Assert.assertNull(ex.getInternalErrorCode());
        }

        EasyMock.verify(restOperations);
    }
}
