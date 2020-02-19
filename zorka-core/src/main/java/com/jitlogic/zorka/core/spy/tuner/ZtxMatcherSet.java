/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.tuner;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.BitVector;
import com.jitlogic.zorka.core.spy.PatternMatcherSet;
import com.jitlogic.zorka.core.spy.SpyMatcher;
import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZtxMatcherSet implements SpyMatcherSet {

    private final static Logger log = LoggerFactory.getLogger(ZtxMatcherSet.class);

    private volatile BitVector cids;
    private volatile BitVector mids;

    private SymbolRegistry registry;

    private volatile PatternMatcherSet patternMatcherSet;

    private CidsMidsZtxReader reader;

    private Map<String,String> ztxs = new ConcurrentHashMap<String, String>();

    private File ztxDir;
    private File ztxLog;

    public final static List<String> INITIAL_EXCLUSIONS = Collections.unmodifiableList(Arrays.asList(
            "com.jitlogic.zorka.**",
            "java.**",
            "sun.reflect.**",
            "sun.awt.**",
            "com.sun.beans.**",
            "$Proxy*",
            "**$$Lambda$**"
    ));

    public ZtxMatcherSet(File ztxDir, File ztxLog, SymbolRegistry registry, boolean initExcl, boolean scanZtx) {
        cids = new BitVector();
        mids = new BitVector();

        this.registry = registry;


        reader = new CidsMidsZtxReader(registry, cids, mids);

        this.ztxDir = ztxDir;
        this.ztxLog = ztxLog;

        patternMatcherSet = new PatternMatcherSet();

        if (initExcl) {
            for (String xcl : INITIAL_EXCLUSIONS) {
                patternMatcherSet = patternMatcherSet.include(SpyMatcher.fromString(xcl));
            }
            patternMatcherSet = patternMatcherSet.include(SpyMatcher.fromString("**").forTrace().priority(1000));
        }

        if (scanZtx) {
            scanZtxDir();
            scanZtxClasspath();
        }

        log.info("Tracer exclusion packages found: " + ztxs.keySet());

        if (!ztxDir.getPath().equals(ztxLog.getParent()) && ztxLog.isFile()) {
            loadZtx(ztxLog);
        }
    }

    @Override
    public boolean classMatch(String className) {
        int classId = registry.symbolId(className);

        if (!patternMatcherSet.classMatch(className)) return false;

        synchronized (this) {
            if (cids.get(classId)) return false;
            probe(className);
            return !cids.get(classId);
        }
    }

    @Override
    public boolean classMatch(Class<?> clazz, boolean matchMethods) {
        String className = clazz.getName();
        return classMatch(className);
    }

    @Override
    public boolean methodMatch(String cn, List<String> sc, List<String> ca, List<String> ci, int acc,
                               String mn, String ms, List<String> ma) {

        if (!patternMatcherSet.methodMatch(cn, sc, ca, ci, acc, mn, ms, ma)) return false;

        synchronized (this) {
            return !mids.get(registry.methodId(cn, mn, ms));
        }
    }

    @Override
    public synchronized void clear() {
        cids.reset();
        mids.reset();
        patternMatcherSet.clear();
    }


    public synchronized void include(SpyMatcher... includes) {
        patternMatcherSet = patternMatcherSet.include(includes);
    }


    private synchronized void scanZtxDir() {
        String[] lst = ztxDir.list();
        if (lst != null) {
            for (String fname : lst) {
                File f = new File(ztxDir, fname);
                if (f.isFile() && fname.endsWith(".ztx")) {
                    if (fname.startsWith("_")) {
                        loadZtx("file:"+f.getPath());
                    } else {
                        ztxs.put(fname.substring(0, fname.length()-4), "file:"+f.getPath());
                    }
                }
            }
        }
    }


    private synchronized void scanZtxClasspath() {
        InputStream is = getClass().getResourceAsStream("/com/jitlogic/zorka/ztx/pkgs.lst");

        if (is != null) {
            try {
                BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
                for (String pkg = rdr.readLine(); pkg != null; pkg = rdr.readLine()) {
                    if (pkg.trim().length() > 0) {
                        ztxs.put(pkg.substring(0, pkg.length()-4), "classpath:/com/jitlogic/zorka/ztx/" + pkg.trim());
                    }
                }
            } catch (IOException e) {
                log.error("I/O error while reading embedded trace exclusions list.");
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("I/O error while closing embedded trace exclusions list.");
                }
            }
        } else {
            log.warn("Embedded trace exclusions list not found.");
        }

    }

    private synchronized void loadZtx(File f) {
        loadZtx("file:"+f.getPath());
    }

    private synchronized void loadZtx(String path) {
        log.info("Loading: " + path);
        InputStream is = null;
        try {
            if (path.startsWith("file:")) {
                is = new FileInputStream(path.substring(5));
                reader.read(is);
            } else if (path.startsWith("classpath:")) {
                is = getClass().getResourceAsStream(path.substring(10));
                if (is != null) {
                    reader.read(is);
                } else {
                    log.error("Tracer exclusion file " + path.substring(10) + " not found in classpath.");
                }
            } else {
                log.error("Invalid exclusion file path prefix: " + path);
            }
        } catch (Exception e) {
            log.error("Error occured when loading tracer exclusion file: " + path, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Error when closing tracer exclusion file: " + path, e);
                }
            }
        }
    }


    private synchronized void probe(String className) {

        Set<String> found = null;

        for (Map.Entry<String,String> e : ztxs.entrySet()) {
            if (className.startsWith(e.getKey())) {
                loadZtx(e.getValue());
                if (found == null) found = new HashSet<String>();
                found.add(e.getKey());
            }
        }

        if (found != null) {
            for (String s : found) {
                ztxs.remove(s);
            }
        }
    }


    public synchronized void add(String className, String methodName, String methodSignature) {
        int mid = registry.methodId(className, methodName, methodSignature);

        if (!mids.get(mid)) {
            mids.set(mid);
            cids.set(registry.symbolId(className));

            OutputStream os = null;

            try {
                int idx = className.lastIndexOf('.');
                String pkg = idx > 0 ? className.substring(0, idx) : "";
                String cls = idx > 0 ? className.substring(idx+1, className.length()) : className;
                String s = pkg+"|"+cls+"|"+methodName+"|"+methodSignature+"\n";
                os = new FileOutputStream(ztxLog, true);
                os.write(s.getBytes());
            } catch (Exception e) {
                log.error("Cannot write to file: " + ztxLog, e);
            } finally {
                try {
                    if (os != null) os.close();
                } catch (IOException e) {
                    log.error("Cannot close file: " + ztxLog, e);
                }
            }
        }
    }

    public synchronized boolean isExcluded(int mid) {
        return mids.get(mid);
    }

    public List<SpyMatcher> getMatchers() {
        return patternMatcherSet.getMatchers();
    }

    public PatternMatcherSet getPatternMatcherSet() {
        return patternMatcherSet;
    }

    public synchronized void setPatternMatcherSet(PatternMatcherSet newSet) {
        this.patternMatcherSet = newSet;
    }
}
