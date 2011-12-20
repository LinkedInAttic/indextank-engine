package com.flaptor.util.apis;

/**
 * Interface for Geocoding queries 
 * 
 * @author Martin Massera
 */
public interface GoogleGeo {
    
    /**
     * get from cache or google api the geographic information
     * 
     * blocking wait to enforce the request limit 
     * @param place
     * @return
     */
    public Geocode getGeocode(String place);
    
    /**
     * retrieve the geocode only if it doesnt have to sleep
     */
    public Geocode getGeocodeNoDelay(String place);
}
