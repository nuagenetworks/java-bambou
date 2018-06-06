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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.nuagenetworks.bambou.spring.TestSpringConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestSpringConfig.class, loader = AnnotationConfigContextLoader.class)
public class RestUtilsTest {
	private static final Logger logger = LoggerFactory.getLogger(RestUtilsTest.class);
    @Test
    public void testCreateRestObjectWithContent() throws RestException, JsonProcessingException, IOException {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode restObjNode = nodeFactory.objectNode();
        restObjNode.put("ID", "123");
        restObjNode.put("parentID", "456");
        restObjNode.put("parentType", "MyParentType");
        restObjNode.put("creationDate", "34567");
        restObjNode.put("lastUpdatedDate", "123456");
        restObjNode.put("owner", "MyOwner");
        RestObject restObj = RestUtils.createRestObjectWithContent(RestObject.class, restObjNode);
        Assert.assertEquals("123", restObj.getId());
        Assert.assertEquals("456", restObj.getParentId());
        Assert.assertEquals("MyParentType", restObj.getParentType());
        Assert.assertEquals("34567", restObj.getCreationDate());
        Assert.assertEquals("123456", restObj.getLastUpdatedDate());
        Assert.assertEquals("MyOwner", restObj.getOwner());
    }
    
    @Test
    public void testToString() throws RestException {
    	RestObject object = new RestObject();
    	object.setId("123");
    	object.setParentId("456");
    	object.setParentType("MyParentType");
    	object.setCreationDate("34567");
    	object.setLastUpdatedDate("123456");
    	object.setOwner("MyOwner");
    	
    	String content = RestUtils.toString(object);
    	JsonNode node = RestUtils.toJson(content);
    	
    	Assert.assertEquals("123", node.get("ID").asText());
        Assert.assertEquals("456", node.get("parentID").asText());
        Assert.assertEquals("MyParentType", node.get("parentType").asText());
        Assert.assertEquals("34567", node.get("creationDate").asText());
        Assert.assertEquals("123456", node.get("lastUpdatedDate").asText());
        Assert.assertEquals("MyOwner", node.get("owner").asText());
    }
    
    @Test
    public void testToJson() throws RestException {
    	
    	JsonNode object = RestUtils.toJson("{ \"firstName\": \"John\", \"lastName\": \"Smith\", \"age\": 42}");
    	JsonNode text = RestUtils.toJson("Hello World!");
    	JsonNode empty = RestUtils.toJson("");
    	JsonNode space = RestUtils.toJson(" ");
    	
    	Assert.assertEquals("John", object.get("firstName").asText());
    	Assert.assertEquals("Smith", object.get("lastName").asText());
    	Assert.assertEquals(42, object.get("age").asInt());
    	
    	Assert.assertEquals("Hello World!", text.asText());
    	Assert.assertEquals("", empty.asText());
    	Assert.assertEquals(" ", space.asText());
    }
}
