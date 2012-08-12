package fr.neatmonster.nocheatplus.checks.moving;

import java.util.Locale;

import net.minecraft.server.AxisAlignedBB;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet10Flying;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.utilities.PlayerLocation;

/*
 * M"""""""`YM          MM""""""""`M          dP dP 
 * M  mmmm.  M          MM  mmmmmmmM          88 88 
 * M  MMMMM  M .d8888b. M'      MMMM .d8888b. 88 88 
 * M  MMMMM  M 88'  `88 MM  MMMMMMMM 88'  `88 88 88 
 * M  MMMMM  M 88.  .88 MM  MMMMMMMM 88.  .88 88 88 
 * M  MMMMM  M `88888P' MM  MMMMMMMM `88888P8 dP dP 
 * MMMMMMMMMMM          MMMMMMMMMMMM                
 */
/**
 * A check to see if people cheat by tricking the server to not deal them fall damage.
 */
public class NoFall extends Check {

    /**
     * Instantiates a new no fall check.
     */
    public NoFall() {
        super(CheckType.MOVING_NOFALL);
    }

    /**
     * Checks a player.
     * 
     * @param player
     *            the player
     * @param from
     *            the from
     * @param to
     *            the to
     */
    public void check(final Player player, final PlayerLocation from, final PlayerLocation to) {
        final MovingConfig cc = MovingConfig.getConfig(player);
        final MovingData data = MovingData.getData(player);
        
      	if (player.isInsideVehicle()){
      		// Emergency fix attempt:
      		data.clearFlyData();
      		data.noFallFallDistance = 0D;
      		data.noFallY = to.getY();
      		player.setFallDistance(0.0f);
      		return;
      	}
        
        // If the player has just started falling, is falling into a liquid, in web or is on a ladder.
      	if (to.isInLiquid()){
      		// TODO: check if it is deep liquid
      		final double dist = data.noFallY - to.getY();
      		if (dist > 0 ){
      			// TODO: ? different model, at least distinguish water, lava, flowing.
      			final double fFLuid = 0.55;
      			data.noFallY = to.getY() + dist *fFLuid;
      			data.noFallFallDistance *= fFLuid;
      		}
      	}
      	else if (to.isInWeb() || to.isOnLadder()){	
            // Reset his fall distance.
            data.noFallFallDistance = 0D;
            data.noFallY = to.getY();
            player.setFallDistance(0.0f);
            return;
        }


//        data.noFallFallDistance = data.noFallFallDistance;
        
        /*
    	 * TODO: This might  actually (probably) calculated before ordinary fall damage can be dealt (!)
    	 *       So NoCheatPlus takes over ALL fall damage dealing, currently :) -
    	 *       This should not lead to alerts and trigger normal fall damage, probably,
    	 *       in case it is not cheating (how to distinguish...).
    	 */

        // If the player just touched the ground for the server, but no for the client.
        if (!data.noFallWasOnGroundServer && data.noFallOnGroundServer
                && (data.noFallWasOnGroundClient || !data.noFallOnGroundClient)) {
        	
            // Calculate the fall damages to be dealt.
            final int fallDamage = (int) data.noFallFallDistance - 2; // Blocks - 3 ?
            // TODO: set accurate fall damage (Boots with feather falling or protection).
            
            if (fallDamage > 0) {
                // Add the fall distance to the violation level.
                data.noFallVL += data.noFallFallDistance;

                // Execute the actions to find out if we need to cancel the event or not.
                if (executeActions(player, data.noFallVL, cc.noFallActions)){
                	// Deal the fall damages to the player.
                	if (player.getGameMode() != GameMode.CREATIVE){
                		final NoFallDamageEvent damageEvent = new NoFallDamageEvent(player, DamageCause.FALL, fallDamage);
                    	Bukkit.getPluginManager().callEvent(damageEvent);
                        if (!damageEvent.isCancelled()) player.damage(damageEvent.getDamage());
                	}
                	// Reset fall distances:
                    data.noFallFallDistance = 0.0;
                    data.noFallY = to.getY();
                    player.setFallDistance(0.0f);
                }
                    
            }
        }

        // If the player just touched the ground for the server.
        else if (!data.noFallWasOnGroundServer && data.noFallOnGroundServer) {
            // Calculate the difference between the fall distance calculated by the server and by the plugin.
        	
        	// TODO: What does the divisor do, is this experience ?
            final double difference = (data.noFallFallDistance - player.getFallDistance()) / data.noFallFallDistance;

            // If the difference is too big and the fall distance calculated by the plugin should hurt the player.
            if (difference > 0.15D && (int) data.noFallFallDistance > 2) {
                // Add the difference to the violation level.
                data.noFallVL += data.noFallFallDistance - player.getFallDistance();

                // Execute the actions to find out if we need to cancel the event or not.
                if (executeActions(player, data.noFallVL, cc.noFallActions)){
                	// Set the fall distance to its right value.
                    player.setFallDistance((float) data.noFallFallDistance);
                }
                    
            } else
                // Reward the player by lowering his violation level.
                data.noFallVL *= 0.95D;
        } else 
            // Reward the player by lowering his violation level.
            data.noFallVL *= 0.95D;
        if (data.noFallOnGroundServer){
        	data.noFallY = to.getY();
        	data.noFallFallDistance = 0.0;
        }
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.checks.Check#getParameter(fr.neatmonster.nocheatplus.actions.ParameterName,
     * org.bukkit.entity.Player)
     */
    @Override
    public String getParameter(final ParameterName wildcard, final ViolationData violationData) {
        if (wildcard == ParameterName.FALL_DISTANCE)
            return String.format(Locale.US, "%.2f", MovingData.getData(violationData.player).noFallFallDistance);
        else
            return super.getParameter(wildcard, violationData);
    }

    /**
     * Handle a movement packet to extract its precious information.
     * 
     * @param player
     *            the player
     * @param packet
     *            the packet
     */
    public void handlePacket(final EntityPlayer player, final Packet10Flying packet) {
        final MovingData data = MovingData.getData(player.getBukkitEntity());
        
        // Attempt to fix vehicle problems:
        if (player.getBukkitEntity().isInsideVehicle()){
        	// rely on vehicle-move for most.
        	data.noFallFallDistance = 0.0;
        	data.noFallY = player.locY;
        	return;
        }
        
        // Suggestion: use reference y position in data and calculate difference to that one!
        	
        data.noFallWasOnGroundClient = data.noFallOnGroundClient;
        data.noFallWasOnGroundServer = data.noFallOnGroundServer;
        data.noFallOnGroundClient = packet.g;
        final AxisAlignedBB boundingBoxGround = player.boundingBox.clone().d(packet.x - player.locX,
                packet.y - player.locY - 0.001D, packet.z - player.locZ);
        data.noFallOnGroundServer = player.world.getCubes(player, boundingBoxGround).size() > 0;
        // make use of data.noFallY (absolute reference for falling height).
        if (packet.hasPos){
        	// TODO: not 100% sure which to use as  reference...
        	if (data.noFallWasOnGroundServer && !data.noFallOnGroundServer){
             	data.noFallFallDistance = 0D;
             	data.noFallY = player.locY;
            }
            else if (data.noFallY - packet.y > 0D)
                data.noFallFallDistance = Math.max(0.0, data.noFallY - packet.y); //+= player.locY - packet.y;
            else if (data.noFallOnGroundServer) data.noFallY = player.locY;
        }
    }
}
