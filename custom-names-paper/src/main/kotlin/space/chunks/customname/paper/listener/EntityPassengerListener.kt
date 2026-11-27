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
        customName.setCarryingPassengers(true)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDismount(event: EntityDismountEvent) {
        val customName = CustomNameStorage.get(event.dismounted.uniqueId) ?: return
        val vehicle = event.dismounted

        // the passenger is still attached while this event runs and the game resyncs the passengers right after, so we check again 2 ticks later
        vehicle.scheduler.runDelayed(plugin, {
            customName.setCarryingPassengers(vehicle.passengers.isNotEmpty())
        }, null, 2)
    }

}
