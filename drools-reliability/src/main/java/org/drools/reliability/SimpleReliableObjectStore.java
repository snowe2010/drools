/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.reliability;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.common.EventFactHandle;
import org.drools.core.common.IdentityObjectStore;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.InternalWorkingMemoryEntryPoint;
import org.infinispan.commons.api.BasicCache;

public class SimpleReliableObjectStore extends IdentityObjectStore {

    private final BasicCache<Long, StoredObject> cache;

    private boolean reInitPropagated = false;

    public SimpleReliableObjectStore(BasicCache<Long, StoredObject> cache) {
        super();
        this.cache = cache;
    }

    @Override
    public void addHandle(InternalFactHandle handle, Object object) {
        super.addHandle(handle, object);
        putIntoPersistedCache(handle, handle.hasMatches());
    }

    @Override
    public void removeHandle(InternalFactHandle handle) {
        removeFromPersistedCache(handle.getObject());
        super.removeHandle(handle);
    }

    List<StoredObject> reInit(InternalWorkingMemory session, InternalWorkingMemoryEntryPoint ep) {
        reInitPropagated = true;
        List<StoredObject> propagated = new ArrayList<>();
        List<StoredObject> notPropagated = new ArrayList<>();
        for (StoredObject entry : cache.values()) {
            if (entry.isPropagated()) {
                propagated.add(entry);
            } else {
                notPropagated.add(entry);
            }
        }
        cache.clear();

        // fact handles with a match have been already propagated in the original session, so they shouldn't fire
        propagated.forEach(obj -> obj.repropagate(ep));
        session.fireAllRules(match -> false);
        reInitPropagated = false;

        // fact handles without any match have never been propagated in the original session, so they should fire
        return notPropagated;
    }

    void putIntoPersistedCache(InternalFactHandle handle, boolean propagated) {
        Object object = handle.getObject();
        StoredObject storedObject = handle.isEvent() ?
                new StoredObject(object, reInitPropagated || propagated, ((EventFactHandle) handle).getStartTimestamp(), ((EventFactHandle) handle).getDuration()) :
                new StoredObject(object, reInitPropagated || propagated);
        cache.put(fhMap.get(object).getId(), storedObject);
    }

    void removeFromPersistedCache(Object object) {
        InternalFactHandle fh = fhMap.get(object);
        if (fh != null) {
            cache.remove(fh.getId());
        }
    }
}