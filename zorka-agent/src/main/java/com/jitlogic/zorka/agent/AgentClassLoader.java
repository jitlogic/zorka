package com.jitlogic.zorka.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AgentClassLoader extends URLClassLoader {

    public AgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
