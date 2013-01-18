/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.spy;

import static com.jitlogic.zorka.spy.SpyMatcher.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SpyMatcherSet {

    private List<SpyMatcher> matchers = new ArrayList<SpyMatcher>();


    public SpyMatcherSet() {

    }

    public SpyMatcherSet(SpyMatcher...matchers) {
        for (SpyMatcher matcher : matchers) {
            this.matchers.add(matcher);
        }
    }

    public SpyMatcherSet(SpyMatcherSet orig, SpyMatcher...matchers) {
        this.matchers.addAll(orig.matchers);
        for (SpyMatcher matcher : matchers) {
            this.matchers.add(matcher);
        }
    }



    /**
     * Checks whenever class name matches any of included matchers. If match result is undecided
     * (eg. some matchers use interface or class annotations), also returns true.
     *
     * This method is used by class transformer to decide whenever
     *
     * @param className
     *
     * @return
     */
    public boolean classMatch(String className) {
        for (SpyMatcher matcher : matchers) {
            int flags = matcher.getFlags();
            if ((0 != (flags & BY_CLASS_NAME)) && match(matcher.getClassPattern(), className)) {
                // Return true or false depending on whether this is normal or inverted match
                return 0 == (flags & EXCLUDE_MATCH);
            }

            if (0 != (flags & (BY_CLASS_ANNOTATION|BY_INTERFACE|BY_METHOD_ANNOTATION))) {
                return true;
            }
        }

        return false;
    }


    public boolean methodMatch(String className, List<String> classAnnotations, List<String> classInterfaces,
        int access, String methodName, String methodSignature, List<String> methodAnnotations) {

        for (SpyMatcher matcher : matchers) {
            int flags = matcher.getFlags();

            if ((0 != (flags & BY_CLASS_NAME) && match(matcher.getClassPattern(), className))
             || (0 != (flags & BY_CLASS_ANNOTATION) && match(matcher.getClassPattern(), classAnnotations))
             || (0 != (flags & BY_INTERFACE) && match(matcher.getClassPattern(), classInterfaces))) {

                if ((0 != (flags & NO_CONSTRUCTORS) && ("<init>".equals(methodName) || "<clinit>".equals(methodName)))
                 || (0 != (flags & NO_ACCESSORS) && isAccessor(methodName, methodSignature))
                 || (0 != (flags & NO_COMMONS) && COMMON_METHODS.contains(methodName))) {
                    return false;
                }

                // Method access-name-signature check
                if ((0 == matcher.getAccess() || 0 != (access & matcher.getAccess()))
                 && (0 == (flags & BY_METHOD_NAME) || match(matcher.getMethodPattern(), methodName))
                 && (0 == (flags & BY_METHOD_SIGNATURE) || match(matcher.getSignaturePattern(), methodSignature))
                 && (0 == (flags & BY_METHOD_ANNOTATION) || methodAnnotations == null
                        || match(matcher.getMethodPattern(), methodAnnotations))) {
                    // Return true or false depending on whether this is normal or inverted match
                    return true;
                }

                // Method annotation is undecidable at this point, always return true
                if (0 != (flags & BY_METHOD_ANNOTATION) && methodAnnotations == null) {
                    return true;
                }

            }
        }

        return false;
    }


    private boolean isAccessor(String methodName, String methodSignature) {
        return (methodName.startsWith("set") && methodSignature.endsWith(")V"))
            || (methodName.startsWith("get") && methodSignature.startsWith("()"))
            || (methodName.startsWith("is") && methodSignature.startsWith("()"))
            || (methodName.startsWith("has") && methodSignature.startsWith("()"));
    }



    public boolean hasMethodAnnotations() {
        for (SpyMatcher matcher : matchers) {
            if (0 != (matcher.getFlags() & SpyMatcher.BY_METHOD_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }



    private boolean match(Pattern pattern, String candidate) {
        return pattern.matcher(candidate).matches();
    }


    private boolean match(Pattern pattern, List<String> candidates) {
        for (String candidate : candidates) {
            if (pattern.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }


    public void  include(SpyMatcher...matchers) {

        for (SpyMatcher matcher : matchers) {
            this.matchers.add(matcher);
        }

    }

}
