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

package com.jitlogic.zorka.viewer;

import java.io.*;
import java.util.Properties;

public class ViewerState {

    public static final String PROP_FILE = ".zorkaViewer.properties";

    public static final String STATE_CWD = "zorka.cwd";

    private Properties props = new Properties();

    public ViewerState() {
        try {
            load();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void load() throws FileNotFoundException {
        props.clear();
        InputStream is = new FileInputStream(
            new File(System.getProperty("user.home"), PROP_FILE));
        try {
            props.load(is);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void save() throws FileNotFoundException {
        OutputStream os = new FileOutputStream(
            new File(System.getProperty("user.home"), PROP_FILE));
        try {
            props.store(os, "Zorka Viewer user config");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public String get(String key, String defval) {
        String rslt = props.getProperty(key, defval);
        if (rslt == null) {
            put(key, defval);
            rslt = defval;
        }

        return rslt;
    }

    public void put(String key, String val) {
        props.put(key, val);
        try {
            save();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
