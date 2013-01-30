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

package com.jitlogic.zorka.agent.rankproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class QueryDef {

    private static final List<QuerySegment> EMPTY_SEG
                = Collections.unmodifiableList(new ArrayList<QuerySegment>(1));

    private String mbsName;
    private String query;
    private List<String> attributes;
    private List<QuerySegment> segments;


    public QueryDef(String mbsName, String query, String... attrs) {
        this.mbsName = mbsName;
        this.query = query;
        this.attributes = Collections.unmodifiableList(Arrays.asList(attrs));
        this.segments = EMPTY_SEG;
    }


    private QueryDef(QueryDef orig) {
        this.mbsName = orig.mbsName;
        this.query = orig.query;
        this.attributes = orig.attributes;
        this.segments = orig.segments;
    }


    private QueryDef withSegs(QuerySegment...segs) {
        QueryDef qdef = new QueryDef(this);

        List<QuerySegment> newSegs = new ArrayList<QuerySegment>(segments.size() + segs.length + 1);
        newSegs.addAll(segments);
        newSegs.addAll(Arrays.asList(segs));
        qdef.segments = Collections.unmodifiableList(newSegs);

        return qdef;
    }


    public QueryDef get(Object...args) {
        QuerySegment[] segs = new QuerySegment[args.length];

        for (int i = 0 ; i < args.length; i++) {
            segs[i] = new QuerySegment(args[i]);
        }

        return withSegs(segs);
    }


    public QueryDef get(String arg, String name) {
        return withSegs(new QuerySegment(arg, name));
    }


    public QueryDef list(String regex) {
        Pattern pattern = regex.startsWith("~") ? Pattern.compile(regex.substring(1))
                : Pattern.compile("^"+regex.replace("**", ".+").replace("*", "[^\\.]+")+"$");

        return withSegs(new QuerySegment(pattern));
    }


    public QueryDef list(String regex, String template) {
        Pattern pattern = regex.startsWith("~") ? Pattern.compile(regex.substring(1))
                : Pattern.compile("^"+regex.replace("**", ".+").replace("*", "[^\\.]+")+"$");

        return withSegs(new QuerySegment(pattern, template));
    }


    public String getMbsName() {
        return mbsName;
    }


    public String getQuery() {
        return query;
    }


    public List<String> getAttributes() {
        return attributes;
    }


    public List<QuerySegment> getSegments() {
        return segments;
    }
}
