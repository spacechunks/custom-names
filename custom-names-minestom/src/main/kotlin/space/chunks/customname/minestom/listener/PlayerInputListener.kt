package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerInputEvent
import space.chunks.customname.minestom.CustomNameStorage

// forwards the sneak state, so the name turns transparent like a vanilla name tag
object PlayerInputListener {

    fun register(node: EventNode<Event>) {
        node.addListener(PlayerInputEvent::class.java) { event ->
            if (!event.hasPressedShiftKey() && !event.hasReleasedShiftKey()) return@addListener
            if (event.player.vehicle != null) return@addListener

            val customName = CustomNameStorage.get(event.player.uuid) ?: return@addListener
            customName.setTargetEntitySneaking(event.isHoldingShiftKey)
        }
    }

}
