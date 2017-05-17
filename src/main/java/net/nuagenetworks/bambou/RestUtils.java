package net.nuagenetworks.bambou;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.util.BambouUtils;

public class RestUtils {

    public static <T extends RestObject> T createRestObjectWithContent(Class<T> restObjectClass, JsonNode jsonNode) throws RestException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.treeToValue(jsonNode, restObjectClass);
        } catch (JsonProcessingException ex) {
            throw new RestException(ex);
        }
    }

    public static String toString(Object content) throws RestException {
        return BambouUtils.toString(content);
    }
}
