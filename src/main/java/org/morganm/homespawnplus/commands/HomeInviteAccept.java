/**
 * 
 */
package org.morganm.homespawnplus.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.morganm.homespawnplus.command.BaseCommand;
import org.morganm.homespawnplus.config.ConfigOptions;
import org.morganm.homespawnplus.i18n.HSPMessages;
import org.morganm.homespawnplus.manager.WarmupRunner;

/**
 * @author morganm
 *
 */
public class HomeInviteAccept extends BaseCommand {

	@Override
	public String[] getCommandAliases() { return new String[] {"hiaccept", "hia"}; }
	
	@Override
	public boolean execute(final Player p, final Command command, final String[] args) {
		if( !isEnabled() || !hasPermission(p) )
			return true;
		
		String warmupName = getCommandName();
		if( plugin.getConfig().getBoolean(ConfigOptions.HOME_INVITE_USE_HOME_WARMUP, true) )
			warmupName = "home";
		
		final org.morganm.homespawnplus.entity.Home h = plugin.getHomeInviteManager().getInvitedHome(p);
		if( h != null ) {
			String cooldownName = getCooldownName("homeinviteaccept", Integer.toString(h.getId()));
			if( plugin.getConfig().getBoolean(ConfigOptions.HOME_INVITE_USE_HOME_COOLDOWN, true) )
				cooldownName = getCooldownName("home", Integer.toString(h.getId()));

			if( !cooldownCheck(p, cooldownName) )
				return true;

			if( hasWarmup(p, warmupName) ) {
	    		final Location finalL = h.getLocation();
				doWarmup(p, new WarmupRunner() {
					private boolean canceled = false;
					private String cdName;
					private String wuName;
					
					public void run() {
						if( !canceled ) {
							util.sendLocalizedMessage(p, HSPMessages.CMD_WARMUP_FINISHED,
									"name", getWarmupName(), "place", h.getName());
							if( applyCost(p, true, cdName) )
								p.teleport(finalL);
							plugin.getHomeInviteManager().removeInvite(p);
						}
					}

					public void cancel() {
						canceled = true;
					}

					public void setPlayerName(String playerName) {}
					public void setWarmupId(int warmupId) {}
					public WarmupRunner setCooldownName(String cd) { cdName = cd; return this; }
					public WarmupRunner setWarmupName(String warmupName) { wuName = warmupName; return this; }
					public String getWarmupName() { return wuName; }
				}.setCooldownName(cooldownName).setWarmupName(warmupName));
			}
			else {
				if( applyCost(p, true, cooldownName) ) {
					p.teleport(h.getLocation());
					util.sendLocalizedMessage(p, HSPMessages.CMD_HIACCEPT_TELEPORTED,
							"home", h.getName(), "player", h.getPlayerName());
					plugin.getHomeInviteManager().removeInvite(p);
				}
			}
		}
		else 
			util.sendLocalizedMessage(p, HSPMessages.CMD_HIACCEPT_NO_INVITE);
		
		return true;
	}

}