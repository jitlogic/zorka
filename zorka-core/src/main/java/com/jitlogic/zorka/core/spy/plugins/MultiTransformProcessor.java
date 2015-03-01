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


import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MultiTransformProcessor implements SpyProcessor {

    private static ZorkaLog log = ZorkaLogger.getLog(MultiTransformProcessor.class);

    private File cfgFile;
    private long cfgModTime = 0;
    private String dstField;
    private List<String> srcExprs;

    private List<String> outputs = new ArrayList<String>();
    private List<Pattern[]> inputs = new ArrayList<Pattern[]>();

    private long lastTstamp = 0L;
    private long reloadInterval = 10 * 1000000000L;

    public MultiTransformProcessor(String cfgPath, String dstField, String... srcExprs) {
        this.cfgFile = new File(cfgPath);
        this.dstField = dstField;
        this.srcExprs = Arrays.asList(srcExprs);
        checkReload();
    }


    public void checkReload() {
        long t = System.nanoTime();

        if (lastTstamp < t - reloadInterval) {
            lastTstamp = t;

            if (cfgFile.lastModified() == cfgModTime) {
                return;
            }
        }

        if (!cfgFile.exists()) {
            log.error(ZorkaLogger.ZSP_SUBMIT, "Config file " + cfgFile + " does not exist. MultiTransformProcessor will have no configuration.");
            return;
        }

        if (!cfgFile.isFile() || !cfgFile.canRead()) {
            log.error(ZorkaLogger.ZSP_SUBMIT, "Config file " + cfgFile + " is not readable. MultiTransformProcessor will have no configuration.");
            return;
        }

        reload();
    }


    public void reload() {

        BufferedReader rdr = null;

        try {
            rdr = new BufferedReader(new FileReader(cfgFile));
            String line;
            while (null != (line = rdr.readLine())) {
                if (line.matches("\\s*") || line.matches("\\s*#.*")) {
                    continue;
                }
                String[] segs = line.split("\\s+");
                if (segs.length != srcExprs.size() + 1) {
                    log.error(ZorkaLogger.ZSP_SUBMIT, "Invalid config line: '" + line + "'. Skipping.");
                    try {
                        Pattern[] patterns = new Pattern[segs.length - 1];
                        for (int i = 1; i < segs.length; i++) {
                            patterns[i - 1] = Pattern.compile(segs[i]);
                        }
                        outputs.add(segs[0]);
                        inputs.add(patterns);
                    } catch (Exception e) {
                        log.error(ZorkaLogger.ZSP_SUBMIT, "Error processing config line: '" + line + "'. Skipped", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZSP_SUBMIT, "I/O error while reading file: " + cfgFile, e);
        } finally {
            if (rdr != null) {
                try {
                    rdr.close();
                } catch (IOException e) {
                }
            }
        }
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        String[] vals = new String[srcExprs.size()];

        for (int i = 0; i < vals.length; i++) {
            vals[i] = ObjectInspector.substitute(srcExprs.get(i), record);
        }

        for (int inp = 0; inp < inputs.size(); inp++) {
            Pattern[] patterns = inputs.get(inp);
            boolean matches = true;
            for (int i = 0; i < vals.length; i++) {
                if (!patterns[i].matcher(vals[i]).matches()) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                record.put(dstField, ObjectInspector.substitute(outputs.get(inp), record));
                return record;
            }
        }

        // No match found: stop processing here
        return null;
    }
}
