package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.util.KVSortingHeap;

import org.junit.Test;

import static org.junit.Assert.*;

public class KVSortingHeapUnitTest {

    @Test
    public void testAddRemoveSingleElement() {
        KVSortingHeap h = new KVSortingHeap(7, true);

        h.add(42, 2);

        assertEquals(1, h.size());
        assertEquals(42, h.next());
        assertEquals(-1, h.next());
    }


    @Test
    public void testAddRemoveElementsNoOverflowInvert() {
        KVSortingHeap h = new KVSortingHeap(7, true);
        h.add(42,2);
        h.add(44,4);
        h.add(41,1);
        h.add(47,7);
        h.add(45,5);
        h.add(43,3);
        h.add(46,6);

        h.heapify();

        assertEquals(41, h.next());
        assertEquals(42, h.next());
        assertEquals(43, h.next());
        assertEquals(44, h.next());
        assertEquals(45, h.next());
        assertEquals(46, h.next());
        assertEquals(47, h.next());
        assertEquals(-1, h.next());
    }

    @Test
    public void testAddRemoveElementsNoOverflowNoInvert() {
        KVSortingHeap h = new KVSortingHeap(7, false);
        h.add(42,2);
        h.add(44,4);
        h.add(41,1);
        h.add(47,7);
        h.add(45,5);
        h.add(43,3);
        h.add(46,6);

        h.heapify();

        assertEquals(47, h.next());
        assertEquals(46, h.next());
        assertEquals(45, h.next());
        assertEquals(44, h.next());
        assertEquals(43, h.next());
        assertEquals(42, h.next());
        assertEquals(41, h.next());
        assertEquals(-1, h.next());
    }


    @Test
    public void testFilterBiggestVals() {
        KVSortingHeap h = new KVSortingHeap(7, true);
        for (int i = 0; i < 100; i++) {
            h.add(40+i, i);
        }

        h.invert();

        assertEquals(139, h.next());
        assertEquals(138, h.next());
        assertEquals(137, h.next());
        assertEquals(136, h.next());
        assertEquals(135, h.next());
        assertEquals(134, h.next());
        assertEquals(133, h.next());
        assertEquals(-1, h.next());
    }

}
