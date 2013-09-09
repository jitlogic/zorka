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
package com.jitlogic.zorka.central.client;


import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sencha.gxt.widget.core.client.Portlet;
import com.sencha.gxt.widget.core.client.button.ToolButton;
import com.sencha.gxt.widget.core.client.container.PortalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;

public class WelcomePanel implements IsWidget {

    private PortalLayoutContainer portal;

    @Override
    public Widget asWidget() {
        if (portal == null) {
            createPortal();
        }
        return portal;
    }

    private void createPortal() {

        portal = new PortalLayoutContainer(3);

        portal.getElement().getStyle().setBackgroundColor("white");
        portal.setColumnWidth(0, .50);
        portal.setColumnWidth(1, .25);
        portal.setColumnWidth(2, .25);

        Portlet wndHelp = configure("Welcome", true);
        wndHelp.add(new HTML(Resources.INSTANCE.tipsHtml().getText()));
        portal.add(wndHelp, 0);

        Portlet wndTopHosts = configure("Top Hosts", true);
        wndTopHosts.add(new HTML("TBD"));
        portal.add(wndTopHosts, 1);

        Portlet wndTopOffenders = configure("Top Offenders", true);
        wndTopOffenders.add(new HTML("TBD"));
        portal.add(wndTopOffenders, 1);

        Portlet wndStatus = configure("Collector status", true);
        wndStatus.add(new HTML("TBD"));
        portal.add(wndStatus, 2);

        Portlet wndAdmin = configure("Admin tasks", true);
        wndAdmin.add(new HTML("TBD"));
        portal.add(wndAdmin, 2);
    }

    private Portlet configure(String title, boolean closeable) {
        final Portlet portlet = new Portlet();
        portlet.setHeadingText(title);
        portlet.setCollapsible(true);
        if (closeable) {
            portlet.getHeader().addTool(new ToolButton(ToolButton.CLOSE, new SelectEvent.SelectHandler() {
                @Override
                public void onSelect(SelectEvent event) {
                    portlet.removeFromParent();
                }
            }));
        }
        return portlet;
    }
}
