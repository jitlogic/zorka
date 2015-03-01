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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.util.ZorkaUtil;

/**
 * Spy context links instrumented methods with spy definitions. New spy context
 * is created for each spy def for each instrumented method. Spy probes send
 * spy context ID to submitter that identifies proper context and attaches it to
 * created records prior to processing.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyContext {

    /**
     * Context ID
     */
    private Integer id;

    /**
     * Spy definition. Eventual consistency is everything we need here.
     */
    private volatile SpyDefinition spyDefinition;

    /**
     * Full name of instrumented class
     */
    private final String className;

    /**
     * Short name of instrumented class
     */
    private final String shortClassName;

    /**
     * Package name of instrumented class
     */
    private final String packageName;

    /**
     * Name of instrumented method
     */
    private final String methodName;

    /**
     * Signature of instrumented method
     */
    private final String methodSignature;

    /**
     * Access flags of instrumented method
     */
    private final int access;

    /**
     * Creates new spy context
     *
     * @param spyDefinition   spy definition
     * @param className       name of instrumented class
     * @param methodName      name of instrumented method
     * @param methodSignature signature of instrumented method
     * @param access          access flags of instrumented method
     */
    public SpyContext(SpyDefinition spyDefinition, String className,
                      String methodName, String methodSignature, int access) {

        this.spyDefinition = spyDefinition;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.access = access;

        String[] segs = className.split("\\.");
        this.shortClassName = segs[segs.length - 1];
        this.packageName = segs.length > 1 ? ZorkaUtil.join(".", ZorkaUtil.clipArray(segs, segs.length - 1)) : "";
    }

    /**
     * Returns context ID
     *
     * @return context ID
     */
    public Integer getId() {
        return id;
    }


    /**
     * Sets context ID
     *
     * @param id context ID
     */
    public void setId(Integer id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    /**
     * Returns spy definition
     *
     * @return spy definition object
     */
    public SpyDefinition getSpyDefinition() {
        return spyDefinition;
    }


    public void setSpyDefinition(SpyDefinition sdef) {
        this.spyDefinition = sdef;
    }


    /**
     * Returns name of instrumented class
     *
     * @return full name of instrumented class
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns short name of instrumented class
     *
     * @return short name of instrumented class
     */
    public String getShortClassName() {
        return shortClassName;
    }

    /**
     * Returns name of instrumented method
     *
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns package name of instrumented class
     *
     * @return package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Formats string by filling it with spy context information.
     * <p/>
     * TODO get rid of it, use standard record accessors ("${__CTX__.className}" etc.)
     *
     * @param template string template
     * @return formatted string
     */
    public String subst(String template) {
        return template
                .replace("${className}", className)
                .replace("${methodName}", methodName)
                .replace("${shortClassName}", shortClassName)
                .replace("${packageName}", packageName);
    }

    @Override
    public int hashCode() {
        return spyDefinition.hashCode() + className.hashCode() +
                methodName.hashCode() + methodSignature.hashCode() + access;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SpyContext)) {
            return false;
        }

        SpyContext ic = (SpyContext) obj;

        return spyDefinition.equals(ic.spyDefinition) &&
                className.equals(ic.className) && methodName.equals(ic.methodName) &&
                methodSignature.equals(ic.methodSignature) && access == ic.access;
    }
}
