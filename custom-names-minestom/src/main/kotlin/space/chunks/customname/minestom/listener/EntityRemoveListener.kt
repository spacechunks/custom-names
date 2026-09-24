package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDespawnEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import space.chunks.customname.minestom.CustomNameStorage

class EntityRemoveListener {

    fun register(node: EventNode<Event>) {
        node.addListener(EntityDespawnEvent::class.java) { event ->
            CustomNameStorage.remove(event.entity.uuid)
        }
        node.addListener(RemoveEntityFromInstanceEvent::class.java) { event ->
            CustomNameStorage.remove(event.entity.uuid)
        }
    }

}
