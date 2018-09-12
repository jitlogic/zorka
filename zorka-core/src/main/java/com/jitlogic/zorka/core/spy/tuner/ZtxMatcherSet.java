package com.jitlogic.zorka.core.spy.tuner;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.BitVector;
import com.jitlogic.zorka.core.spy.PatternMatcherSet;
import com.jitlogic.zorka.core.spy.SpyMatcher;
import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZtxMatcherSet implements SpyMatcherSet {

    private final static Logger log = LoggerFactory.getLogger(ZtxMatcherSet.class);

    private volatile BitVector cids;
    private volatile BitVector mids;

    private SymbolRegistry registry;

    private PatternMatcherSet patternMatcherSet;

    private CidsMidsZtxReader reader;

    private List<String> ztxPkgs  = new ArrayList<String>();
    private List<String> ztxPaths = new ArrayList<String>();

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

    public ZtxMatcherSet(File ztxDir, File ztxLog, SymbolRegistry registry, boolean initExcl) {
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

        scanZtxDir();
        scanZtxClasspath();

        if (!ztxDir.getPath().equals(ztxLog.getParent()) && ztxLog.isFile()) {
            loadZtx(ztxLog);
        }
    }

    @Override
    public boolean classMatch(String className) {
        int classId = registry.symbolId(className);

        return patternMatcherSet.classMatch(className) && !(cids.get(classId) || (probe(className) && cids.get(classId)));
    }

    @Override
    public boolean classMatch(Class<?> clazz, boolean matchMethods) {
        String className = clazz.getName();
        int classId = registry.symbolId(className);

        return patternMatcherSet.classMatch(className) && !(cids.get(classId) || (probe(className) && cids.get(classId)));
    }

    @Override
    public boolean methodMatch(String cn, List<String> sc, List<String> ca, List<String> ci, int acc,
                               String mn, String ms, List<String> ma) {

        boolean pm = patternMatcherSet.methodMatch(cn, sc, ca, ci, acc, mn, ms, ma);
        boolean mm = !mids.get(registry.methodId(cn, mn, ms));
        return pm && mm;
    }

    @Override
    public void clear() {
        cids.reset();
        mids.reset();
        patternMatcherSet.clear();
    }


    public void include(SpyMatcher... includes) {
        patternMatcherSet = patternMatcherSet.include(includes);
    }


    private void scanZtxDir() {
        String[] lst = ztxDir.list();
        if (lst != null) {
            for (String fname : lst) {
                File f = new File(ztxDir, fname);
                if (f.isFile() && fname.endsWith(".ztx")) {
                    if (fname.startsWith("_")) {
                        loadZtx("file:"+f.getPath());
                    } else {
                        ztxPkgs.add(fname.substring(0, fname.length()-4));
                        ztxPaths.add("file:"+f.getPath());
                    }
                }
            }
        }
    }


    private void scanZtxClasspath() {
        InputStream is = getClass().getResourceAsStream("/com/jitlogic/zorka/ztx/pkgs.lst");

        if (is != null) {
            try {
                BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
                for (String pkg = rdr.readLine(); pkg != null; pkg = rdr.readLine()) {
                    if (pkg.trim().length() > 0) {
                        ztxPkgs.add(pkg);
                        ztxPaths.add("classpath:/com/jitlogic/zorka/ztx/" + pkg.trim() + ".ztx");
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

    private void loadZtx(File f) {
        loadZtx("file:"+f.getPath());
    }

    private void loadZtx(String path) {
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


    private boolean probe(String className) {

        List<Integer> found = new ArrayList<Integer>();

        synchronized (this) {
            for (int i = 0; i < ztxPkgs.size(); i++) {
                if (className.startsWith(ztxPkgs.get(i))) {
                    found.add(i);
                    loadZtx(ztxPaths.get(i));
                }
            }
        }

        for (int i = found.size()-1; i >= 0; i--) {
            ztxPkgs.remove(i);
            ztxPaths.remove(i);
        }

        return found.size() > 0;
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
                String cls = idx > 0 ? className.substring(idx+1, className.length()-1) : className;
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

    public List<SpyMatcher> getMatchers() {
        return patternMatcherSet.getMatchers();
    }

    public PatternMatcherSet getPatternMatcherSet() {
        return patternMatcherSet;
    }

    public void setPatternMatcherSet(PatternMatcherSet newSet) {
        this.patternMatcherSet = newSet;
    }
}
