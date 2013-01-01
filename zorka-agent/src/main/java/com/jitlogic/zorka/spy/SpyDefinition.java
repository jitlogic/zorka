/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;


import com.jitlogic.zorka.spy.probes.SpyProbe;
import com.jitlogic.zorka.spy.probes.SpyTimeProbe;
import com.jitlogic.zorka.spy.processors.SpyProcessor;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;

import static com.jitlogic.zorka.api.SpyLib.*;

/**
 * This class defines mini-DSL for configuring instrumentation. Language allows for
 * choosing classes and methods to instrument, extracting parameters, return values,
 * transforming/filtering intercepted values and presenting them via JMX in various
 * ways.
 *
 * TODO better description of this class
 *
 */
public class SpyDefinition {

    private static final List<SpyProcessor> EMPTY_XF =
            Collections.unmodifiableList(Arrays.asList(new SpyProcessor[0]));
    private static final List<SpyMatcher> EMPTY_MATCHERS =
            Collections.unmodifiableList(Arrays.asList(new SpyMatcher[0]));
    private static final List<SpyProbe> EMPTY_AF =
            Collections.unmodifiableList(Arrays.asList(new SpyProbe[0]));

    private List<SpyProbe>[] probes;
    private List<SpyProcessor>[] processors;

    private List<SpyMatcher> matchers = EMPTY_MATCHERS;

    public static SpyDefinition instrument() {
        return new SpyDefinition()
            .onEnter(new SpyTimeProbe("T1"))
            .onReturn(new SpyTimeProbe("T2"))
            .onError(new SpyTimeProbe("T2"))
            .onEnter();
    }

    public static SpyDefinition instance() {
        return new SpyDefinition();
    }

    public SpyDefinition() {

        probes = new List[4];
        for (int i = 0; i < probes.length; i++) {
            probes[i] = EMPTY_AF;
        }

        processors = new List[4];
        for (int i = 0; i < processors.length; i++) {
            processors[i] = EMPTY_XF;
        }
    }


    private SpyDefinition(SpyDefinition orig) {
        this.probes = ZorkaUtil.copyArray(orig.probes);
        this.processors = ZorkaUtil.copyArray(orig.processors);
        this.matchers = orig.matchers;
    }


    /**
     * Returns list of probe definitions from particular stage
     *
     * @param stage stage we're interested in
     *
     * @return list of probes defined for this stage
     */
    public List<SpyProbe> getProbes(int stage) {
        return probes[stage];
    }


    /**
     * Returns list of processors for a particular stage.
     *
     * @param stage
     *
     * @return
     */
    public List<SpyProcessor> getProcessors(int stage) {
        return processors[stage];
    }


    /**
     * Returns true if given class name matches this sdef.
     *
     *
     * @param classCandidates
     *
     * @return
     */
    public boolean match(List<String> classCandidates) {

        for (SpyMatcher matcher : matchers) {
            if (matcher.matches(classCandidates)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return true this spy def contains class annotation matcher(s).
     *
     * @return true if any of matchers contains class annotation match
     */
    public boolean hasClassAnnotation() {
        for (SpyMatcher matcher : matchers) {
            if (matcher.hasClassAnnotation()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this spy def contains method annotation matcher(s).
     *
     * @return true if any of matchers contains method annotation match
     */
    public boolean hasMethodAnnotation() {
        for (SpyMatcher matcher : matchers) {
            if (matcher.hasMethodAnnotation()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if given method (of given class) matches this spy definition.
     * Note that method signature and access bits are also checked.
     *
     *
     * @param classCandidates
     *
     * @param methodName method name
     *
     * @param methodDesc method descriptor (as in classfile)
     *
     * @param access access bits (as in classfile)
     *
     * @return true if all arguments match properly.
     */
    public boolean match(List<String> classCandidates, String methodName, String methodDesc, int access) {

        for (SpyMatcher matcher : matchers) {
            if (matcher.matches(classCandidates, methodName, methodDesc, access)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns list of matchers declared SpyDefinition.
     *
     * @return list of matchers
     */
    public List<SpyMatcher> getMatchers() {
        return matchers;
    }


    /**
     * Instructs spy what should be collected at the beginning of a method.
     *
     * @return
     */
    public SpyDefinition onEnter(SpyDefArg...args) {
        return with(ON_ENTER, args);
    }


    /**
     * Instructs spy what should be collected at the end of a method.
     *
     * @return
     */
    public SpyDefinition onReturn(SpyDefArg...args) {
        return with(ON_RETURN, args);
    }


    /**
     * Instructs spy what should be collected at exception handling code of a method.
     *
     * @return
     */
    public SpyDefinition onError(SpyDefArg...args) {
        return with(ON_ERROR, args);
    }


    /**
     * Instructs spy that subsequent transforms will be executed at data submission
     * point.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onSubmit(SpyDefArg...args) {
        return with(ON_SUBMIT, args);
    }


    /**
     * Instructs spy what method to include.
     *
     * @param matchers
     *
     * @return
     */
    public SpyDefinition include(SpyMatcher...matchers) {
        SpyDefinition sdef = new SpyDefinition(this);
        List<SpyMatcher> lst = new ArrayList<SpyMatcher>(sdef.matchers.size()+1+matchers.length);
        lst.addAll(sdef.matchers);
        for (SpyMatcher matcher : matchers) {
            lst.add(matcher);
        }
        sdef.matchers = lst;
        return sdef;
    }


    /**
     * Declares which arguments should be fetched by instrumenting code.
     * This method should generally be called once as arguments are fetched
     * only at one place of a method (beginning or end - depending on
     * whether instrumentation will actually run at the beginning or at the end of
     * method.
     *
     * <p>For instance methods first argument will have index 1 and
     *    instance reference will be present at index 0. </p>
     * <p>For static methods arguments start with 0. </p>
     *
     * @param args argument indexes to be fetched (or class names if
     *             class objects are to be fetched)
     *
     * @return spy definition with augmented fetched argument list;
     */
    private SpyDefinition with(int curStage, SpyDefArg...args) {
        SpyDefinition sdef = new SpyDefinition(this);

        List<SpyProbe> newProbes = new ArrayList<SpyProbe>(sdef.probes[curStage].size()+args.length+2);
        newProbes.addAll(sdef.probes[curStage]);

        List<SpyProcessor> newProcessors = new ArrayList<SpyProcessor>(sdef.processors[curStage].size()+args.length+2);
        newProcessors.addAll(sdef.processors[curStage]);

        for (Object arg : args) {
            if (arg instanceof SpyProcessor) {
                newProcessors.add((SpyProcessor)arg);
            } else if (arg instanceof SpyProbe) {
                newProbes.add((SpyProbe)arg);
            } else {
                throw new IllegalArgumentException();
            }
        }

        sdef.probes = ZorkaUtil.copyArray(probes);
        sdef.probes[curStage] = Collections.unmodifiableList(newProbes);

        sdef.processors = ZorkaUtil.copyArray(processors);
        sdef.processors[curStage] = Collections.unmodifiableList(newProcessors);

        return sdef;
    }


    // TODO toString() method

    // TODO some method printing 'execution plan' of this SpyDef
}
