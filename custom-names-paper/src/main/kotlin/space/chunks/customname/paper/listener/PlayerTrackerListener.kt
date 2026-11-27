package space.chunks.customname.paper.listener

import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.paper.CustomNameStorage

class PlayerTrackerListener(
    private val plugin: JavaPlugin,
) : Listener {

    @EventHandler
    fun onTrackEntity(event: PlayerTrackEntityEvent) {
        val customName = CustomNameStorage.get(event.entity.uniqueId) ?: return
        event.entity.scheduler.runDelayed(plugin, {
            customName.sendToClient(event.player)
        }, null, 1)
    }

    @EventHandler
    fun onUntrackEntity(event: PlayerUntrackEntityEvent) {
        val customName = CustomNameStorage.get(event.entity.uniqueId) ?: return
        customName.removeFromClient(event.player)
    }

}
