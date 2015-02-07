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

package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents exception in symbolic form (suitable to be saved into trace file
 * and restored by trace reader without need to actually have this exact exception
 * class in trace reader classpath).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SymbolicException implements SymbolicRecord, Serializable {


    /**
     * Exception class name symbol ID
     */
    private int classId;


    /**
     * Exception message text
     */
    private String message;


    /**
     * Stack trace consists of (class-name),(method-name),(line-number) triples.
     */
    private SymbolicStackElement[] stackTrace;


    /**
     * Cause (inner exception)
     */
    private SymbolicException cause;


    /**
     * Creates new symbolic exception object.
     *
     * @param classId class ID
     * @param message error message
     */
    public SymbolicException(int classId, String message, SymbolicStackElement[] stackTrace, SymbolicException cause) {
        this.classId = classId;
        this.message = message;
        this.stackTrace = ZorkaUtil.copyArray(stackTrace);
        this.cause = cause;
    }


    /**
     * Creates new symbolic exception object from local throwable object.
     *
     * @param exception
     * @param symbols
     */
    private SymbolicException(Throwable exception, SymbolRegistry symbols, SymbolicException cause) {
        init(exception, symbols);

        if (cause != null) {
            this.cause = cause;
        }
    }

    public SymbolicException(Throwable exception, SymbolRegistry symbols, boolean withCause) {
        init(exception, symbols);

        if (exception.getCause() != null && withCause) {
            this.cause = new SymbolicException(exception.getCause(), symbols, null);
        }
    }

    private void init(Throwable exception, SymbolRegistry symbols) {
        this.classId = symbols.symbolId(exception.getClass().getName());
        this.message = exception.getMessage();

        StackTraceElement[] orig = exception.getStackTrace();

        if (orig != null && orig.length > 0) {
            stackTrace = new SymbolicStackElement[orig.length];
            for (int i = 0; i < orig.length; i++) {
                stackTrace[i] = new SymbolicStackElement(orig[i], symbols);
            }
        } else {
            stackTrace = new SymbolicStackElement[0];
        }
    }


    public int getClassId() {
        return classId;
    }


    public String getMessage() {
        return message;
    }


    @Override
    public int hashCode() {
        return 31 * classId + (message != null ? message.hashCode() : 17);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SymbolicException) {
            SymbolicException sex = (SymbolicException) obj;

            if (sex.classId != classId || !ZorkaUtil.objEquals(sex.message, message) ||
                    !ZorkaUtil.objEquals(sex.cause, cause) || sex.stackTrace.length != stackTrace.length) {
                return false;
            }

            for (int i = 0; i < stackTrace.length; i++) {
                if (!ZorkaUtil.objEquals(sex.stackTrace[i], stackTrace[i])) {
                    return false;
                }
            }

            return true;

        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "" + classId + ": " + message;
    }


    public SymbolicException getCause() {
        return cause;
    }


    public SymbolicStackElement[] getStackTrace() {
        return stackTrace;
    }


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        classId = checker.checkSymbol(classId, this);

        for (SymbolicStackElement el : stackTrace) {
            el.traverse(checker);
        }

        if (cause != null) {
            cause.traverse(checker);
        }
    }
}
