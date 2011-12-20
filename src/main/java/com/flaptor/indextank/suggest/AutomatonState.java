package com.flaptor.indextank.suggest;

import java.util.Set;

public interface AutomatonState {
	public Set<AutomatonEdge> getEdges();
	public boolean isFinal();
}
