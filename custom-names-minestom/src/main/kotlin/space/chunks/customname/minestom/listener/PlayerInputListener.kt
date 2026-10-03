package space.chunks.customname.minestom.listener

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerInputEvent
import space.chunks.customname.minestom.CustomNameStorage

/**
 * Responsible for forwarding sneaking state over the
 * nametag entity. This causes the name tag to appear transparent
 * when sneaking.
 *
 * This matches vanilla behavior.
 */
object PlayerInputListener {

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
