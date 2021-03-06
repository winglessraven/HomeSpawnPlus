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
package org.morganm.homespawnplus.command;

import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.morganm.homespawnplus.HomeSpawnPlus;
import org.morganm.homespawnplus.HomeSpawnUtils;
import org.morganm.homespawnplus.config.ConfigOptions;
import org.morganm.homespawnplus.i18n.HSPMessages;
import org.morganm.homespawnplus.manager.CooldownManager;
import org.morganm.homespawnplus.manager.WarmupManager;
import org.morganm.homespawnplus.manager.WarmupRunner;
import org.morganm.homespawnplus.util.Debug;


/** Abstract class that takes care of some routine tasks for commands, to keep those
 * objects as light as possible and so as to not violate the DRY principle.
 * 
 * @author morganm
 *
 */
public abstract class BaseCommand implements Command, CommandExecutor {
	protected Debug debug;
	protected HomeSpawnPlus plugin;
	protected HomeSpawnUtils util;
	protected CooldownManager cooldownManager;
	protected WarmupManager warmupManager;
	protected Logger log;
	protected String logPrefix;
	private boolean enabled;
	private String permissionNode;
	private String commandName;
	private Map<String, Object> commandParams;
	
//	public BaseCommand(String name) {
//		super(name);
//	}

	public String getDescription() { return null; }
	public String getUsage() {
		return "/<command>";
	}
	
	/** By default, commands do not respond to console input. They can override this if they wish
	 * to do so.
	 * @deprecated use execute(CommandSender sender, String[] args)
	 */
	public boolean execute(ConsoleCommandSender console, org.bukkit.command.Command command, String[] args)
	{
		return this.execute(console, args);
	}
	/** By default, we do nothing. This is a legacy method that will be phased out.
	 * @deprecated use execute(CommandSender sender, String[] args)
	 * 
	 */
	public boolean execute(Player p, org.bukkit.command.Command command, String[] args)
	{
		return this.execute(p, args);
	}
	/** This is the new preferred method for commands to override.
	 * 
	 */
	public boolean execute(CommandSender sender, String[] args) {
		return false;
		
		/*
		// support legacy Command mode by calling those methods first
		if( sender instanceof Player ) {
			Player p = (Player) sender;
			
			return this.execute(p, null, args);
		}
		else if( sender instanceof ConsoleCommandSender ) {
			ConsoleCommandSender console = (ConsoleCommandSender) sender;
			
			return this.execute(console, null, args);
		}
		// no legacy methods? Call the preferred one
		else
			return execute(sender, args);
			*/
	}
	
