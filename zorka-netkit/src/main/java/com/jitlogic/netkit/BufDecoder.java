package com.jitlogic.netkit;

public interface BufDecoder extends BufHandler {

    boolean hasError();

    void reset();

    boolean hasInitial();

    boolean hasFinished();

}
