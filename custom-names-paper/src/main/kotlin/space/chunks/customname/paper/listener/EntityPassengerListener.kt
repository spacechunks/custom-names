package space.chunks.customname.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.paper.CustomNameStorage

/**
 * Responsible for hiding the name on entities that have
 * vehicles on them.
 *
 * This matches vanilla behavior.
 */
class EntityPassengerListener(
    private val plugin: JavaPlugin
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onEntityMount(event: EntityMountEvent) {
        val playerName = CustomNameStorage.getCustomPlayerName(event.mount.uniqueId) ?: return
        playerName.setHidden(true)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDismount(event: EntityDismountEvent) {
        val playerName = CustomNameStorage.getCustomPlayerName(event.dismounted.uniqueId) ?: return

        if (event.dismounted.passengers.isEmpty()) {
            // Run 2 ticks later, we need to ensure that the game sends the packets to update the
            // passengers.
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                playerName.setHidden(false)
            }, 2)
        }
    }

}