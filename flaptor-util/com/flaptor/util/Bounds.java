package com.flaptor.util;


public class Bounds<N extends Comparable<N>> {
    private N min;
    private N max;
    private boolean minInclusive;
    private boolean maxInclusive;
    
    public Bounds(N min, N max) {
        this(min, max, true, true);
    }

    public Bounds(N min, N max, boolean minInclusive, boolean maxInclusive) {
        this.min = min;
        this.max = max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public boolean inBounds(N n) {
        return leftBound(n) && rightBound(n);
    }

    public boolean leftBound(N n) {
        boolean left = true;
        if (min != null) {
            int c = n.compareTo(min); 
            left = c > 0 || (minInclusive && c == 0);
        }
        return left;
    }

    public boolean rightBound(N n) {
        boolean right = true;
        if (max != null) {
            int c = n.compareTo(max); 
            right = c < 0 || (maxInclusive && c == 0);
        }
        return right;
    }
    
    public static <N extends Comparable<N>> Bounds<N> forRange(N min, N max) {
        return new Bounds<N>(min, max);
    }

    public static <N extends Comparable<N>> Bounds<N> forRange(N min, N max, boolean minInclusive, boolean maxInclusive) {
        return new Bounds<N>(min, max, minInclusive, maxInclusive);
    }
    
    public static <N extends Comparable<N>> Bounds<N> atLeast(N min) {
        return new Bounds<N>(min, null);
    }
    
    public static <N extends Comparable<N>> Bounds<N> atMost(N max) {
        return new Bounds<N>(null, max);
    }

    public static <N extends Comparable<N>> Bounds<N> parse(Class<N> type, String value) {
        String[] parts = value.split(":");
        N min = "*".equals(parts[0]) ? null : create(type, parts[0]);
        N max = "*".equals(parts[1]) ? null : create(type, parts[1]);
        return new Bounds<N>(min, max);
    }
    
    public static <N> N create(Class<N> type, String value) {
        try {
            return type.getConstructor(String.class).newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse bounds.", e);
        }
    }
    
    @Override
    public String toString() {
        return (min == null ? "*" : min) + ":" + (max == null ? "*" : max);
    }
}