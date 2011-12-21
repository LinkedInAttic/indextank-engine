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

/**
 * 
 */
package com.flaptor.indextank.index.term.query;

public final class RawMatch implements Comparable<RawMatch> {
	private int rawId;
	private double score;
    private double boost;
	
	public RawMatch(int rawId, double score, double boost) {
		this.rawId = rawId;
		this.score = score;
        this.boost = boost;
	}

	@Override
	public int compareTo(RawMatch o) {
		return rawId - o.rawId;
	}
	
	public int getRawId() {
		return rawId;
	}
    
	public double getScore() {
		return score;
	}

    public double getBoost() {
        return boost;
    }

	public void setRawId(int rawId) {
        this.rawId = rawId;
    }
	
	public void setScore(double score) {
		this.score = score;
	}

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public double getBoostedScore() {
        return score * boost;
    }

	@Override
	public String toString() {
		return String.valueOf(rawId) + " score:"+score + " boost:"+boost;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + rawId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RawMatch other = (RawMatch) obj;
		if (rawId != other.rawId)
			return false;
		return true;
	}

}
