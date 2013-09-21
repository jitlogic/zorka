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
package com.jitlogic.zico.client.portal;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.api.AdminApi;
import com.jitlogic.zico.client.panel.PanelFactory;
import com.sencha.gxt.widget.core.client.Portlet;
import com.sencha.gxt.widget.core.client.button.ToolButton;
import com.sencha.gxt.widget.core.client.container.PortalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Provider;
import java.util.Map;

public class WelcomePanel implements IsWidget {

    private PortalLayoutContainer portal;

    private AdminApi adminApi;
    private Provider<ZicoShell> shell;

    private SystemInfoPortlet systemInfoPortlet;
    private PanelFactory panelFactory;

    private ErrorHandler errorHandler;

    @Inject
    public WelcomePanel(AdminApi adminApi, SystemInfoPortlet systemInfoPortlet,
                        PanelFactory panelFactory, Provider<ZicoShell> shell,
                        ErrorHandler errorHandler) {

        this.adminApi = adminApi;
        this.systemInfoPortlet = systemInfoPortlet;
        this.panelFactory = panelFactory;
        this.shell = shell;
        this.errorHandler = errorHandler;
    }

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

        createHelpPortlet();

        //Portlet wndTopHosts = newPortlet("Top Hosts", false);
        //wndTopHosts.add(new HTML("TBD"));
        //portal.add(wndTopHosts, 1);

        //Portlet wndTopOffenders = newPortlet("Top Offenders", false);
        //wndTopOffenders.add(new HTML("TBD"));
        //portal.add(wndTopOffenders, 1);

        portal.add(systemInfoPortlet, 2);

        createAdminPortlet();
    }

    private void createHelpPortlet() {
        Portlet wndHelp = newPortlet("Welcome", true);
        HTML htmlHelp = new HTML(Resources.INSTANCE.tipsHtml().getText());
        VerticalLayoutContainer vp = new VerticalLayoutContainer();
        vp.add(htmlHelp);
        wndHelp.add(vp);
        portal.add(wndHelp, 0);
    }

    private void createAdminPortlet() {
        Portlet wndAdmin = newPortlet("Admin tasks", false);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        Hyperlink lnkTraceDisplayTemplates = new Hyperlink("Trace List Display Templates", "");
        vp.add(lnkTraceDisplayTemplates);

        lnkTraceDisplayTemplates.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openTemplatePanel();
            }
        }, ClickEvent.getType());

        //Hyperlink lnkUsersAccess = new Hyperlink("Users & Access Privileges", "");
        //vp.add(lnkUsersAccess);

        wndAdmin.add(vp);
        portal.add(wndAdmin, 2);
    }

    private void openTemplatePanel() {
        adminApi.getTidMap(new MethodCallback<Map<String, String>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                errorHandler.error("Error calling method: " + method, exception);
            }

            @Override
            public void onSuccess(Method method, Map<String, String> response) {
                shell.get().addView(panelFactory.traceTemplatePanel(response), "Templates");
            }
        });
    }

    private Portlet newPortlet(String title, boolean closeable) {
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
