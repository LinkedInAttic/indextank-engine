package com.flaptor.util.apis;

import java.io.Serializable;

/**
 * represents a geographic position. Can be a country (locality = null) 
 * or a locality (locality != null)
 * country must always be set
 * 
 * @author Martin Massera
 */
public class Geocode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String locality;
    private String country;
    private double[] latLong;
    
    public Geocode() {}
    public Geocode(String country, String locality, double[] latLong) {
        this.country = country;
        this.locality = locality;
        this.latLong = latLong;
    }
    
    public String getLocality() {
        return locality;
    }
    public String getCountry() {
        return country;
    }
    public double[] getLatLong() {
        return latLong;
    }
    public void setLocality(String locality) {
        this.locality = locality;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public void setLatLong(double[] latLong) {
        this.latLong = latLong;
    }

    public int hashCode() {
        return 
            country.hashCode() + 
            ((locality != null) ? locality.hashCode() : 0);
    }
    public boolean equals(Object obj) {
        if (!(obj instanceof Geocode)) return false;
        Geocode code = (Geocode)obj;
        if (!country.equals(code.country)) return false;
        if (locality == null) return code.locality == null;
        else return locality.equals(code.locality);
    }
    
    @Override
    public String toString() {
        return country + 
            ((locality != null) ? ("." + locality) : ""); 
//            +((latLong != null) ? (" ("+latLong[0] + ","+latLong[1]+")") : "");
    }
}