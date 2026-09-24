package space.chunks.customname.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.paper.CustomNameStorage

class PlayerQuitListener(
    private val plugin: JavaPlugin
) : Listener {

    @EventHandler
    fun playerQuit(event: PlayerQuitEvent) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            CustomNameStorage.remove(event.player.uniqueId)
        }, 5)
    }

}