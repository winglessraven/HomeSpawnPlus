/**
 * 
 */
package org.morganm.homespawnplus.storage;

import java.io.IOException;

import org.morganm.homespawnplus.HomeSpawnPlus;


/**
 * @author morganm
 *
 */
public class StorageFactory {
	public static final int STORAGE_TYPE_EBEANS = 0;
	public static final int STORAGE_TYPE_CACHED_EBEANS = 1;
	
	public static Storage getInstance(int storageType, HomeSpawnPlus plugin)
		throws StorageException, IOException
	{
		if ( storageType == STORAGE_TYPE_EBEANS ) {
			return new StorageEBeans(plugin);
		}
		else if( storageType == STORAGE_TYPE_CACHED_EBEANS ) {
			return new StorageCache(new StorageEBeans(plugin));
		}
		else {
			throw new StorageException("Unable to create Storage interface, invalid type given: "+storageType);
		}
	}

}