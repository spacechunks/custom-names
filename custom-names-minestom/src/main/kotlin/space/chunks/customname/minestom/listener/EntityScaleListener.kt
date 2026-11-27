package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTickEvent
import space.chunks.customname.minestom.CustomNameStorage

object EntityScaleListener {

    fun register(node: EventNode<Event>) {
        node.addListener(EntityTickEvent::class.java) { event ->
            val customName = CustomNameStorage.get(event.entity.uuid) ?: return@addListener
            customName.updateTextOffset()
        }
    }

}
