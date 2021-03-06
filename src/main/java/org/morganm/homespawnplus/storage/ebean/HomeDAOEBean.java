/*******************************************************************************
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (c) 2012 Mark Morgan.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * Contributors:
 *     Mark Morgan - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.morganm.homespawnplus.storage.ebean;

import java.util.Set;

import org.morganm.homespawnplus.entity.Home;
import org.morganm.homespawnplus.storage.dao.HomeDAO;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.Transaction;

/**
 * @author morganm
 *
 */
public class HomeDAOEBean implements HomeDAO {
	private EbeanServer ebean;
	
	public HomeDAOEBean(final EbeanServer ebean) {
		setEbeanServer(ebean);
	}
	
	public void setEbeanServer(final EbeanServer ebean) {
		this.ebean = ebean;
	}

	@Override
	public Home findHomeById(int id) {
		String q = "find home where id = :id";
		
		Query<Home> query = ebean.createQuery(Home.class, q);
		query.setParameter("id", id);
		
		return query.findUnique();
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#getDefaultHome(java.lang.String, java.lang.String)
	 */
	@Override
	public Home findDefaultHome(String world, String playerName) {
		String q = "find home where lower(playerName) = lower(:playerName) and world = :world and defaultHome = 1";
		
		Query<Home> query = ebean.createQuery(Home.class, q);
		query.setParameter("playerName", playerName);
		query.setParameter("world", world);

		return query.findUnique();
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#getBedHome(java.lang.String, java.lang.String)
	 */
	@Override
	public Home findBedHome(String world, String playerName) {
		String q = "find home where lower(playerName) = lower(:playerName) and world = :world and bedHome = 1";
		
		Query<Home> query = ebean.createQuery(Home.class, q);
		query.setParameter("playerName", playerName);
		query.setParameter("world", world);
		
		return query.findUnique();
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#getNamedHome(java.lang.String, java.lang.String)
	 */
	@Override
	public Home findHomeByNameAndPlayer(String homeName, String playerName) {
		String q = "find home where lower(playerName) = lower(:playerName) and name = :name";
		
		Query<Home> query = ebean.createQuery(Home.class, q);
		query.setParameter("playerName", playerName);
		query.setParameter("name", homeName);
		
		return query.findUnique();
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#getHomes(java.lang.String, java.lang.String)
	 */
	@Override
	public Set<Home> findHomesByWorldAndPlayer(String world, String playerName) {
		String q = "find home where lower(playerName) = lower(:playerName) and world like :world order by world";
		
		if( world == null || "all".equals(world) || "*".equals(world) )
			world = "%";
		
		Query<Home> query = ebean.createQuery(Home.class, q);
		query.setParameter("playerName", playerName);
		query.setParameter("world", world);
		
		return query.findSet();
	}

	public Set<Home> findHomesByPlayer(String playerName) {
		return findHomesByWorldAndPlayer(null, playerName);
	}
	
	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#getAllHomes()
	 */
	@Override
	public Set<Home> findAllHomes() {
		return ebean.find(Home.class).findSet();
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.storage.dao.HomeDAO#writeHome(org.morganm.homespawnplus.entity.Home)
	 */
	@Override
	public void saveHome(Home home) {
		final int homeId = home.getId();
		
		Transaction tx = ebean.beginTransaction();
		// We should only have one "BedHome" per player per world. So if this update is setting
		// BedHome to true, then we make sure to clear out all others for this player/world combo
		if( home.isBedHome() ) {
			SqlUpdate update = ebean.createSqlUpdate("update hsp_home set bed_home=0"
					+" where player_name = :playerName and world = :world and id != :id");
			update.setParameter("playerName", home.getPlayerName());
			update.setParameter("world", home.getWorld());
			update.setParameter("id", homeId);
			update.execute();
		}
		
		// We should only have one defaultHome per player per world. So if this update is setting
		// defaultHome to true, then we make sure to clear out all others for this player/world combo
		if( home.isDefaultHome() ) {
			SqlUpdate update = ebean.createSqlUpdate("update hsp_home set default_home=0"
					+" where player_name = :playerName and world = :world and id != :id");
			update.setParameter("playerName", home.getPlayerName());
			update.setParameter("world", home.getWorld());
			update.setParameter("id", homeId);
			update.execute();
		}
		tx.commit();
		
		ebean.save(home);
		
		// clean up any related home invites as well
		tx = ebean.beginTransaction();
		SqlUpdate update = ebean.createSqlUpdate("delete from hsp_homeinvite"
				+" where home_id = :id");
		update.setParameter("id", homeId);
		update.execute();
		tx.commit();
	}

	@Override
	public void deleteHome(Home home) {
		ebean.delete(home);
	}
}
