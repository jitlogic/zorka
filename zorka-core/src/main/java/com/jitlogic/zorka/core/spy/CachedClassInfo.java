/*
 * Copyright (c) 2012-2020 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.core.spy;

public class CachedClassInfo {

    final static int IS_INTERFACE = 0x01;

    private final int flags;
    private final String name, superclassName;
    private final String[] interfaceNames;

    CachedClassInfo(int flags, String name, String superclassName, String[] interfaceNames) {
        this.name = name;
        this.superclassName = superclassName;
        this.interfaceNames = interfaceNames;
        this.flags = flags;
    }

    public String getClassName() {
        return name;
    }

    public String getSuperclassName() {
        return superclassName;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public boolean isInterface() {
        return 0 != (flags & IS_INTERFACE);
    }
}
