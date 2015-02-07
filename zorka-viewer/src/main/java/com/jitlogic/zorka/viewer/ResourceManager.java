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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * Manages access to various resources contained in viewer jar file.
 * Currently only icons are supported but other resource types can
 * be added as well.
 */
public class ResourceManager {

    /** Used to load files from classpath */
    private final Class<?> clazz;

    /** Creates new resource manager. Marked private to prevent instantiation of this object by client code. */
    private ResourceManager(){
        this.clazz = this.getClass();
    }
    
    /** Icon cache - contains references to all loaded icons. */
    private Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();


    /**
     * Returns icon of given name and dimensions. A file with the same name
     * and .png extension has to exist somewhere in /icons/WWxHH in classpath.
     * If no icon of specified size has been found, method will try to find
     * icon of generic size and scale it.
     *
     * @param name icon name
     *
     * @param w width
     *
     * @param h height
     *
     * @return icon object
     */
    private synchronized ImageIcon getIcon(String name, int w, int h) {
    	String key = "" + w + "x" + h + ":" + name;
    	
    	if (!iconCache.containsKey(key)) {
    		URL url = clazz.getResource("/icons/" + w + "x" + h + "/" + name + ".png");
    		if (url != null) {
    			iconCache.put(key, new ImageIcon(url));
    		}
    	}
    	
    	if (!iconCache.containsKey(key)) {
    		ImageIcon icon = new ImageIcon(clazz.getResource("/icons/" + name + ".png"));
    		if (icon.getIconWidth() == w && icon.getIconHeight() == h) {
    			iconCache.put(key, icon);
    		} else {
    			Image img = icon.getImage();
    			BufferedImage bi = new BufferedImage(w, h, 
    				BufferedImage.TYPE_INT_ARGB);
    			Graphics g = bi.createGraphics();
    			g.drawImage(img, 0, 0, w, h, null);
    			iconCache.put(key, new ImageIcon(bi));
    		}
    	}
    	
    	return iconCache.get(key);
    }

    /** Singleton instance of resource manager */
	private static ResourceManager resourceManager;


    /**
     * Returns instance of resource manager. Creates new instance if necessary.
     *
     * @return resource manager instance
     */
    public static synchronized ResourceManager getInstance() {
    	if (null == resourceManager)
    		resourceManager = new ResourceManager();
    	return resourceManager;
    }


    /**
     * Returns standard 16x16 icon.
     *
     * @param name icon name
     *
     * @return icon object.
     */
    public static ImageIcon getIcon16x16(String name) {
    	return getInstance().getIcon(name, 16, 16);
    }
    
    public static ImageIcon getIcon12x12(String name) {
        return getInstance().getIcon(name, 12, 12);
    }
}
