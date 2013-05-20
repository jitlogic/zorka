package com.jitlogic.zorka.console.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;

public class Console implements EntryPoint {
    public void onModuleLoad() {
        RootPanel.get("console").add(new TracePanel());
    }
}
