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

/**
 * Maintains rank lists of objects of various types. This can be used to monitor
 * and identify components consuming most resources (eg. threads, methods of EJB beans,
 * methods instrumented by Zorka etc.).
 *
 * @param <T> rankable item type
 */
public class RankList<T extends Rankable<?>> implements RankLister<T> {

    /** Rank lister used to scan and update set of watched components. */
    private RankLister<T> lister;

    /** Metric to be used as ranking criterium */
    private int metric;

    /** Which average will be used as metric criterium */
    private int average;

    /** Maximum number of items in tanking */
    private int maxSize;

    /** How often items should be reranked */
    private long rerankTime;

    /** Last rank cycle time */
    private long lastTime;

    /** Number of reranks (since ranking was created and started) */
    private int numReranks;

    /** List of (ranked) objects */
    private List<T> rankList;

    /** Utility class (to be made fully static at some point) */
    private final ZorkaUtil util = ZorkaUtil.getInstance();

    /**
     * Standard constructor.
     *
     * @param lister rank lister used to look for new items
     *
     * @param maxSize maximum number of items shown in ranking
     *
     * @param metric metric used as rank criterium
     *
     * @param average average that will be used as rank criterium
     *
     * @param rerankTime how often list should be reranked
     */
    public RankList(RankLister<T> lister, int maxSize, int metric, int average, long rerankTime) {
        this.lister = lister;
        this.maxSize = maxSize;
        this.metric = metric;
        this.average = average;
        this.rerankTime = rerankTime;
        this.lastTime = 0L;
    }


    /**
     * Returns n-th item from ranking
     *
     * @param n item index (starting with 0)
     *
     * @return item
     */
    public synchronized T get(int n) {

        if (util.currentTimeMillis() > lastTime + rerankTime) {
            rerank(util.currentTimeMillis());
        }

        try {
            return rankList.get(n);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

    }


    /**
     * Returns all items included in ranking.
     *
     * @return list of ranked items
     */
    public synchronized List<T> list() {

        if (util.currentTimeMillis() > lastTime + rerankTime) {
            rerank(util.currentTimeMillis());
        }

        return rankList;
    }


    /**
     * Returns size of rank list.
     *
     * @return number of items in ranking
     */
    public synchronized int size() {
        long tstamp = util.currentTimeMillis();
        if (tstamp > lastTime + rerankTime) {
            rerank(tstamp * BucketAggregate.MS);

        }

        return rankList.size();
    }


    /**
     * Recalculates ranking (this is done periodically).
     *
     * @param tstamp current time
     */
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


    /**
     * Returns number of reranks performed since rank list started.
     *
     * @return number of reranks
     */
    public synchronized int getNumReranks() {
        return numReranks;
    }
}
