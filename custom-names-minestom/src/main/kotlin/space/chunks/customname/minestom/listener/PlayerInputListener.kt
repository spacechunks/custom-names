package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerInputEvent
import space.chunks.customname.minestom.CustomNameStorage

class PlayerInputListener {

    fun register(node: EventNode<Event>) {
        node.addListener(PlayerInputEvent::class.java) { event ->
            if (event.hasPressedShiftKey() || event.hasReleasedShiftKey()) {
                val playerName = CustomNameStorage.getCustomPlayerName(event.player.uuid) ?: return@addListener

                if (event.player.vehicle == null) {
                    playerName.setTargetEntitySneaking(event.isHoldingShiftKey)
                }
            }
        }
    }

}
