/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.netkit.log;

import java.util.concurrent.atomic.AtomicLong;

public class CountingSink extends EventSink {

    private final String name;

    private final AtomicLong calls = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong time = new AtomicLong(0);

    public CountingSink(String name) {
        this.name = name;
    }

    @Override
    public void call(long t) {
        calls.incrementAndGet();
        if (t > 0) time.addAndGet(time()-t);
    }

    @Override
    public void error(long t) {
        errors.incrementAndGet();
        if (t > 0) time.addAndGet(time()-t);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getCalls() {
        return calls.get();
    }

    @Override
    public long getErrors() {
        return errors.get();
    }

    @Override
    public long getTime() {
        return time.get();
    }
}
