package fr.neatmonster.nocheatplus.checks.combined;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * 
 * @author mc_dev
 *
 */
public class CombinedListener implements Listener {
	
	protected final Improbable improbable;

	public CombinedListener(){
		this.improbable = new Improbable();
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerToggleSneak(final PlayerToggleSneakEvent event){
		// Check also in case of cancelled events.
		if (Improbable.check(event.getPlayer(), 0.35f, System.currentTimeMillis())) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerToggleSprint(final PlayerToggleSprintEvent event){
		// Check also in case of cancelled events.
		if (Improbable.check(event.getPlayer(), 0.35f, System.currentTimeMillis())) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerMove(final PlayerMoveEvent event){
		final long now = System.currentTimeMillis();
		final Player player = event.getPlayer();
		
		final Location loc = player.getLocation();
		final String worldName = loc.getWorld().getName();
		final CombinedData data = CombinedData.getData(player);
		data.lastMoveTime = now;
		// Just add the yaw to the list.
		Combined.feedYawRate(player, loc.getYaw(), now, worldName, data);
	}
	
	// (possibly other types of events, but these combine with fighting).
}
