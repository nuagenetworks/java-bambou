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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nuagenetworks.bambou.annotation.RestEntity;
import net.nuagenetworks.bambou.BulkResponse;
import net.nuagenetworks.bambou.operation.RestObjectOperations;
import net.nuagenetworks.bambou.util.BambouUtils;

public class RestObject implements RestObjectOperations, Serializable {

    private static final String PATCH_TYPE_HEADER = "X-Nuage-PatchType";
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(RestObject.class);

    @JsonProperty(value = "ID")
    protected String id;

    @JsonProperty(value = "parentID")
    protected String parentId;

    @JsonProperty(value = "parentType")
    protected String parentType;

    private transient Map<String, RestFetcher<? extends RestObject>> fetcherRegistry = Collections
            .synchronizedMap(new HashMap<String, RestFetcher<? extends RestObject>>());

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    @Override
    public void unassign(RestSession<?> session, List<? extends RestObject> childRestObjs, boolean commit) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(1);
    	// Make sure the child objects passed in is not null or empty
    	if (childRestObjs == null || childRestObjs.size() == 0) {	
    		throw new RestException("Child objects was null or empty.");	
    	}
        
        // Extract IDs from the specified child objects
        List<String> ids = new ArrayList<String>();
        for (RestObject restObject : childRestObjs) {
            ids.add(restObject.getId());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(PATCH_TYPE_HEADER, "REMOVE");
        Class<?> childRestObjClass = childRestObjs.get(0).getClass();
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.PATCH, getResourceUrlForChildType(session, childRestObjClass), params, headers, ids, BambouUtils.getArrayClass(this));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            if (commit) {
                // Add all the children passed in to the fetcher
                for (RestObject childRestObj : childRestObjs) {
                    removeChild(childRestObj);
                }
            }
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void assignOne(RestSession<?> session, RestObject childRestObj) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(1);
        
        // Extract IDs from the specified child objects
        List<String> ids = new ArrayList<String>();
        ids.add(childRestObj.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.set(PATCH_TYPE_HEADER, "ADD");
        Class<?> childRestObjClass = childRestObj.getClass();
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.PATCH, getResourceUrlForChildType(session, childRestObjClass), params, headers, ids, BambouUtils.getArrayClass(this));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            addChild(childRestObj);
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void unassign(List<? extends RestObject> childRestObjs) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.unassign(this, childRestObjs, true);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void assignOne(RestObject childRestObj) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.assignOne(this, childRestObj);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void fetch() throws RestException {
        this.fetch(false);
    }

    @Override
    public void fetch(boolean withMetadata) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.fetch(this, withMetadata);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void save() throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.save(this);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void save(Integer responseChoice) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.save(this, responseChoice);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void delete() throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.delete(this);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void delete(Integer responseChoice) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.delete(this, responseChoice);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void createChild(RestObject childRestObj) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.createChild(this, childRestObj);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void createChild(RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.createChild(this, childRestObj, responseChoice, commit);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public <T extends RestObject> BulkResponse<T> createChildren(List<T> children) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            return session.createChildren(this, children);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void instantiateChild(RestObject childRestObj, RestObject fromTemplate) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.instantiateChild(this, childRestObj, fromTemplate);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void instantiateChild(RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.instantiateChild(this, childRestObj, fromTemplate, responseChoice, commit);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void assign(List<? extends RestObject> childRestObjs) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.assign(this, childRestObjs);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }

    @Override
    public void assign(List<? extends RestObject> childRestObjs, boolean commit) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.assign(this, childRestObjs, commit);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }
    
    public void assign(List<? extends RestObject> childRestObjs, Integer responseChoice, boolean commit) throws RestException {
        RestSession<?> session = RestSession.getCurrentSession();
        if (session != null) {
            session.assign(this, childRestObjs,responseChoice, commit);
        } else {
            throw new RestException("Session not available in current thread");
        }
    }
    
    

    protected void registerFetcher(RestFetcher<?> fetcher, String restName) {
        fetcherRegistry.put(restName, fetcher);
    }

    @Override
    public void fetch(RestSession<?> session) throws RestException {
        this.fetch(session, false);
    }

