package com.flaptor.indextank.index.term;

public final class DocTermMatch {
    private int rawId;
    private int[] positions;
    private double normalization;
    private int positionsLength;

    public DocTermMatch(int rawId, int[] positions, int positionsLength, double normalization) {
        this.rawId = rawId;
        this.positions = positions;
        this.positionsLength = positionsLength;
        this.normalization = normalization;
    }
    
    public int getRawId() {
        return rawId;
    }
    
    public int[] getPositions() {
        return positions;
    }
    
    public int getPositionsLength() {
        return positionsLength;
    }
    
    public double getNormalization() {
        return normalization;
    }

	public double getTermScore() {
	    return (Math.sqrt(positionsLength) * getNormalization());
	}
	
	public double getSquareTermScore() {
	    return positionsLength * getNormalization() * getNormalization();
	}
	
	public void setRawId(int rawId) {
        this.rawId = rawId;
    }
	
	public void setPositions(int[] positions) {
        this.positions = positions;
    }
	
	public void setPositionsLength(int positionsLength) {
        this.positionsLength = positionsLength;
    }
	
	public void setNormalization(double normalization) {
        this.normalization = normalization;
    }
	
	@Override
	public String toString() {
	    return "DTM:" + rawId;
	}

}