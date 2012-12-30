/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.rankproc;

import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankList<T extends Rankable<?>> implements RankLister<T> {

    private final RankLister<T> lister;
    private final int metric, average, maxSize;
    private final long rerankTime, lastTime = 0L;

    private int numReranks = 0;

    private List<T> rankList;

    private final ZorkaUtil util = ZorkaUtil.getInstance();

    public RankList(RankLister<T> lister, int maxSize, int metric, int average, long rerankTime) {
        this.lister = lister;
        this.maxSize = maxSize;
        this.metric = metric;
        this.average = average;
        this.rerankTime = rerankTime;
    }


    public synchronized T get(int i) {

        if (util.currentTimeMillis() > lastTime + rerankTime) {
            rerank(util.currentTimeMillis());
        }

        try {
            return rankList.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

    }


    public synchronized List<T> list() {

        if (util.currentTimeMillis() > lastTime + rerankTime) {
            rerank(util.currentTimeMillis());
        }

        return rankList;
    }


    public synchronized int size() {
        long tstamp = util.currentTimeMillis();
        if (tstamp > lastTime + rerankTime) {
            rerank(tstamp * BucketAggregate.MS);

        }

        return rankList.size();
    }


    private void rerank(final long tstamp) {
        List<T> lst = lister.list();
        Collections.sort(lst, new Comparator<T>() {
            public int compare(T o1, T o2) {
                double dd = o2.getAverage(tstamp, metric, average) - o1.getAverage(tstamp, metric, average);
                return dd == 0.0 ? 0 : dd > 0 ? 1 : -1;
            }
        });

        rankList = Collections.unmodifiableList(ZorkaUtil.clip(lst, maxSize));

        numReranks++;
    }


    public synchronized int getNumReranks() {
        return numReranks;
    }
}
