/**
 * 
 */
package org.morganm.homespawnplus;

import javax.inject.Inject;

import org.morganm.homespawnplus.config.ConfigStorage;
import org.morganm.homespawnplus.guice.InjectorFactory;
import org.morganm.homespawnplus.server.api.Plugin;
import org.morganm.homespawnplus.server.api.event.EventDispatcher;
import org.morganm.homespawnplus.util.SpawnUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/** Main object that controls plugin startup and shutdown.
 * 
 * @author morganm
 *
 */
public class HomeSpawnPlus {
    private final Logger log = LoggerFactory.getLogger(HomeSpawnPlus.class);

    // members that are passed in to us when instantiated
    private final Object originalPluginObject;
    private final ConfigStorage configStorage;

    // members that are injected by the IoC container
    @Inject private Initializer initializer;
    @Inject private EventDispatcher eventDispatcher;
    @Inject private Plugin plugin;
    @Inject private SpawnUtil spawnUtil;
    
    public HomeSpawnPlus(Object originalPluginObject, ConfigStorage configStorage) {
        this.originalPluginObject = originalPluginObject;
        this.configStorage = configStorage;
    }

    public void onEnable() throws Exception {
//        GuiceDebug.enable();
        final Injector injector = InjectorFactory.createInjector(originalPluginObject, configStorage); // IoC container
        injector.injectMembers(this);   // inject all dependencies for this object

        initializer.initAll();
        eventDispatcher.registerEvents();
        
        log.info("version {}, build {} is enabled", plugin.getVersion(), plugin.getBuildNumber());
    }
    
    public void onDisable() {
        // unhook multiverse (if needed)
//        multiverse.onDisable();

        spawnUtil.updateAllPlayerLocations();
        initializer.shutdownAll();

        log.info("version {}, build {} is disabled", plugin.getVersion(), plugin.getBuildNumber());
    }

    /** Routine to detect other plugins that use the same commands as HSP and
     * often cause conflicts and create confusion.
     * 
     */
    /* only method left from old plugin class, probably should put this into the integration
     * classes when I build them.
     * 
    private void detectAndWarn() {
        // do nothing if warning is disabled
        if( !getConfig().getBoolean(ConfigOptions.WARN_CONFLICTS, true) )
            return;
        
        if( getServer().getPluginManager().getPlugin("Essentials") != null ) {
            log.warning(logPrefix+" Essentials found. It is likely your HSP /home and /spawn commands will"
                    + " end up going to Essentials instead.");
            log.warning(logPrefix+" Also note that HSP can convert your homes from Essentials for you. Just"
                    + " run the command \"/hspconvert essentials\" (must have hsp.command.admin permission)");
            log.warning(logPrefix+" Set \"core.warnConflicts\" to false in your HSP config.yml to disable"
                    + " this warning.");
        }
        
        if( getServer().getPluginManager().getPlugin("CommandBook") != null ) {
            log.warning(logPrefix+" CommandBook found. It is likely your HSP /home and /spawn commands will"
                    + " end up going to CommandBook instead. Please add \"homes\" and"
                    + " \"spawn-locations\" to your CommandBook config.yml \"components.disabled\" section.");
            log.warning(logPrefix+" Set \"core.warnConflicts\" to false in your HSP config.yml to disable"
                    + " this warning.");
        }
    }
    */

}
