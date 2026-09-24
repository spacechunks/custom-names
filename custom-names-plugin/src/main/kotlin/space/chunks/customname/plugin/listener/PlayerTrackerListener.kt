package space.chunks.customname.plugin.listener

import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.plugin.CustomNameStorage

/**
 * Responsible for hiding the name on entities that have
 * vehicles on them.
 *
 * This matches vanilla behavior.
 */
class PlayerTrackerListener(
    private val plugin: JavaPlugin
) : Listener {

    @EventHandler
    fun trackEntity(event: PlayerTrackEntityEvent) {
        val playerName = CustomNameStorage.getCustomPlayerName(event.entity.uniqueId) ?: return
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            playerName.sendToClient(event.player)
        }, 1)
    }

    @EventHandler
    fun untrackEntity(event: PlayerUntrackEntityEvent) {
        val playerName = CustomNameStorage.getCustomPlayerName(event.entity.uniqueId) ?: return
        playerName.removeFromClient(event.player)
    }

}