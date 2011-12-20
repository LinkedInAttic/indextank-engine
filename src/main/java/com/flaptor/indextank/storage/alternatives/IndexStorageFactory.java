package com.flaptor.indextank.storage.alternatives;

/**
 * It allows to lazily instanciate IndexStorage while saving storage configuration in advance. 
 * @author leandro
 *
 */
public interface IndexStorageFactory {
    
    IndexStorage getIndexStorage(String indexId, String environmentIdentifier);
}
