package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTickEvent
import space.chunks.customname.minestom.CustomNameStorage

// hides the name while the entity carries passengers, just like vanilla does
object EntityPassengerListener {

    fun register(node: EventNode<Event>) {
        node.addListener(EntityTickEvent::class.java) { event ->
            val customName = CustomNameStorage.get(event.entity.uuid) ?: return@addListener
            customName.setCarryingPassengers(event.entity.hasPassenger())
        }
    }

}
