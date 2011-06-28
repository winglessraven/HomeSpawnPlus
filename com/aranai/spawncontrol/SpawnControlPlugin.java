/**
 * 
 */
package com.aranai.spawncontrol;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.aranai.spawncontrol.command.CommandProcessor;
import com.aranai.spawncontrol.entity.Home;
import com.aranai.spawncontrol.entity.Spawn;

/** This is just a basic shell that handles setting up the SpawnControl singleton
 * instance and dealing with Bukkit-related overrides and methods.
 * 
 * @author morganm
 *
 */
public class SpawnControlPlugin extends JavaPlugin
{
	private CommandProcessor cmdProcessor;
	
	@Override
    public void onEnable() {
    	SpawnControl.createInstance(this);
    	SpawnControl.getInstance().onEnable();
    	cmdProcessor = new CommandProcessor(SpawnControl.getInstance());
    }
    
	@Override
    public void onDisable() {
    	SpawnControl.getInstance().onDisable();
		SpawnControl.clearInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    	return cmdProcessor.onCommand(sender, command, commandLabel, args);
    }
    
	/** Invoke the underlying EBEAN installDDL() method to initialize our database.
	 * 
	 */
    protected void initDB() {
        installDDL();
    }
    
    /** Define the Entity classes that we want serialized to the database.
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> classList = new LinkedList<Class<?>>();
        classList.add(Home.class);
        classList.add(Spawn.class);
        return classList;
    }
}