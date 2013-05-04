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

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;

public class MetadataStorageUnitTest extends ZorkaFixture {

    private AsyncFileOutput output;
    private SymbolRegistry sreg;
    private MetricsRegistry mreg;


    private void open(String fname) {
        String path = zorka.path(getTmpDir(), fname);
        output = new AsyncFileOutput(path);
        sreg = new SimpleSymbolRegistry(output);
        mreg = new SimpleMetricsRegistry(output);
        new MetadataLoader(sreg, mreg).load(path);
        method("open").in(output).invoke();
    }


    private void close() {
        Queue queue = field("submitQueue").ofType(Queue.class).in(output).get();

        while (queue.size() > 0) {
            method("runCycle").in(output).invoke();
        }

        method("flush").in(output).invoke();
        method("close").in(output).invoke();

        mreg = null; sreg = null; output = null;
    }

    @Test
    public void testCreateWriteReadSymbols() {
        open("meta.dat");
        sreg.put(1, "oja!");
        close();

        open("meta.dat");
        assertThat(sreg.size()).isEqualTo(1);
        assertThat(sreg.symbolName(1)).isEqualTo("oja!");
        close();
    }


    @Test
    public void testAllocateTwoSymbolsAndReload() {
        open("meta.dat");
        int oja = sreg.symbolId("oja!");
        int uja = sreg.symbolId("uja!");
        close();

        open("meta.dat");
        assertThat(sreg.symbolName(oja)).isEqualTo("oja!");
        assertThat(sreg.symbolName(uja)).isEqualTo("uja!");
        close();
    }


    @Test
    public void testOpenCloseTwiceAndCheckIfSymbolFileDoesNotGrow() {
        open("meta.dat");
        int oja = sreg.symbolId("oja!");
        close();

        String path = zorka.path(getTmpDir(), "meta.dat");
        long len = new File(path).length();

        open("meta.dat");
        sreg.symbolName(oja);
        close();

        assertThat(new File(path).length()).isEqualTo(len);
    }


    @Test
    public void testCreateSaveLoadMetricTemplate() {

        MetricTemplate mt = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        open("meta.dat");
        int id = mreg.getTemplate(mt).getId();
        close();

        open("meta.dat");
        MetricTemplate mt2 = mreg.getTemplate(id);
        close();

        assertThat(mt2).isNotNull();
        assertThat(mt2.getType()).isEqualTo(MetricTemplate.RAW_DATA);
        assertThat(mt2.getName()).isEqualTo("test");
        assertThat(mt2.getNomField()).isNull();
        assertThat(mt2.getDivField()).isNull();

        assertThat(mt2).isEqualTo(mt);
    }


    @Test
    public void testOpenCloseTwiceAndCheckIfMetricTemplateFileDoesNotGrow() {
        MetricTemplate mt = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        open("meta.dat");
        int id = mreg.getTemplate(mt).getId();
        close();

        String path = zorka.path(getTmpDir(), "meta.dat");
        long len = new File(path).length();

        open("meta.dat");
        assertThat(mreg.getTemplate(id)).isNotNull();
        close();

        assertThat(new File(path).length()).isEqualTo(len);
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
    public void testSaveLoadMetricTemplateAndCheckIfRedefinedObjectMatchesOriginal() {
        MetricTemplate mt1 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        open("meta.dat");
        int id = mreg.getTemplate(mt1).getId();
        close();

        MetricTemplate mt2 = new MetricTemplate(MetricTemplate.RAW_DATA, "test", "B", null, null);

        open("meta.dat");
        int id2 = mreg.getTemplate(mt2).getId();
        close();

        assertThat(id2).isEqualTo(id);
    }

    public void testSaveLoadMetricAndItsTemplate() {
        // TODO
    }


}
