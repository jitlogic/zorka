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

public abstract class EventSink {

    public long time() {
        return System.nanoTime();
    }

    public void call() {
        call(0);
    }

    public abstract void call(long t);

    public void error() {
        error(0);
    }

    public abstract void error(long t);

    public abstract String getName();

    public abstract long getCalls();

    public abstract long getErrors();

    public abstract long getTime();
}
