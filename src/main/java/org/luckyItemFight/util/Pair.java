package org.luckyItemFight.util;

public class Pair<K,V> {
    private K first;
    private V second;
    public Pair(K first,V second) {
        this.first = first;
        this.second = second;
    }

    public K first() { return first; }
    public V second() { return second; }

    public void first(K val) { this.first = val; }
    public void second(V val) { this.second = val; }
}
