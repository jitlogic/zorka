package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.ZorkaSubmitter;

import java.util.ArrayList;
import java.util.List;

public class TestStringOutput implements ZorkaSubmitter<String> {

    private List<String> results = new ArrayList<String>();

    public List<String> getResults() {
        return results;
    }

    @Override
    public boolean submit(String item) {
        return results.add(item);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String r : results) {
            sb.append(r);
            sb.append('\n');
        }
        return sb.toString();
    }
}
