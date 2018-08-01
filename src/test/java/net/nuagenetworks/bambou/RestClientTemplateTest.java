package net.nuagenetworks.bambou;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.nuagenetworks.bambou.service.RestClientTemplate;

public class RestClientTemplateTest {
	ClientHttpRequestFactory factory;
	ClientHttpRequest client;
	
	private RestClientTemplate tested = new RestClientTemplate();
	private ObjectMapper mapper = new ObjectMapper();
	
	@Before
	public void setUp() throws IOException {
		factory = EasyMock.createMock(ClientHttpRequestFactory.class);
		client = EasyMock.createMock(ClientHttpRequest.class);
		EasyMock.expect(factory.createRequest(EasyMock.anyObject(URI.class), EasyMock.anyObject(HttpMethod.class))).andReturn(client);
		EasyMock.replay(factory);
		tested.setRequestFactory(factory);
	}
	
	@Test
	public void testExchangeGetWithJsonResponseTypeAndTextHtmlContentType() throws IOException, URISyntaxException {
		Map<String, String> charsetMap = new HashMap<String, String>();
		HttpHeaders responseHeaders = new HttpHeaders();
		charsetMap.put("charset", "utf-8");
		responseHeaders.setContentType(new MediaType(MediaType.TEXT_HTML, charsetMap));
		
		ObjectNode object = mapper.createObjectNode();
		object.put("id", 10);
		object.put("name", "Northern Profile");
		object.put("enabled", true);
		
		InputStream responseBody = new ByteArrayInputStream(mapper.writeValueAsBytes(object));
		
		ClientHttpResponse response = EasyMock.createMock(ClientHttpResponse.class);
		EasyMock.expect(response.getHeaders()).andReturn(responseHeaders).anyTimes();
		EasyMock.expect(response.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
		EasyMock.expect(response.getBody()).andReturn(responseBody).anyTimes();
		response.close();
		EasyMock.expectLastCall().anyTimes();
		EasyMock.replay(response);
		
		EasyMock.reset(client);
		EasyMock.expect(client.execute()).andReturn(response);
		EasyMock.expect(client.getHeaders()).andReturn(new HttpHeaders()).anyTimes();
		EasyMock.replay(client);
		
	
		ResponseEntity<JsonNode> resultEntity = tested.exchange(new URI("http://test"), HttpMethod.GET, null, JsonNode.class);
		JsonNode result = resultEntity.getBody();
		
		assertEquals(result.get("id").asInt(), 10);
		assertEquals(result.get("name").asText(), "Northern Profile");
		assertTrue(result.get("enabled").asBoolean());
	}
	
	@Test
	public void testExchangeGetWithStringResponseTypeAndTextHtmlContentType() throws URISyntaxException, IOException {
		
		Map<String, String> charsetMap = new HashMap<String, String>();
		HttpHeaders responseHeaders = new HttpHeaders();
		charsetMap.put("charset", "utf-8");
		responseHeaders.setContentType(new MediaType(MediaType.TEXT_HTML, charsetMap));
		
		InputStream responseBody = new ByteArrayInputStream("Hello World!".getBytes());
		
		ClientHttpResponse response = EasyMock.createMock(ClientHttpResponse.class);
		EasyMock.expect(response.getHeaders()).andReturn(responseHeaders).anyTimes();
		EasyMock.expect(response.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
		EasyMock.expect(response.getBody()).andReturn(responseBody).anyTimes();
		response.close();
		EasyMock.expectLastCall().anyTimes();
		EasyMock.replay(response);
		
		EasyMock.reset(client);
		EasyMock.expect(client.execute()).andReturn(response);
		EasyMock.expect(client.getHeaders()).andReturn(new HttpHeaders()).anyTimes();
		EasyMock.replay(client);
		
		ResponseEntity<String> resultEntity = tested.exchange(new URI("http://test"), HttpMethod.GET, null, String.class);
		String result = resultEntity.getBody();
		
		assertEquals(result, "Hello World!");
		
	}
}
