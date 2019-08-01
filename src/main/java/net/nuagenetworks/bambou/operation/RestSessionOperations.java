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
package net.nuagenetworks.bambou.operation;

import java.util.List;

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.bambou.BulkResponse;
import net.nuagenetworks.bambou.RestFetcher;
import net.nuagenetworks.bambou.RestObject;

public interface RestSessionOperations {

    void start() throws RestException;

    void reset() throws RestException;

    void fetch(RestObject restObj) throws RestException;

    void save(RestObject restObj) throws RestException;

    void save(RestObject restObj, Integer responseChoice) throws RestException;

    void delete(RestObject restObj) throws RestException;

    void delete(RestObject restObj, Integer responseChoice) throws RestException;

    void createChild(RestObject restObj, RestObject childRestObj) throws RestException;

    void createChild(RestObject restObj, RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException;

    <T extends RestObject> BulkResponse<T> createChildren(RestObject parent, List<T> children) throws RestException;
    
    <T extends RestObject> BulkResponse<T> bulkSave(List<T> objList) throws RestException;

    <T extends RestObject> BulkResponse<T> bulkDelete(List<T> objList) throws RestException;

    void instantiateChild(RestObject restObj, RestObject childRestObj, RestObject fromTemplate) throws RestException;

    void instantiateChild(RestObject restObj, RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit) throws RestException;

    void assign(RestObject restObj, List<? extends RestObject> childRestObjs) throws RestException;

    void assign(RestObject restObj, List<? extends RestObject> childRestObjs, boolean commit) throws RestException;

    <T extends RestObject> List<T> get(RestFetcher<T> fetcher) throws RestException;

    <T extends RestObject> List<T> fetch(RestFetcher<T> fetcher) throws RestException;

    <T extends RestObject> T getFirst(RestFetcher<T> fetcher) throws RestException;

    <T extends RestObject> int count(RestFetcher<T> fetcher) throws RestException;

    <T extends RestObject> List<T> get(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException;

    <T extends RestObject> List<T> fetch(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException;

    <T extends RestObject> T getFirst(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException;

    <T extends RestObject> int count(RestFetcher<T> fetcher, String filter, String orderBy, String[] groupBy, Integer page, Integer pageSize,
            String queryParameters, boolean commit) throws RestException;

    void assign(RestObject restObj, List<? extends RestObject> childRestObjs, Integer responseChoice, boolean commit) throws RestException;
}
