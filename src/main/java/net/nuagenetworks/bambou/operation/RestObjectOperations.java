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
import net.nuagenetworks.bambou.RestObject;
import net.nuagenetworks.bambou.RestSession;

public interface RestObjectOperations {

    void fetch() throws RestException;

    void fetch(boolean withMetadata) throws RestException;

    void save() throws RestException;

    void save(Integer responseChoice) throws RestException;

    void delete() throws RestException;

    void delete(Integer responseChoice) throws RestException;

    void createChild(RestObject childRestObj) throws RestException;

    void createChild(RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException;

    void instantiateChild(RestObject childRestObj, RestObject fromTemplate) throws RestException;

    void instantiateChild(RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit) throws RestException;

    void unassign(List<? extends RestObject> childRestObjs) throws RestException;

    void assignOne(RestObject childRestObj) throws RestException;

    void assign(List<? extends RestObject> childRestObjs) throws RestException;

    void assign(List<? extends RestObject> childRestObjs, boolean commit) throws RestException;
    
    void assign(List<? extends RestObject> childRestObjs,Integer responseChoice, boolean commit) throws RestException;
    
    <T extends RestObject> BulkResponse<T> createChildren(List<T> children) throws RestException;

    void fetch(RestSession<?> session, boolean withMetadata) throws RestException;

    void fetch(RestSession<?> session) throws RestException;

    void save(RestSession<?> session) throws RestException;

    void save(RestSession<?> session, Integer responseChoice) throws RestException;

    void delete(RestSession<?> session) throws RestException;

    void delete(RestSession<?> session, Integer responseChoice) throws RestException;

    void createChild(RestSession<?> session, RestObject childRestObj) throws RestException;

    void createChild(RestSession<?> session, RestObject childRestObj, Integer responseChoice, boolean commit) throws RestException;

    void instantiateChild(RestSession<?> session, RestObject childRestObj, RestObject fromTemplate) throws RestException;

    void instantiateChild(RestSession<?> session, RestObject childRestObj, RestObject fromTemplate, Integer responseChoice, boolean commit)
            throws RestException;

    void assign(RestSession<?> session, List<? extends RestObject> childRestObjs) throws RestException;

    void assign(RestSession<?> session, List<? extends RestObject> childRestObjs, boolean commit) throws RestException;
    
    void unassignAll(RestSession<?> session, Class<? extends RestObject> objectType, boolean commit) throws RestException;
    
    void unassignAll(RestSession<?> session, Class<? extends RestObject> objectType, Integer responseChoice ,boolean commit) throws RestException;

    void assign(RestSession<?> session, List<? extends RestObject> childRestObjs, Integer responseChoice, boolean commit) throws RestException;

    void unassign(RestSession<?> session, List<? extends RestObject> childRestObjs, boolean commit) throws RestException;

    void assignOne(RestSession<?> session, RestObject childRestObj) throws RestException;
}
