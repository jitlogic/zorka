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

/**
 *
 */
public class InstrumentationContext {

    private Integer id = null;

    private SpyDefinition spyDefinition;
    private String className;
    private String methodName;
    private String methodDesc;
    private int access;

    public InstrumentationContext(SpyDefinition spyDefinition, String className,
                 String methodName, String methodDesc, int access) {

        this.spyDefinition = spyDefinition;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.access = access;
    }

    public void setId(Integer id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public Integer getId() {
        return id;
    }

    public SpyDefinition getSpyDefinition() {
        return spyDefinition;
    }

    @Override
    public int hashCode() {
        return spyDefinition.hashCode() + className.hashCode() +
            methodName.hashCode() + methodDesc.hashCode() + access;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InstrumentationContext)) {
            return false;
        }

        InstrumentationContext ic = (InstrumentationContext)obj;

        return spyDefinition.equals(ic.spyDefinition) &&
            className.equals(ic.className) && methodName.equals(ic.methodName) &&
            methodDesc.equals(ic.methodDesc) && access == ic.access;
    }
}
