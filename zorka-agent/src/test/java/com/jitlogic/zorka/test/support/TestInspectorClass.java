package com.jitlogic.zorka.test.support;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TestInspectorClass {

    private static int priv = 234;

    public static int count = 123;

    public static int count() {
        return count+3;
    }

    public static int getCount() {
        return count + 2;
    }

}
