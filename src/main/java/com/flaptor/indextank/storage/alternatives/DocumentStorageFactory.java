package com.flaptor.indextank.storage.alternatives;

import java.util.Map;

public interface DocumentStorageFactory {
	public DocumentStorage fromConfiguration(Map<?, ?> config);
}
