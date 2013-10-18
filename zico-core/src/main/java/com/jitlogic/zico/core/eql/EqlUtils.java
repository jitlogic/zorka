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
package com.jitlogic.zico.core.eql;


import com.jitlogic.zico.core.eql.ast.EqlOp;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EqlUtils {

    public static boolean equals(Object obj1, Object obj2) {

        if (obj1 == null && obj2 == null)
            return true;

        if (obj1 == null || obj2 == null)
            return false;

        if (obj1.getClass() == obj2.getClass())
            return obj1.equals(obj2);

        if (obj1 instanceof Number && obj2 instanceof Number) {
            Number n1 = (Number) obj1, n2 = (Number) obj2;

            if (obj1.getClass() == Float.class || obj1.getClass() == Double.class ||
                    obj2.getClass() == Float.class || obj2.getClass() == Double.class) {
                return n1.doubleValue() == n2.doubleValue();
            } else {
                return n1.longValue() == n2.longValue();
            }
        }

        return false;
    }


    public static boolean regex(Object arg1, Object arg2) {
        if (arg1 == null)
            return false;

        return compile(arg2).matcher(arg1.toString()).matches();
    }

    public static Pattern compile(Object arg) {
        if (arg == null) {
            throw new EqlException("Attempt to construct regex pattern from null value.");
        }

        if (arg instanceof Pattern) {
            return (Pattern) arg;
        }

        String s = arg.toString();

        if (s.startsWith("^")) {
            s = s.substring(1, s.length());
        } else {
            s = ".*" + s;
        }

        if (s.endsWith("$")) {
            s = s.substring(0, s.length() - 1);
        } else {
            s = s + ".*";
        }

        try {
            return Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            throw new EqlException("Invalid regex pattern", e);
        }
    }

    public static int compare(Object obj1, Object obj2) {

        if (obj1 == null && obj2 == null)
            return 0;

        if (obj1 == null || obj2 == null)
            return obj1 == null ? -1 : 1;

        if (obj1 instanceof Comparable && obj2 instanceof Comparable) {
            return ((Comparable) obj1).compareTo(obj2);
        }

        throw new EqlException("Objects '" + obj1.getClass() + "' and '" + obj2.getClass() + "' are not comparable");
    }


    public static Object arithmetic(Object obj1, EqlOp op, Object obj2) {

        if (obj1 == null || obj2 == null) {
            if (op == EqlOp.ADD) {
                return obj2 == null ? obj1 : obj2;
            } else {
                throw new EqlException("Arithmetic operation on null values.");
            }
        }

        if (obj1 instanceof Number && obj2 instanceof Number) {
            Number n1 = (Number) obj1, n2 = (Number) obj2;
            if (obj1.getClass() == Float.class || obj1.getClass() == Double.class ||
                    obj2.getClass() == Float.class || obj2.getClass() == Double.class) {
                switch (op) {
                    case ADD:
                        return n1.doubleValue() + n2.doubleValue();
                    case SUB:
                        return n1.doubleValue() - n2.doubleValue();
                    case MUL:
                        return n1.doubleValue() * n2.doubleValue();
                    case DIV:
                        return n1.doubleValue() / n2.doubleValue();
                    case REM:
                        return n1.doubleValue() % n2.doubleValue();
                }
            } else if (obj1.getClass() == Long.class || obj2.getClass() == Long.class) {
                switch (op) {
                    case ADD:
                        return n1.longValue() + n2.longValue();
                    case SUB:
                        return n1.longValue() - n2.longValue();
                    case MUL:
                        return n1.longValue() * n2.longValue();
                    case DIV:
                        return n1.longValue() / n2.longValue();
                    case REM:
                        return n1.longValue() % n2.longValue();
                }
            } else {
                switch (op) {
                    case ADD:
                        return n1.intValue() + n2.intValue();
                    case SUB:
                        return n1.intValue() - n2.intValue();
                    case MUL:
                        return n1.intValue() * n2.intValue();
                    case DIV:
                        return n1.intValue() / n2.intValue();
                    case REM:
                        return n1.intValue() % n2.intValue();
                }
            }
        }

        if (obj1.getClass() == String.class || obj2.getClass() == String.class) {
            switch (op) {
                case ADD:
                    return obj1.toString() + obj2.toString();
            }
        }

        throw new EqlException("Cannot add non-numbers and non-strings.");
    }


    public static boolean logical(Object arg1, EqlOp op, Object arg2) {
        boolean b1 = ZorkaUtil.coerceBool(arg1), b2 = ZorkaUtil.coerceBool(arg2);

        switch (op) {
            case AND:
                return b1 && b2;
            case OR:
                return b1 || b2;
        }

        throw new EqlException("Illegal logical operation: " + op);
    }


    public static Object negate(Object arg) {

        if (arg == null) {
            throw new EqlException("Cannot perform bitwise operation on null value.");
        }

        if (arg instanceof Number && arg.getClass() != Float.class && arg.getClass() != Double.class) {
            if (arg.getClass() == Long.class) {
                return ~((Number) arg).longValue();
            } else {
                return ~(((Number) arg).intValue());
            }
        }

        throw new EqlException("Cannot perform bitwise negation on " + arg.getClass());
    }


    public static Object bitwise(Object arg1, EqlOp op, Object arg2) {

        if (arg1 == null || arg2 == null) {
            throw new EqlException("Cannot perform bitwise operation on null");
        }

        if (arg1 instanceof Number && arg2 instanceof Number) {
            if (arg1.getClass() == Float.class || arg1.getClass() == Double.class
                    || arg2.getClass() == Float.class || arg2.getClass() == Double.class) {
                throw new EqlException("Cannot perform bitwise operation on " + arg1.getClass() + " and " + arg2.getClass());
            }

            Number n1 = (Number) arg1, n2 = (Number) arg2;

            if (arg1.getClass() == Long.class || arg2.getClass() == Long.class) {
                switch (op) {
                    case BIT_AND:
                        return n1.longValue() & n2.longValue();
                    case BIT_OR:
                        return n1.longValue() | n2.longValue();
                    case BIT_XOR:
                        return n1.longValue() ^ n2.longValue();
                }
            } else {
                switch (op) {
                    case BIT_AND:
                        return n1.intValue() & n2.intValue();
                    case BIT_OR:
                        return n1.intValue() | n2.intValue();
                    case BIT_XOR:
                        return n1.intValue() ^ n2.intValue();
                }
            }

            throw new EqlException("Unsupported operation: " + op);

        } else
            throw new EqlException("Cannot perform bitwise operation on " + arg1.getClass() + " and " + arg2.getClass());
    }
}
