package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTickEvent
import space.chunks.customname.minestom.CustomNameStorage

// forwards the sneak state, so the name turns transparent like a vanilla name tag, minestom updates it from the player input already
object EntitySneakListener {

    fun register(node: EventNode<Event>) {
        node.addListener(EntityTickEvent::class.java) { event ->
            val customName = CustomNameStorage.get(event.entity.uuid) ?: return@addListener
            customName.setTargetEntitySneaking(event.entity.isSneaking && event.entity.vehicle == null)
        }
    }

}
