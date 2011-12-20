/**
 * 
 */
package com.flaptor.indextank.index.rti.inverted;

class Key implements Comparable<Key> {
	Key(String field, String term) {
		this.field = field;
		this.term = term;
	}
	String field;
	String term;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
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
		Key other = (Key) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}
	@Override
	public int compareTo(Key o) {
		int c = field.compareTo(o.field);
		if (c == 0) {
			c = term.compareTo(o.term);
		}
		return c;
	}
}