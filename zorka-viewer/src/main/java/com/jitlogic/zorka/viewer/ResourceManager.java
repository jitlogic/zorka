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

package com.jitlogic.zorka.viewer;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

public class ResourceManager {
    
    private final Class<?> clazz;
    
    public ResourceManager(){
        this.clazz = this.getClass();
    }
    
    
    private Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();
    
    // TODO zero ochrony przed bledami - dopisac jakies checki + stosowne logi (oraz "awaryjna" ikonke)
    private synchronized ImageIcon getIcon(String name, int w, int h) {
    	String key = "" + w + "x" + h + ":" + name;
    	
    	if (!iconCache.containsKey(key)) {
    		URL url = clazz.getResource("/icons/" + w + "x" + h + "/" + name + ".png");
    		if (url != null) {
    			iconCache.put(key, new ImageIcon(url));
    		}
    	}
    	
    	if (!iconCache.containsKey(key)) {
    		// TODO czy to bedzie dzialac po zapaczkowaniu calego projektu w .jar ??
    		ImageIcon icon = new ImageIcon(clazz.getResource("/icons/" + name + ".png"));
    		if (icon.getIconWidth() == w && icon.getIconHeight() == h) {
    			// Nie potrzeba skalowania
    			iconCache.put(key, icon);
    		} else {
    			// Musimy przeskalowac
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
    
	private static ResourceManager resourceManager;
    
    public static synchronized ResourceManager getInstance() {
    	if (null == resourceManager)
    		resourceManager = new ResourceManager();
    	return resourceManager;
    }
    
    public static ImageIcon getIcon16x16(String name) {
    	return getInstance().getIcon(name, 16, 16);
    }
    

    public static ImageIcon getIcon24x24(String name) {
    	return getInstance().getIcon(name, 24, 24);
    }
    
}
