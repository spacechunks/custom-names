package space.chunks.customname.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.paper.CustomNameStorage

// hides the name while the entity carries passengers, just like vanilla does
class EntityPassengerListener(
    private val plugin: JavaPlugin,
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onEntityMount(event: EntityMountEvent) {
        val customName = CustomNameStorage.get(event.mount.uniqueId) ?: return
        customName.setHidden(true)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDismount(event: EntityDismountEvent) {
        val customName = CustomNameStorage.get(event.dismounted.uniqueId) ?: return
        if (event.dismounted.passengers.isNotEmpty()) return

        // the game sends the passenger update a bit later, so we wait 2 ticks before showing the name again
        event.dismounted.scheduler.runDelayed(plugin, { customName.setHidden(false) }, null, 2)
    }

}