	@Override
	public final boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
		debug.debug("onCommand() label=",label);
		// support legacy Command mode by calling those methods first
		if( sender instanceof Player ) {
			Player p = (Player) sender;
			
			debug.debug("onCommand() invoking Player execute");
			return this.execute(p, command, args);
		}
		else if( sender instanceof ConsoleCommandSender ) {
			ConsoleCommandSender console = (ConsoleCommandSender) sender;
			
			debug.debug("onCommand() invoking Console execute");
			return this.execute(console, command, args);
		}
		// no legacy methods? Call the preferred one
		else {
			debug.debug("onCommand() invoking preferred execute");
			return execute(sender, args);
		}
	}
	
	public void setCommandParameters(Map<String, Object> params) {
		this.commandParams = params;
	}
	protected Object getParam(String param) {
		if( commandParams != null )
			return commandParams.get(param);
		else
			return null;
	}
	/** Return a given parameter as a string. If the parameter doesn't exist
	 * or it is not a string, null is returned.
	 * 
	 * @param param
	 * @return
	 */
	protected String getStringParam(String param) {
		Object v = getParam(param);
		if( v != null && v instanceof String)
			return (String) v;
		else if( v != null )
			return v.toString();
		else
			return null;
	}

	/** Returns this object for easy initialization in a command hash.
	 * 
	 * @param plugin
	 * @return
	 */
	public Command setPlugin(HomeSpawnPlus plugin) {
		this.debug = Debug.getInstance();
		this.plugin = plugin;
		this.util = plugin.getUtil();
		this.cooldownManager = plugin.getCooldownManager();
		this.warmupManager = plugin.getWarmupmanager();
		this.log = HomeSpawnPlus.log;
		this.logPrefix = HomeSpawnPlus.logPrefix;
		enabled = !plugin.getHSPConfig().getBoolean(getDisabledConfigFlag(), Boolean.FALSE);
		return this;
	}
	
	/** Return true if the command is enabled, false if it is not.
	 * 
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	protected String getDisabledConfigFlag() {
		return ConfigOptions.COMMAND_TOGGLE_BASE + getCommandName();
	}
	
	/** Check to see if player has sufficient money to pay for this command.
	 * 
	 * @param p
	 * @return
	 */
	protected boolean costCheck(Player p) {
		boolean returnValue = false;
		
		Economy economy = plugin.getEconomy();
		if( economy == null )
			returnValue = true;
		
		if( !returnValue && plugin.hasPermission(p, HomeSpawnPlus.BASE_PERMISSION_NODE + ".CostExempt." + getCommandName()) )
			returnValue = true;

		if( !returnValue ) {
			int price = getPrice(p);
			if( price > 0 ) {
				double balance = economy.getBalance(p.getName());
				if( balance >= price )
					returnValue = true;
			}
			else
				returnValue = true;	// no cost for this command
		}
		
		return returnValue;
	}
	
	protected int getPrice(Player p) {
		return util.getCommandCost(p, getCommandName());
	}
	
	protected void printInsufficientFundsMessage(Player p) {
		Economy economy = plugin.getEconomy();
		if( economy != null )
			util.sendLocalizedMessage(p, HSPMessages.COST_INSUFFICIENT_FUNDS,
					"price", economy.format(getPrice(p)),
					"balance", economy.format(economy.getBalance(p.getName())));
//			util.sendMessage(p, "Insufficient funds, you need at least "+economy.format(getPrice(p))
//					+ " (you only have "+economy.format(economy.getBalance(p.getName()))+")"
//				);
	}
	
	/**
	 * 
	 * @param p
	 * @return true on success, false if there was an error that should prevent the action from taking place
	 */
	protected boolean applyCost(Player p, boolean applyCooldown, String cooldownName) {
		boolean returnValue = false;
		
		Economy economy = plugin.getEconomy();
		if( economy == null )
			returnValue = true;
		
		final String perm = HomeSpawnPlus.BASE_PERMISSION_NODE + ".CostExempt." + getCommandName();
		if( !returnValue && plugin.hasPermission(p, perm) )
			returnValue = true;
		debug.debug("applyCost: player=",p,", exempt permissionChecked=",perm,", exempt returnValue=",returnValue);

		if( !costCheck(p) ) {
			printInsufficientFundsMessage(p);
			returnValue = false;
		}
		else if( !returnValue ) {
			int price = getPrice(p);
			if( price > 0 ) {
				EconomyResponse response = economy.withdrawPlayer(p.getName(), price);
				
				if( response.transactionSuccess() ) {
					if( plugin.getHSPConfig().getBoolean(ConfigOptions.COST_VERBOSE, true) ) {
						// had an error report that might have been related to a null value
						// being returned from economy.format(price), so let's check for that
						// and protect against any error.
						String priceString = economy.format(price);
						if( priceString == null )
							priceString = ""+price;
						
						util.sendLocalizedMessage(p, HSPMessages.COST_CHARGED,
								"price", priceString,
								"command", getCommandName());
//						util.sendMessage(p, economy.format(price) + " charged for use of the " + getCommandName() + " command.");
					}
					
					returnValue = true;
				}
				else {
					util.sendLocalizedMessage(p, HSPMessages.COST_ERROR,
							"price", economy.format(price),
							"errorMessage", response.errorMessage);
//					util.sendMessage(p, "Error subtracting "+price+" from your account: "+response.errorMessage);
					returnValue = false;
				}
			}
			else
				returnValue = true;	// no cost for this command
		}
		
		// if applyCooldown flag is true and the returnValue is true, then apply the Cooldown now
		if( applyCooldown && returnValue == true )
			applyCooldown(p, cooldownName);
		
		return returnValue;
	}
	protected boolean applyCost(Player p, boolean applyCooldown) {
		return applyCost(p, applyCooldown, null);
	}
	protected boolean applyCost(Player p) {
		return applyCost(p, false);
	}
	
	protected void doWarmup(Player p, WarmupRunner wr) {
		if ( !isWarmupPending(p, wr.getWarmupName()) ) {
			warmupManager.startWarmup(p.getName(), wr);
			
			util.sendLocalizedMessage(p, HSPMessages.WARMUP_STARTED,
					"name", wr.getWarmupName(),
					"seconds", warmupManager.getWarmupTime(p, wr.getWarmupName()).warmupTime);
//			util.sendMessage(p, "Warmup "+wr.getWarmupName()+" started, you must wait "+
//					warmupManager.getWarmupTime(p, wr.getWarmupName()).warmupTime+" seconds.");
		}
		else
			util.sendLocalizedMessage(p, HSPMessages.WARMUP_ALREADY_PENDING, "name", wr.getWarmupName());
//			util.sendMessage(p, "Warmup already pending for "+wr.getWarmupName());
		
	}
	
	/** Most commands for this plugin check 3 things:
	 * 
	 *   1 - is the command enabled in the config?
	 *   2 - does the player have access to run the command?
	 *   3 - is the command on cooldown for the player?
	 *   
	 * This method just implements all 3 checks.
	 * 
	 * @param p the player object that is running the command
	 * 
	 * @return returns false if the checks fail and Command processing should stop, true if the command is allowed to continue
	 */
	protected boolean defaultCommandChecks(Player p) {
		debug.devDebug("defaultCommandChecks() enabled=",enabled);
		if( !enabled )
			return false;

		final boolean hasP = hasPermission(p);
		debug.devDebug("defaultCommandChecks() hasPermission =",hasP);
		if( !hasP )
			return false;

		final boolean cd = cooldownCheck(p);
		debug.devDebug("defaultCommandChecks() cooldownCheck = ",cd);
		if( !cd )
			return false;
		
		debug.devDebug("defaultCommandChecks() all defaultCommandChecks return true");
		return true;
	}
	protected boolean defaultCommandChecks(CommandSender sender) {
		if( sender instanceof Player ) {
			return defaultCommandChecks((Player) sender);
		}
		else {		// it's a local or remote console
			return true;
		}
	}
	
	@Override
	public void setCommandName(String name) {
		commandName = name;
	}
	
	/** Can be overridden, but default implementation just applies the command name
	 * as the lower case version of the class name of the implemented command.
	 */
	@Override
	public String getCommandName() {
		if( commandName == null ) {
			String className = this.getClass().getName();
			int index = className.lastIndexOf('.');
			commandName = className.substring(index+1).toLowerCase();
		}
		
		return commandName;
	}
	
	/** Here for convenience if the command has no aliases.  Otherwise, this should be overridden.
	 * 
	 */
	@Override
	public String[] getCommandAliases() {
		return null;
	}
	
	/** check the default command cooldown for the player
	 * 
	 * @param p
	 * @return true if cooldown is available, false if currently in cooldown period
	 */
	protected boolean cooldownCheck(Player p, String cooldownName) {
		if( cooldownName == null )
			cooldownName = getCommandName();
		return cooldownManager.cooldownCheck(p, cooldownName);
	}
	protected boolean cooldownCheck(Player p) {
		return cooldownCheck(p, getCommandName());
	}
	
	protected void applyCooldown(Player p, String cooldownName) {
		if( cooldownName == null )
			cooldownName = getCommandName();
		cooldownManager.setCooldown(p,  cooldownName);
	}
	protected void applyCooldown(Player p) {
		applyCooldown(p,  getCommandName());
	}

	/**
	 * 
	 * @return true if this command & player has a warmup associated with it
	 */
	protected boolean hasWarmup(Player p, String warmupName) {
		return warmupManager.hasWarmup(p, warmupName);
	}
	protected boolean hasWarmup(Player p) {
		return hasWarmup(p, getCommandName());
	}
	
	/** check if a warmup is already pending for this command
	 * 
	 * @param p
	 * @return true if warmup is already pending, false if not
	 */
	protected boolean isWarmupPending(Player p, String warmupName) {
		return warmupManager.isWarmupPending(p.getName(), warmupName);
	}
	protected boolean isWarmupPending(Player p) {
		return isWarmupPending(p, getCommandName());
	}
	
	protected String getCooldownName(String baseName, String homeName) {
		if( baseName == null )
			baseName = getCommandName();
		
		if( homeName != null && cooldownManager.isCooldownSeparationEnabled(baseName) )
			return baseName + "." + homeName;
		else
			return baseName;
	}

	public final String getCommandPermissionNode() {
		if( permissionNode == null ) {
			// set permission node from config params, if set
			permissionNode = getStringParam("permission");
			
			// otherwise use default permission node
			if( permissionNode == null )
				permissionNode = HomeSpawnPlus.BASE_PERMISSION_NODE + ".command." + getCommandName();
		}
		
		return permissionNode;
	}
	
	/** Return true if the player has permission to run this command.  If they
	 * don't have permission, print them a message saying so and return false.
	 * 
	 * @param p
	 * @return
	 */
	protected boolean hasPermission(Player p) {
		if( !plugin.hasPermission(p, getCommandPermissionNode()) ) {
			util.sendLocalizedMessage(p, HSPMessages.NO_PERMISSION);
//			p.sendMessage("You don't have permission to do that.");
			return false;
		}
		else
			return true;
	}
}
