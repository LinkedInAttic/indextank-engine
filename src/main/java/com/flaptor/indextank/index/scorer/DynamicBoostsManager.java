package com.flaptor.indextank.index.scorer;

import java.io.Serializable;

@Deprecated
public class DynamicBoostsManager {

    @Deprecated
    static class DynamicBoosts implements Boosts, Serializable {
        private static final long serialVersionUID = 1L;
        
        float[] boosts;
    	int timestamp;
    	
		@Override
		public float getBoost(int boostIndex) {
			return boosts[boostIndex];
		}

		/*@Override
		public int getBoostCount() {
			return boosts.length;
		}*/

		@Override
		public int getTimestamp() {
			return timestamp;
		}

    }
    

}
