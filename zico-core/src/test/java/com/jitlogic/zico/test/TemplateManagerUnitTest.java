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
package com.jitlogic.zico.test;

import com.jitlogic.zico.core.TraceTemplateManager;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zico.test.support.ZicoFixture;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

public class TemplateManagerUnitTest extends ZicoFixture {

    private TraceTemplateManager templateManager;


    @Before
    public void acquireApplicationObjects() {
        templateManager = injector.getInstance(TraceTemplateManager.class);
    }


    private TraceTemplate tti(int order, String condition, String templ) {
        TraceTemplate tti = new TraceTemplate();
        tti.setOrder(order);
        tti.setCondition(condition);
        tti.setTemplate(templ);
        return tti;
    }


    @Test
    public void testInsertNewTemplateRecord() throws Exception {
        TraceTemplate t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");

        t1.setId(systemService.saveTemplate(t1));
        assertTrue("TEMPLATE_ID should be assigned by database and > 0", t1.getId() > 0);

        List<TraceTemplate> lst = systemService.listTemplates();
        assertEquals(1, lst.size());
        assertEquals(t1.getId(), lst.get(0).getId());
    }


    @Test
    public void testInsertReopenAndReadTemplate() throws Exception {
        TraceTemplate t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");

        t1.setId(systemService.saveTemplate(t1));

        templateManager.close();
        templateManager.open();

        List<TraceTemplate> lst = systemService.listTemplates();
        assertEquals(1, lst.size());
    }


    @Test
    public void testInsertAndModifyNewTemplate() {
        TraceTemplate t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");
        t1.setId(systemService.saveTemplate(t1));


        t1.setTemplate("some.Class.findKey(${ARG0})");
        systemService.saveTemplate(t1);

        List<TraceTemplate> lst = systemService.listTemplates();
        assertEquals(1, lst.size());
        assertEquals(t1.getId(), lst.get(0).getId());
    }


    @Test
    public void testAddRemoveTemplate() {
        TraceTemplate t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");
        t1.setId(systemService.saveTemplate(t1));

        systemService.removeTemplate(t1.getId());
        assertEquals(0, systemService.listTemplates().size());
    }


    @Test
    public void testSearchForEmptyTraceIdMap() {
        assertThat(systemService.getTidMap(null)).hasSize(0);
    }


    @Test
    public void testExportImportTemplates() {
        TraceTemplate t1 = tti(0, "METHOD='findKey'", "findKey(${ARG0}");
        int tid = systemService.saveTemplate(t1);

        templateManager.export();
        templateManager.close();

        templateManager.open();

        TraceTemplate t2 = templateManager.find(TraceTemplate.class, tid);
        assertNotNull(t2);
        assertEquals(t1.getTemplate(), t2.getTemplate());
        assertEquals(t1.getOrder(), t2.getOrder());
        assertEquals(t1.getFlags(), t2.getFlags());
        assertEquals(t1.getCondition(), t2.getCondition());
    }

}
