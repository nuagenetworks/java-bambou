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

import java.io.IOException;
import java.util.Arrays;

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
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.RestSession;
import net.nuagenetworks.bambou.model.Events;
import net.nuagenetworks.bambou.spring.TestSpringConfig;
import net.nuagenetworks.bambou.testobj.TestRootObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestPushCenterTest {

    @Autowired
    private RestOperations restOperations;

    @Autowired
    private RestSession<TestRootObject> session;

    private ObjectMapper mapper = new ObjectMapper();
    private int listenerInvocationCount = 0;

    @After
    public void resetTest() {
        session.reset();
    }

    @Test
    public void testNewPushCenter() {
        String url = "http://vsd";

        RestPushCenterLongPoll pushCenter = new RestPushCenterLongPoll(session);
        pushCenter.setUrl(url);
        Assert.assertEquals(url, pushCenter.getUrl());
        Assert.assertFalse(pushCenter.isRunning());
    }

    @Test
    public void testStartPushCenter() throws InterruptedException {
        String url = "http://vsd";

        EasyMock.reset(restOperations);
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>("{}".getBytes(), HttpStatus.OK)).atLeastOnce();
        EasyMock.replay(restOperations);

        RestPushCenterLongPoll pushCenter = new RestPushCenterLongPoll(session);
        pushCenter.setUrl(url);
        pushCenter.start();

        Thread.sleep(500);
        Assert.assertTrue(pushCenter.isRunning());
        pushCenter.stop();
        Assert.assertFalse(pushCenter.isRunning());

        EasyMock.verify(restOperations);
    }

    @Test
    public void testPushCenterListener() throws InterruptedException, JsonProcessingException, IOException {
        String url = "http://vsd";

        Events events = new Events();
        JsonNode event = mapper.readTree("{\"k1\":\"v1\"}");
        events.setEvents(Arrays.asList(event));

        EasyMock.reset(restOperations);
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(mapper.writeValueAsString(events).getBytes(), HttpStatus.OK)).atLeastOnce();
        EasyMock.replay(restOperations);

        listenerInvocationCount = 0;
        RestPushCenterListener listener = new RestPushCenterListener() {
            @Override
            public void onEvent(JsonNode event) {
                listenerInvocationCount++;
            }
        };
        RestPushCenterLongPoll pushCenter = new RestPushCenterLongPoll(session);
        pushCenter.setUrl(url);
        pushCenter.addListener(listener);
        pushCenter.start();

        Thread.sleep(500);
        Assert.assertTrue(pushCenter.isRunning());
        Assert.assertTrue(listenerInvocationCount > 0);

        pushCenter.stop();
        pushCenter.removeListener(listener);

        Assert.assertFalse(pushCenter.isRunning());

        EasyMock.verify(restOperations);
    }

    @Test
    public void testPushCenterWith400Response() throws InterruptedException, JsonProcessingException, IOException {
        String url = "http://vsd";

        Events events = new Events();
        JsonNode event = mapper.readTree("{\"k1\":\"v1\"}");
        events.setEvents(Arrays.asList(event));
        events.setUuid("1");

        EasyMock.reset(restOperations);
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(mapper.writeValueAsString(events).getBytes(), HttpStatus.OK));
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events?uuid=1"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(mapper.writeValueAsString(events).getBytes(), HttpStatus.OK));
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events?uuid=1"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>("".getBytes(), HttpStatus.BAD_REQUEST));
        EasyMock.expect(restOperations.exchange(EasyMock.eq(url + "/events"), EasyMock.eq(HttpMethod.GET), EasyMock.anyObject(HttpEntity.class),
                EasyMock.eq(byte[].class))).andReturn(new ResponseEntity<byte[]>(mapper.writeValueAsString(events).getBytes(), HttpStatus.OK));
        EasyMock.replay(restOperations);

        RestPushCenterLongPoll pushCenter = new RestPushCenterLongPoll(session);
        pushCenter.setUrl(url);
        pushCenter.start();

        Thread.sleep(500);
        Assert.assertTrue(pushCenter.isRunning());

        pushCenter.stop();

        Assert.assertFalse(pushCenter.isRunning());

        EasyMock.verify(restOperations);
    }
}
