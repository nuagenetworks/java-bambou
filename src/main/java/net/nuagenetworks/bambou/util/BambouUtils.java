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
package net.nuagenetworks.bambou.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.bambou.RestObject;

public class BambouUtils {

    private static final String RESPONSE_CHOICE_PARAM = "responseChoice";

    public static String toString(Object content) throws RestException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            throw new RestException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T[]> getArrayClass(T restObj) throws RestException {
        Class<T> restObjClass = (Class<T>) restObj.getClass();
        return getArrayClass(restObjClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T[]> getArrayClass(Class<T> restObjClass) throws RestException {
        try {
            return (Class<T[]>) Class.forName("[L" + restObjClass.getName() + ";");
        } catch (ClassNotFoundException ex) {
            throw new RestException(ex);
        }
    }

    public static void copyJsonProperties(RestObject fromRestObj, RestObject toRestObj) throws RestException {
        if (fromRestObj.getClass() != toRestObj.getClass()) {
            return;
        }

        List<Field> fields = getAllFields(fromRestObj.getClass());
        for (Field field : fields) {
            if (field.getAnnotation(JsonProperty.class) != null) {
                PropertyDescriptor pd = getPropertyDescriptor(toRestObj, field.getName());
                if (pd != null) {
                    Object value = getRestObjectProperty(pd, fromRestObj);
                    setRestObjectProperty(pd, toRestObj, value);
                }
            }
        }
    }

    public static <T extends RestObject> void setTemplateId(T restObj, RestObject template) throws RestException {
        PropertyDescriptor pd = getPropertyDescriptor(restObj, "templateID");
        if (pd != null) {
            setRestObjectProperty(pd, restObj, template.getId());
        } else {
            throw new RestException(String.format("Cannot instantiate a child that does not have a templateID property: %s", restObj));
        }
    }

    public static String getResponseChoiceParam(Integer responseChoice) {
        if (responseChoice == null) {
            return null;
        } else {
            return RESPONSE_CHOICE_PARAM + '=' + responseChoice.intValue();
        }
    }

    private static PropertyDescriptor getPropertyDescriptor(RestObject restObject, String propertyName) throws RestException {
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(restObject.getClass()).getPropertyDescriptors()) {
                if (pd.getName().equals(propertyName)) {
                    return pd;
                }
            }
        } catch (IntrospectionException ex) {
            throw new RestException(ex);
        }

        return null;
    }

    private static void setRestObjectProperty(PropertyDescriptor pd, RestObject restObject, Object value) throws RestException {
        try {
            pd.getWriteMethod().invoke(restObject, value);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RestException(ex);
        }
    }

    private static Object getRestObjectProperty(PropertyDescriptor pd, RestObject restObject) throws RestException {
        try {
            return pd.getReadMethod().invoke(restObject);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RestException(ex);
        }
    }

    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private static List<Field> getAllFields(Class<?> type) {
        return getAllFields(new LinkedList<Field>(), type);
    }
}
