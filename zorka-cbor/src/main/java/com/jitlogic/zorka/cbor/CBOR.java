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

package com.jitlogic.zorka.cbor;

/**
 *
 */
public class CBOR {

    public static final int TYPE_MASK = 0xe0;
    public static final int VALU_MASK = 0x1f;

    public static final int UINT_BASE = 0x00;
    public static final int UINT_CODE0 = 0x00;
    public static final int UINT_CODE1 = 0x18;
    public static final int UINT_CODE2 = 0x19;
    public static final int UINT_CODE4 = 0x1a;
    public static final int UINT_CODE8 = 0x1b;
    public static final int UINT_MASK  = 0x1f;

    public static final int NINT_BASE = 0x20;
    public static final int NINT_CODE0 = 0x20;
    public static final int NINT_CODE1 = 0x38;
    public static final int NINT_CODE2 = 0x39;
    public static final int NINT_CODE4 = 0x3a;
    public static final int NINT_CODE8 = 0x3b;

    public static final int BYTES_BASE = 0x40;
    public static final int BYTES_CODE0 = 0x40;
    public static final int BYTES_CODE1 = 0x58;
    public static final int BYTES_CODE2 = 0x59;
    public static final int BYTES_CODE4 = 0x5a;
    public static final int BYTES_CODE8 = 0x5b;
    public static final int BYTES_VCODE = 0x5f;

    public static final int STR_BASE = 0x60;
    public static final int STR_CODE0 = 0x60;
    public static final int STR_CODE1 = 0x78;
    public static final int STR_CODE2 = 0x79;
    public static final int STR_CODE4 = 0x7a;
    public static final int STR_CODE8 = 0x7b;
    public static final int STR_VCODE = 0x7f;

    public static final int ARR_BASE = 0x80;
    public static final int ARR_CODE0 = 0x80;
    public static final int ARR_CODE1 = 0x98;
    public static final int ARR_CODE2 = 0x99;
    public static final int ARR_CODE4 = 0x9a;
    public static final int ARR_CODE8 = 0x9b;
    public static final int ARR_VCODE = 0x9f;

    public static final int MAP_BASE = 0xa0;
    public static final int MAP_CODE0 = 0xa0;
    public static final int MAP_CODE1 = 0xb8;
    public static final int MAP_CODE2 = 0xb9;
    public static final int MAP_CODE4 = 0xba;
    public static final int MAP_CODE8 = 0xbb;
    public static final int MAP_VCODE = 0xbf;

    public static final int TAG_BASE  = 0xc0;
    public static final int TAG_CODE0 = 0xc0;
    public static final int TAG_CODE1 = 0xd8;
    public static final int TAG_CODE2 = 0xd9;
    public static final int TAG_CODE4 = 0xda;
    public static final int TAG_CODE8 = 0xdb;

    public static final int SIMPLE_BASE  = 0xe0;
    public static final int FALSE_CODE   = 0xf4;
    public static final int TRUE_CODE    = 0xf5;
    public static final int NULL_CODE    = 0xf6;
    public static final int UNKNOWN_CODE = 0xf7;
    public static final int SIMPLE_END   = 0xf8;

    public static final int FLOAT_BASE2 = 0xf9;
    public static final int FLOAT_BASE4 = 0xfa;
    public static final int FLOAT_BASE8 = 0xfb;

    public static final int BREAK_CODE = 0xff;

    public static final Object BREAK = new Object();
    public static final Object UNKNOWN = new Object();

    public static int cborSize(long v) {
        if (v < 0) {
            v = -v - 1;
        }
        return v < UINT_CODE1 ? 1
            : v < 0x100 ? 2
            : v < 0x10000 ? 3
            : v < 0x1000000L ? 5 : 9;
    }

    private CBOR() {
        // This class cannot be instantiated
    }
}
