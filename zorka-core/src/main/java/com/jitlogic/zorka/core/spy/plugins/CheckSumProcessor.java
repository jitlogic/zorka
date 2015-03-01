/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;


import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

public class CheckSumProcessor implements SpyProcessor {

    private static final ZorkaLog log = ZorkaLogger.getLog(CheckSumProcessor.class);

    public final static int CRC32_TYPE = 1;
    public final static int MD5_TYPE = 2;
    public final static int SHA1_TYPE = 3;

    public final static int MAX_LIMIT = 1024;

    /**
     * Checksum type
     */
    private int type;

    /**
     * Output limit
     */
    private int limit;

    /**
     * Source field
     */
    private String srcField;

    /**
     * Destination field
     */
    private String dstField;


    public CheckSumProcessor(String dstField, String srcField, int type, int limit) {
        this.dstField = dstField;
        this.srcField = srcField;
        this.limit = limit;
        this.type = type;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = record.get(srcField);

        if (val != null) {
            switch (type) {
                case CRC32_TYPE:
                    record.put(dstField, cut(ZorkaUtil.crc32(val.toString())));
                    break;
                case MD5_TYPE:
                    record.put(dstField, cut(ZorkaUtil.md5(val.toString())));
                    break;
                case SHA1_TYPE:
                    record.put(dstField, cut(ZorkaUtil.sha1(val.toString())));
                    break;
                default:
                    log.error(ZorkaLogger.ZSP_ERRORS, "Unknown checksum type: " + type);
            }
        }

        return record;
    }


    private String cut(String input) {
        return input.length() > limit ? input.substring(0, limit) : input;
    }


}
