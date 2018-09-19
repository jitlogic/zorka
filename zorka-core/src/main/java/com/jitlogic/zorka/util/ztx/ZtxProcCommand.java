/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.util.ztx;

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.System.err;

public class ZtxProcCommand {

    public static void write(Map<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data, File f) {
        PrintWriter w = null;

        try {
            w = new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)));
            for (Map.Entry<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> pe : data.entrySet()) {
                if (pe.getValue().size() == 1 && pe.getValue().firstEntry().getValue().size() == 1 &&
                        pe.getValue().firstEntry().getValue().firstEntry().getValue().size() == 1) {
                    w.println(pe.getKey() + "|" + pe.getValue().firstEntry().getKey() + "|" +
                        pe.getValue().firstEntry().getValue().firstEntry().getKey() + "|" +
                        pe.getValue().firstEntry().getValue().firstEntry().getValue().first());
                } else {
                    w.println(pe.getKey());
                    for (Map.Entry<String,NavigableMap<String,NavigableSet<String>>> ce : pe.getValue().entrySet()) {
                        w.println(" " + ce.getKey());
                        for (Map.Entry<String,NavigableSet<String>> se : ce.getValue().entrySet()) {
                            if (se.getValue().size() == 1) {
                                w.println("  " + se.getKey() + "|" + se.getValue().first());
                            } else {
                                w.println("  " + se.getKey());
                                for (String s : se.getValue()) {
                                    w.println("   " + s);
                                }
                            }
                        } // for: se
                    } // for: ce
                } // if
            } // for: pe

        } catch (IOException e) {
            throw new ZorkaRuntimeException("Error writing to file " + f, e);
        } finally {
            if (w != null) w.close();
        }
    }

    public static Pattern pattern(String s) {
        return Pattern.compile(s.replace(".", "\\.") + "(\\..*)?");
    }

    public static boolean matches(List<Pattern> patterns, String s) {
        for (Pattern pattern : patterns)
            if (pattern.matcher(s).matches()) return true;
        return false;
    }

    public static void main(String[] args) throws IOException {
        File outf = null;
        List<File> inpf = new ArrayList<File>();
        List<Pattern> incls = new ArrayList<Pattern>();
        List<Pattern> excls = new ArrayList<Pattern>();

        for (int i = 1; i < args.length-1; i++) {
            if ("-o".equals(args[i])) {
                outf = new File(args[i+1]);
            }
            if ("-f".equals(args[i])) {
                File f = new File(args[i+1]);
                if (!f.isFile()) {
                    err.println("No such input file: " + f);
                    return;
                }
                inpf.add(f);
            }
            if ("-i".equals(args[i])) {
                incls.add(pattern(args[i+1]));
            }
            if ("-x".equals(args[i])) {
                excls.add(pattern(args[i+1]));
            }
        }

        if (incls.isEmpty())
            incls.add(Pattern.compile(".*"));

        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> idata = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        ZtxProcReader rdr = new ZtxProcReader(idata);

        for (File f : inpf)
            rdr.read(f);

        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> odata = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        for (Map.Entry<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> e : idata.entrySet()) {
            if (matches(incls, e.getKey()) && !matches(excls, e.getKey()))
                odata.put(e.getKey(), e.getValue());
        }

        write(odata, outf);
    }
}
