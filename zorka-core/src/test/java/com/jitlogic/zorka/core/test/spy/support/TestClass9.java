package com.jitlogic.zorka.core.test.spy.support;

public class TestClass9 {

    private static long N = 10;

    public void run() {
        try {
            step1();
        } catch (Exception e) {
        }
    }

    public void step1() throws Exception {
        step2();
        Thread.sleep(5*N);
    }

    public void step2() throws Exception {
        step3();
    }

    public void step3() throws Exception {
        Thread.sleep(5*N);
        step4();
        Thread.sleep(4*N);
    }

    public void step4() throws Exception {
        step5();
    }

    public void step5() throws Exception {
        Thread.sleep(5*N);
    }

    public void err() {
        try {
            bork3();
        } catch (Exception e) {
        }
    }

    public void err1() throws Exception {
        bork3();
    }

    public void bork3() throws Exception {
        Thread.sleep(2*N);
        try {
            bork2();
        } catch (Exception e) {
            throw new RuntimeException("Borken: ", e);
        }
    }

    public void bork2() throws Exception {
        Thread.sleep(2*N);
        bork1();
    }

    public void bork1() throws Exception {
        Thread.sleep(2*N);
        throw new RuntimeException("BORK!");
    }

}