    @Override
    public void fetch(RestSession<?> session, boolean withMetadata) throws RestException {
        String params = BambouUtils.getEmbededMetadataParam(withMetadata);
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.GET, getResourceUrl(session), params, null, null,
                BambouUtils.getArrayClass(this));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL && response.getBody().length == 1) {
            // Success
            RestObject responseRestObj = response.getBody()[0];
            BambouUtils.copyJsonProperties(responseRestObj, this);
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void save(RestSession<?> session) throws RestException {
        save(session, null);
    }

    @Override
    public void save(RestSession<?> session, Integer responseChoice) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(responseChoice);
        ResponseEntity<Object> response = session.sendRequestWithRetry(HttpMethod.PUT, getResourceUrl(session), params, null, this, Object.class);
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            // Success
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void delete(RestSession<?> session) throws RestException {
        delete(session, 1);
    }

    @Override
    public void delete(RestSession<?> session, Integer responseChoice) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(responseChoice);
        ResponseEntity<Object> response = session.sendRequestWithRetry(HttpMethod.DELETE, getResourceUrl(session), params, null, null, Object.class);
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            // Success
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void createChild(RestSession<?> session, RestObject childRestObj) throws RestException {
        createChild(session, childRestObj, null, true);
    }

    @Override
    public void createChild(RestSession<?> session, RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(responseChoice);
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.POST, getResourceUrlForChildType(session, childRestObj.getClass()),
                params, null, childRestObj, BambouUtils.getArrayClass(childRestObj));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL && response.getBody().length >= 1) {
            // Success
            if (response.getBody().length > 1) {
                logger.warn("HTTP response to POST request constains more than one object. Only processing first object");
            }

            RestObject responseRestObj = response.getBody()[0];
            BambouUtils.copyJsonProperties(responseRestObj, childRestObj);

            if (commit) {
                addChild(childRestObj);
            }
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    public <T extends RestObject> BulkResponse<T> createChildren(RestSession<?> session, List<T> children) throws RestException {
        ResponseEntity<BulkResponse> response = session.sendRequestWithRetry(HttpMethod.POST, getResourceUrlForChildType(session, children.get(0).getClass()),
                null, null, children, BulkResponse.class);
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL ) {
            ObjectMapper mapper = new ObjectMapper();
            BulkResponse<T> resp = response.getBody();
            for (BulkResponse.ResponseItem item : resp.getResponse()) {
                if (item.getStatus().startsWith("2")) {
                    RestObject obj = item.getRestObject(children.get(0).getClass());
                    RestObject target = children.get(item.getIndex());
                    BambouUtils.copyJsonProperties(obj, target);
                    addChild(target);
                } else {
                    logger.debug("Error creating bulk entity: "+item.getStatus());
                }
            }
            return resp;
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }

    @Override
    public void instantiateChild(RestSession<?> session, RestObject childRestObj, RestObject fromTemplate) throws RestException {
        instantiateChild(session, childRestObj, fromTemplate, null, true);
    }

    @Override
    public void instantiateChild(RestSession<?> session, RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit)
            throws RestException {
        if (fromTemplate.getId() == null) {
            throw new RestException(String.format("Cannot instantiate a child from a template with no ID: %s", fromTemplate));
        }
        BambouUtils.setTemplateId(childRestObj, fromTemplate);

        createChild(session, childRestObj, responseChoice, commit);
    }

    @Override
    public void assign(RestSession<?> session, List<? extends RestObject> childRestObjs) throws RestException {
        assign(session, childRestObjs, true);
    }
    
    @Override
    public void assign(RestSession<?> session, List<? extends RestObject> childRestObjs ,boolean commit) throws RestException {
        assign(session, childRestObjs, null,commit);
    }
    

    @Override
    public void assign(RestSession<?> session, List<? extends RestObject> childRestObjs, Integer responseChoice ,boolean commit) throws RestException {
        String params = BambouUtils.getResponseChoiceParam(responseChoice);
    	// Make sure the child objects passed in is not null or empty
    	if (childRestObjs == null || childRestObjs.size() == 0) {	
    		throw new RestException("Child objects was null or empty.");	
    	}
        
        // Extract IDs from the specified child objects
        List<String> ids = new ArrayList<String>();
        for (RestObject restObject : childRestObjs) {
            ids.add(restObject.getId());
        }

        Class<?> childRestObjClass = childRestObjs.get(0).getClass();
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.PUT, getResourceUrlForChildType(session, childRestObjClass), params, null,
                ids, BambouUtils.getArrayClass(this));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            // Success

            if (commit) {
                // Add all the children passed in to the fetcher
                for (RestObject childRestObj : childRestObjs) {
                    addChild(childRestObj);
                }
            }
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
    }
    
    @Override
    public void unassignAll(RestSession<?> session, Class<? extends RestObject> objectType, boolean commit) throws RestException {
        unassignAll(session, objectType, null,commit);
    }


	@Override
	public void unassignAll(RestSession<?> session, Class<? extends RestObject> objectType, Integer responseChoice, boolean commit)
			throws RestException {
	    String params = BambouUtils.getResponseChoiceParam(responseChoice);
		// Make sure the objectType is not null
    	if (objectType == null) {	
    		throw new RestException("Object type was null.");	
    	}
        
        // Dummy list of ids, as we are unassigning all from the parent
        List<String> ids = new ArrayList<String>();
        
        ResponseEntity<RestObject[]> response = session.sendRequestWithRetry(HttpMethod.PUT, getResourceUrlForChildType(session, objectType), params, null,
                ids, BambouUtils.getArrayClass(this));
        if (response.getStatusCode().series() == HttpStatus.Series.SUCCESSFUL) {
            // Success

            if (commit) {
                removeChildren(objectType);
            }
        } else {
            // Error
            throw new RestException("Response received with status code: " + response.getStatusCode());
        }
		
	}

	@JsonIgnore
    public String getRestName() {
        return getRestName(getClass());
    }

    @JsonIgnore
    protected static String getRestName(Class<?> restObjClass) {
        RestEntity annotation = restObjClass.getAnnotation(RestEntity.class);
        return annotation.restName();
    }

    private void addChild(RestObject childRestObj) throws RestException {
        // Get the object's resource name
        String restName = getRestName(childRestObj.getClass());

        // Add child object to registered fetcher for child type
        @SuppressWarnings("unchecked")
        RestFetcher<RestObject> children = (RestFetcher<RestObject>) fetcherRegistry.get(restName);
        if (children == null) {
            throw new RestException(String.format("Could not find fetcher with name %s while adding %s in parent %s", restName, childRestObj, this));
        }

        if (!children.contains(childRestObj)) {
            children.add(childRestObj);
        }
    }
    
    private void removeChildren(Class<? extends RestObject> objectType) throws RestException {
    	// Get the object's resource name
        String restName = getRestName(objectType);

        // Add child object to registered fetcher for child type
        @SuppressWarnings("unchecked")
        RestFetcher<RestObject> children = (RestFetcher<RestObject>) fetcherRegistry.get(restName);
        if (children == null) {
            throw new RestException(String.format("Could not find fetcher with name %s while removing children %s in parent %s", restName, objectType, this));
        }

        children.clear();
    }

    private void removeChild(RestObject child) throws RestException {
    	// Get the object's resource name
        String restName = getRestName(child.getClass());

        // Add child object to registered fetcher for child type
        @SuppressWarnings("unchecked")
        RestFetcher<RestObject> children = (RestFetcher<RestObject>) fetcherRegistry.get(restName);
        if (children == null) {
            throw new RestException(String.format("Could not find fetcher with name %s while removing children in parent %s", restName, this));
        }

        children.remove(child);
    }

    @JsonIgnore
    protected String getResourceUrl(RestSession<?> session) {
        // Get the object's resource name
        RestEntity annotation = getClass().getAnnotation(RestEntity.class);
        String resourceName = annotation.resourceName();

        // Build the base URL
        String url = session.getRestBaseUrl();

        // Build the complete URL for the specified object
        if (id != null) {
            return String.format("%s/%s/%s", url, resourceName, id);
        } else {
            return String.format("%s/%s", url, resourceName);
        }
    }

    @JsonIgnore
    protected String getResourceUrlForChildType(RestSession<?> session, Class<?> childRestObjClass) {
        // Get the child object's resource name
        RestEntity annotation = childRestObjClass.getAnnotation(RestEntity.class);
        String childResourceName = annotation.resourceName();

        return String.format("%s/%s", getResourceUrl(session), childResourceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof RestObject)) {
            throw new IllegalStateException();
        }

        RestObject restObj1 = this;
        RestObject restObj2 = (RestObject) obj;

        if (!restObj1.getRestName().equals(restObj2.getRestName())) {
            return false;
        }

        if (restObj1.id != null && restObj2.id != null) {
            return restObj1.id.equals(restObj2.id);
        }

        return false;
    }

    @Override
    public String toString() {
        return "RestObject [id=" + id + ", parentId=" + parentId + ", parentType=" + parentType + ", fetcherRegistry=" + fetcherRegistry + "]";
    }
}
