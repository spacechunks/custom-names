package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTickEvent
import space.chunks.customname.minestom.CustomNameStorage

/**
 * Responsible for hiding the name on entities that have
 * passengers on them.
 *
 * This matches vanilla behavior.
 */
object EntityPassengerListener {

    fun register(node: EventNode<Event>) {
        node.addListener(EntityTickEvent::class.java) { event ->
            val customName = CustomNameStorage.getCustomPlayerName(event.entity.uuid)
            if (customName != null) {
                val hasPassengers = event.entity.hasPassenger()
                if (hasPassengers && !customName.isHidden()) {
                    customName.setHidden(true)
                } else if (!hasPassengers && customName.isHidden()) {
                    customName.setHidden(false)
                }
            }
        }
    }

}
