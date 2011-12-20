package com.flaptor.indextank.index.scorer;

public interface Boosts {
	public float getBoost(int boostIndex);
	public int getTimestamp();
}
