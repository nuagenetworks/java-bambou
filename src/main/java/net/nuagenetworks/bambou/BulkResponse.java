/*
  Copyright (c) 2019, Alcatel-Lucent Inc
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class BulkResponse<T extends RestObject> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(BulkResponse.class);

    public static class ResponseMetadata implements Serializable{
        protected String success;
        protected String failure;
        protected String total;


        public void setSuccess(String val) { this.success = val; }
        public void setFailure(String val) { this.failure = val; }
        public void setTotal(String val) { this.total = val; }

        public String getSuccess() { return this.success; }
        public String getFailure() { return this.failure; }
        public String getTotal() { return this.total; }
    }


    public static class ResponseItem<T extends RestObject> implements Serializable {
        protected String status;
        protected int index;
        protected JsonNode data;

        public void setStatus(String val) { this.status = val; }
        public void setIndex(int val) { this.index = val; }
        public void setData(JsonNode val) { this.data = val; }

        public String getStatus() { return this.status; }
        public int getIndex() { return this.index; }
        public JsonNode getData() { return this.data; }

        @JsonIgnore
        public RestObject getRestObject(Class<? extends RestObject> type) {
            if (this.status.startsWith("2")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    RestObject obj = mapper.treeToValue(this.data, type);
                    return obj;
                } catch (JsonProcessingException e) {
                    logger.info("Can't parse json object");
                }
            }
            return null;
        }
    }

    protected List<ResponseItem<T>> response;

    protected ResponseMetadata responseMetadata;

    public void setResponse(List<ResponseItem<T>> val) { this.response = val; }
    public void setResponseMetadata(ResponseMetadata val) { this.responseMetadata = val; }

    public ResponseMetadata getResponseMetadata() { return this.responseMetadata; }
    public List<ResponseItem<T>> getResponse() { return this.response; }

}
