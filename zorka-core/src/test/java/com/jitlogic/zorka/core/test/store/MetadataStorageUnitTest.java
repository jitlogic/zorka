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

package com.jitlogic.zorka.core.test.store;

import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.store.*;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;

public class MetadataStorageUnitTest extends ZorkaFixture {

    private SymbolRegistry sreg1, sreg2;
    private MetricsRegistry mreg1, mreg2;
    private File file;

    @Before
    public void setUp() {
        file = new File(getTmpDir(), "symbols.db");
    }

    @After
    public void tearDown() {
        if (sreg1 != null) {
            sreg1.close(); sreg1 = null;
        }
        if (sreg2 != null) {
            sreg2.close(); sreg2 = null;
        }
    }

    @Test
    public void testCreateWriteReadSymbols() {
        sreg1 = new SymbolRegistry(file);
        sreg1.put(1, "oja!");
        sreg1.close();

        sreg2 = new SymbolRegistry(file);
        assertThat(sreg2.size()).isEqualTo(1);
        assertThat(sreg2.symbolName(1)).isEqualTo("oja!");

    }


    @Test
    public void testAllocateTwoSymbolsAndReload() {
        sreg1 = new SymbolRegistry(file);
        int oja = sreg1.symbolId("oja!");
        int uja = sreg1.symbolId("uja!");
        sreg1.close();

        sreg2 = new SymbolRegistry(file);
        assertThat(sreg2.symbolName(oja)).isEqualTo("oja!");
        assertThat(sreg2.symbolName(uja)).isEqualTo("uja!");
        sreg2.close();
    }



    @Test
    public void testCreateSaveLoadMetricTemplate() throws Exception {

        MetricTemplate mt = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        mreg1 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        int id = mreg1.getTemplate(mt).getId();
        mreg1.close();

        MetricsRegistry mreg2 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        MetricTemplate mt2 = mreg2.getTemplate(id);
        mreg2.close();

        assertThat(mt2).isNotNull();
        assertThat(mt2.getType()).isEqualTo(MetricTemplate.RAW_DATA);
        assertThat(mt2.getName()).isEqualTo("test");
        assertThat(mt2.getNomField()).isNull();
        assertThat(mt2.getDivField()).isNull();

        assertThat(mt2).isEqualTo(mt);
    }


    @Test
    public void testOpenCloseTwiceAndCheckIfMetricTemplateFileDoesNotGrow() throws Exception {
        MetricTemplate mt = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        mreg1 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        int id = mreg1.getTemplate(mt).getId();
        mreg1.close();


        mreg2 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        assertThat(mreg2.getTemplate(id)).isNotNull();
        mreg2.close();
    }


    @Test
    public void testMetricTemplateHashingFn() {
        MetricTemplate mt1 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);
        MetricTemplate mt2 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        assertThat(mt2).isEqualTo(mt1);

        Map<MetricTemplate, MetricTemplate> map = new HashMap<MetricTemplate, MetricTemplate>();
        map.put(mt1, mt1);

        assertThat(map.get(mt2)).isNotNull();
    }


    @Test
    public void testSaveLoadMetricTemplateAndCheckIfRedefinedObjectMatchesOriginal() throws Exception {
        MetricTemplate mt1 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        MetricsRegistry mreg1 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        int id = mreg1.getTemplate(mt1).getId();
        mreg1.close();

        MetricTemplate mt2 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        MetricsRegistry mreg2 = new MetricsRegistry(new File(getTmpDir(), "metrics.db"));
        int id2 = mreg2.getTemplate(mt2).getId();
        mreg2.close();

        assertThat(id2).isEqualTo(id);
    }

    public void testSaveLoadMetricAndItsTemplate() {
        // TODO
    }


}
