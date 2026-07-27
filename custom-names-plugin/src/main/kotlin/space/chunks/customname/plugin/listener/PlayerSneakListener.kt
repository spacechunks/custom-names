package space.chunks.customname.plugin.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import space.chunks.customname.plugin.CustomNameStorage

/**
 * Responsible for forwarding sneaking state over the
 * nametag entity. This causes the name tag to appear transparent
 * when sneaking.
 *
 * This matches vanilla behavior.
 */
class PlayerSneakListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun toggleSneak(event: PlayerToggleSneakEvent) {
        val playerName = CustomNameStorage.getCustomPlayerName(event.player.uniqueId)?: return
        // Does the entity have a custom name?
        if (!event.player.isInsideVehicle) {
            playerName.setTargetEntitySneaking(event.isSneaking)
        }
    }

}