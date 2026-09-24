package space.chunks.customname.plugin.listener

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import space.chunks.customname.plugin.CustomNameStorage

class EntityRemoveListener : Listener {

    @EventHandler
    fun removeEntity(event: EntityRemoveFromWorldEvent) {
        CustomNameStorage.remove(event.entity.uniqueId)
    }

}
