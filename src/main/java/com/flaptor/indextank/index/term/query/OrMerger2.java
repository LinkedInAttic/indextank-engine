package com.flaptor.indextank.index.term.query;

import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.PeekingSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;

final class OrMerger2 extends AbstractSkippableIterable<RawMatch> {
	
	OrMerger2(SkippableIterable<RawMatch> l, SkippableIterable<RawMatch> r, double boost, double leftBoost, double rightBoost, double leftNorm, double rightNorm) {
	    this.left = l;
	    this.right = r;
        this.boost = boost;
        this.leftBoost = leftBoost;
        this.rightBoost = rightBoost;        
        this.leftNorm = leftNorm;
        this.rightNorm = rightNorm;
    }

    private final SkippableIterable<RawMatch> left;
    private final SkippableIterable<RawMatch> right;
    private double boost;
    private double leftBoost;
    private double rightBoost;
    private double leftNorm;
    private double rightNorm;

    @Override
    public SkippableIterator<RawMatch> iterator() {
        return new AbstractSkippableIterator<RawMatch>() {
            private PeekingSkippableIterator<RawMatch> it1 = new PeekingSkippableIterator<RawMatch>(left.iterator());
            private PeekingSkippableIterator<RawMatch> it2 = new PeekingSkippableIterator<RawMatch>(right.iterator());
            
            @Override
            protected RawMatch computeNext() {
                while (true) {
                    if (!it1.hasNext()) {
                        if (!it2.hasNext()) {
                            return endOfData();
                        } else {
                            RawMatch t = it2.next();
                            //System.out.println("OR!<: [S:"+t.getBoostedScore()+", B:"+boost+"]");
                            t.setScore(t.getBoostedScore());
                            t.setBoost(boost);
                            return t;
                        }
                    } else {
                        if (!it2.hasNext()) {
                            RawMatch t = it1.next();
                            //System.out.println("OR!>: [S:"+t.getBoostedScore()+", B:"+boost+"]");
                            t.setScore(t.getBoostedScore());
                            t.setBoost(boost);
                            return t;
                        }
                    }
                    
                    RawMatch t1 = it1.peek();
                    RawMatch t2 = it2.peek();
                    int c = t1.compareTo(t2);
                    
                    if (c == 0) {
                        //System.out.println("OR: [S:"+t1.getScore()+", B:"+t1.getBoost()+"] + [S:"+t2.getScore()+", B:"+t2.getBoost()+"] = [S:"+(t1.getBoostedScore()+t2.getBoostedScore())+", B:"+boost+"]");
                        t1.setScore(t1.getBoostedScore() + t2.getBoostedScore());
                        t1.setBoost(boost);
                        it1.next(); it2.next();
                        return t1;
                    } else if (c < 0) {
                        //System.out.println("OR<: [S:"+t1.getScore()+", B:"+t1.getBoost()+"] + [S:0, B:"+t2.getBoost()+"] = [S:"+t1.getBoostedScore()+", B:"+boost+"]");
                        t1.setScore(t1.getBoostedScore());
                        t1.setBoost(boost);
                        it1.next();
                        return t1;
                    } else {
                        //System.out.println("OR>: [S:0, B:"+t1.getBoost()+"] + [S:"+t2.getScore()+", B:"+t2.getBoost()+"] = [S:"+t2.getBoostedScore()+", B:"+boost+"]");
                        t2.setScore(t2.getBoostedScore());
                        t2.setBoost(boost);
                        it2.next();
                        return t2;
                    }
                }
            }
            
            @Override
            public void skipTo(int i) {
                it1.skipTo(i);
                it2.skipTo(i);
            }
        };
    }

}
