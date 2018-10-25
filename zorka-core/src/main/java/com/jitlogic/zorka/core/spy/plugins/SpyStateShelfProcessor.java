/*
 * Copyright (c) 2012-2017 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.SpyStateShelf;
import com.jitlogic.zorka.core.spy.SpyStateShelfData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpyStateShelfProcessor implements SpyProcessor {

    private Logger log = LoggerFactory.getLogger(SpyStateShelfProcessor.class);

    private String keyAttr;
    private String[] vAttrs;
    private SpyStateShelf shelf;

    private long timeout;
    private boolean shelve;
    private boolean useHashCode;

    public SpyStateShelfProcessor(SpyStateShelf shelf, String keyAttr, long timeout,
                                  boolean shelve, boolean useHashCode, String...vAttrs) {
        this.shelf = shelf;
        this.vAttrs = vAttrs;
        this.keyAttr = keyAttr;
        this.timeout = timeout;
        this.shelve = shelve;
        this.useHashCode = useHashCode;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        Object key = rec.get(keyAttr);

        if (key != null) {
            if (useHashCode) key = key.hashCode();
            if (shelve) {
                List<Object> data = new ArrayList<Object>(vAttrs.length);
                for (String vAttr : vAttrs) {
                    data.add(rec.get(vAttr));
                }
                SpyStateShelfData sd = new SpyStateShelfData(System.currentTimeMillis(), timeout, data);
                if (log.isDebugEnabled()) {
                    log.debug("Shelving: k=" + key + ", data=" + sd);
                }
                shelf.shelve(key, sd);
            } else {
                SpyStateShelfData sd = shelf.unshelve(key);
                if (sd != null) {
                  List<Object> data = sd.getData();
                  if (data.size() < vAttrs.length) {
                      log.warn("Unshelved data has less values than receiver expected.");
                  }
                  for (int i = 0; i < Math.min(vAttrs.length, data.size()); i++) {
                      rec.put(vAttrs[i], data.get(i));
                  }
                } else {
                    log.warn("Unable to find object with key=" + key);
                }
            }
        } else {
            log.warn("Got empty key object, nothing to shelf.");
        }

        return rec;
    }

}
