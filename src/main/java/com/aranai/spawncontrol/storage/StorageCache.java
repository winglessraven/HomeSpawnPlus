/**
 * 
 */
package com.aranai.spawncontrol.storage;

import java.util.HashMap;
import java.util.Set;

import com.aranai.spawncontrol.entity.Home;
import com.aranai.spawncontrol.entity.Spawn;

/** Wraps another storage type and caches the entities in memory as they are read and written.
 * 
 * @author morganm
 *
 */
public class StorageCache implements Storage
{
	private Storage original;

	// only populated if getallHomes() is called
	private Set<Home> allHomes;
	// only populated if getallSpawns() is called
	private Set<Spawn> allSpawns;
	
	// flat list of all groups that are used in spawngroups
	private Set<String> spawnDefinedGroups;
	
	// all homes, organized by world then player
	private HashMap <String, HashMap<String, Home>> homes;
	// all spawns, organized by world
	private HashMap <String, Spawn> spawns;
	// group spawns organized by world then group
	private HashMap <String, HashMap<String, Spawn>> groupSpawns;
	
	public StorageCache(Storage original) {
		this.original = original;
		homes = new HashMap <String, HashMap<String, Home>>();
		spawns = new HashMap <String, Spawn>();
		groupSpawns = new HashMap <String, HashMap<String, Spawn>>();
	}
	
	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#initializeStorage()
	 */
	@Override
	public void initializeStorage() {
		original.initializeStorage();
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getHome(java.lang.String, java.lang.String)
	 */
	@Override
	public Home getHome(String world, String playerName) {
		HashMap<String, Home> worldHomes = homes.get(world);
		if( worldHomes == null ) {
			worldHomes = new HashMap<String, Home>();
			homes.put(world, worldHomes);
		}
		
		Home home = worldHomes.get(playerName);
		// not cached, lets get it from backing store
		if( home == null ) {
			home = original.getHome(world, playerName);
			worldHomes.put(playerName, home);				// add it to cache
		}
		
		return home;
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getSpawn(java.lang.String)
	 */
	@Override
	public Spawn getSpawn(String world) {
		Spawn spawn = spawns.get(world);
		// not cached, lets get it from backing store
		if( spawn == null ) {
			spawn = original.getSpawn(world);
			spawns.put(world, spawn);						// add it to cache
		}
		
		return spawn;
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getSpawn(java.lang.String, java.lang.String)
	 */
	@Override
	public Spawn getSpawn(String world, String group) {
		HashMap<String, Spawn> worldSpawns = groupSpawns.get(world);
		if( worldSpawns == null ) {
			worldSpawns = new HashMap<String, Spawn>();
			groupSpawns.put(world, worldSpawns);
		}
		
		Spawn spawn = worldSpawns.get(group);
		// not cached, lets get it from backing store
		if( spawn == null ) {
			spawn = original.getSpawn(world, group);
			worldSpawns.put(group, spawn);					// add it to cache
		}
		
		return spawn;
	}
	
	// update a Home in the in-memory cache
	private void updateHome(Home home) {
		String world = home.getWorld();
		String playerName = home.getPlayerName();
		
		HashMap<String, Home> worldHomes = homes.get(world);
		if( worldHomes == null ) {
			worldHomes = new HashMap<String, Home>();
			homes.put(world, worldHomes);
		}
		worldHomes.put(playerName, home);
	}

	/** Called only when getAllHomes() hits the database to return all homes - since we have
	 * them all in memory already, we store them in the key-value hash.
	 * 
	 * @param allHomes
	 */
	private void updateHomes(Set<Home> allHomes) {
		for(Home home : allHomes) {
			updateHome(home);
		}
	}
	
	// update a Spawn in the in-memory cache
	private void updateSpawn(Spawn spawn) {
		String world = spawn.getWorld();
		String group = spawn.getGroup();
		
		spawns.put(world, spawn);
		
		HashMap<String, Spawn> worldSpawns = groupSpawns.get(world);
		if( worldSpawns == null ) {
			worldSpawns = new HashMap<String, Spawn>();
			groupSpawns.put(world, worldSpawns);
		}
		worldSpawns.put(group, spawn);
		
		// if the spawnDefinedGroups cache is populated and group isn't null, make sure this group is in the set.
		if( spawnDefinedGroups != null && group != null )
			spawnDefinedGroups.add(group);
	}
	
	/** Called only when getAllSpawns() hits the database to return all spawns - since we have
	 * them all in memory already, we store them in the key-value hashes.
	 * 
	 * @param allSpawns
	 */
	private void updateSpawns(Set<Spawn> allSpawns) {
		for(Spawn spawn : allSpawns) {
			updateSpawn(spawn);
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getSpawnDefinedGroups()
	 */
	public Set<String> getSpawnDefinedGroups() {
		if( spawnDefinedGroups == null )
			spawnDefinedGroups = original.getSpawnDefinedGroups();
		
		return spawnDefinedGroups;
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getAllHomes()
	 */
	@Override
	public Set<Home> getAllHomes() {
		if( allHomes == null ) {
			allHomes = original.getAllHomes();
			updateHomes(allHomes);
		}
		
		return allHomes;
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#getAllSpawns()
	 */
	@Override
	public Set<Spawn> getAllSpawns() {
		if( allSpawns == null ) {
			allSpawns = original.getAllSpawns();
			updateSpawns(allSpawns);
		}
		
		return allSpawns;
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#writeHome(com.aranai.spawncontrol.entity.Home)
	 */
	@Override
	public void writeHome(Home home) {
		original.writeHome(home);
		
		updateHome(home);

		// update the allHomes cache too if it is populated
		if( allHomes != null )
			allHomes.add(home);
	}

	/* (non-Javadoc)
	 * @see com.aranai.spawncontrol.storage.Storage#writeSpawn(com.aranai.spawncontrol.entity.Spawn)
	 */
	@Override
	public void writeSpawn(Spawn spawn) {
		original.writeSpawn(spawn);
		
		updateSpawn(spawn);

		// update the allSpawns cache too if it is populated
		if( allSpawns != null )
			allSpawns.add(spawn);
	}

}