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
package org.morganm.homespawnplus.strategies;

import org.morganm.homespawnplus.entity.Spawn;
import org.morganm.homespawnplus.strategy.BaseStrategy;
import org.morganm.homespawnplus.strategy.StrategyContext;
import org.morganm.homespawnplus.strategy.StrategyResult;

/**
 * @author morganm
 *
 */
public class SpawnNamedSpawn extends BaseStrategy {
	private String namedSpawn;
	
	public SpawnNamedSpawn() {}
	public SpawnNamedSpawn(final String namedSpawn) {
		this.namedSpawn = namedSpawn;
	}

	@Override
	public StrategyResult evaluate(StrategyContext context) {
		// take the name from the argument, if given
		String name = context.getArg();
		// otherwise use the name given at instantiation
		if( name == null )
			name = namedSpawn;
		
		Spawn spawn = plugin.getUtil().getSpawnByName(name);
		
		// since namedSpawn is very specific, it's usually an error condition if we didn't
		// find a named spawn that the admin identified, so print a warning so they can
		// fix the issue.
		if( spawn == null )
			log.warning(logPrefix+" No spawn found for name \""+name+"\" for \""+getStrategyConfigName()+"\" strategy");
		
		return new StrategyResult(spawn);
	}

	@Override
	public String getStrategyConfigName() {
		return "spawnNamedSpawn";
	}

}
