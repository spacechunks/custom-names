package space.chunks.customname.paper.listener

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import space.chunks.customname.paper.CustomNameStorage

class EntityRemoveListener : Listener {

    @EventHandler
    fun onEntityRemove(event: EntityRemoveFromWorldEvent) {
        // players are removed from the world on death, respawn and world change too, quitting is handled separately
        if (event.entity is Player) return
        CustomNameStorage.remove(event.entity.uniqueId)
    }

}
