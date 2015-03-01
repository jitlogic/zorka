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
package com.jitlogic.zorka.core.test.perfmon.support;

import com.jitlogic.zorka.core.perfmon.RankLister;

import java.util.*;

public class TestRankLister implements RankLister<TestRankItem> {

    private String[] metrics, averages;
    private List<TestRankItem> rankList;

    public TestRankLister(String metric, String average, double...data) {
        this(new String[] { metric }, new String[] { average }, data);
    }

    public TestRankLister(String[] metrics, String[] averages, double...data) {
        this.metrics = metrics;
        this.averages = averages;
        init(data);
    }

    public void init(double...data) {
        int len = metrics.length * averages.length;

        this.rankList = new ArrayList<TestRankItem>();

        for (int i = 0; i < data.length; i += len) {
            rankList.add(new TestRankItem(metrics, averages, data, i));
        }
    }

    public List<TestRankItem> list() {
        List<TestRankItem> lst = new ArrayList<TestRankItem>(rankList.size()+2);
        lst.addAll(rankList);
        return lst;
    }
}
