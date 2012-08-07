package com.jitlogic.zorka.agent.testutil;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TestJmx implements TestJmxMBean {

    public long nom, div;

    public long getNom() {
        return nom;
    }

    public long getDiv() {
        return div;
    }

    public void setNom(long nom) {
        this.nom = nom;
    }

    public void setDiv(long div) {
        this.div = div;
    }
}
