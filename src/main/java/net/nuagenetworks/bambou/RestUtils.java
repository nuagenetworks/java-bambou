package net.nuagenetworks.bambou;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestUtils {
    
    public static <T extends RestObject> T createRestObjectWithContent(Class<T> restObjectClass, JsonNode jsonNode) throws RestException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.treeToValue(jsonNode, restObjectClass);
        } catch (JsonProcessingException ex) {
            throw new RestException(ex);
        }
    }
}
