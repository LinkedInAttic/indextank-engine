/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
