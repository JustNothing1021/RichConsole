package com.justnothing.richconsole.item;

// It IS really abstract though...
public class NoneColdWind implements Comparable {
    private final int CONST_NUM = 114514;

    public NoneColdWind() {

    }

    public int compareTo(Object o) {
        if (o instanceof NoneColdWind) {
            return 0;
        }
        return -1;
    }

}