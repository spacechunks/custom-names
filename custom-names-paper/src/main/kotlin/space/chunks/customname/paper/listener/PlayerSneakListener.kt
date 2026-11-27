package space.chunks.customname.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import space.chunks.customname.paper.CustomNameStorage

// forwards the sneak state, so the name turns transparent like a vanilla name tag
class PlayerSneakListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onToggleSneak(event: PlayerToggleSneakEvent) {
        val customName = CustomNameStorage.get(event.player.uniqueId) ?: return
        if (event.player.isInsideVehicle) return

        customName.setTargetEntitySneaking(event.isSneaking)
    }

}
