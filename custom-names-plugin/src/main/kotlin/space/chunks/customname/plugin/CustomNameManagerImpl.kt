package space.chunks.customname.plugin

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomNameManager
import space.chunks.customname.plugin.listener.EntityPassengerListener
import space.chunks.customname.plugin.listener.PlayerQuitListener
import space.chunks.customname.plugin.listener.PlayerSneakListener
import space.chunks.customname.plugin.listener.PlayerTrackerListener

class CustomNameManagerImpl(
    private val plugin: JavaPlugin
): CustomNameManager<Entity> {

    fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PlayerTrackerListener(plugin), plugin)
        Bukkit.getPluginManager().registerEvents(PlayerSneakListener(), plugin)
        Bukkit.getPluginManager().registerEvents(EntityPassengerListener(plugin), plugin)
        Bukkit.getPluginManager().registerEvents(PlayerQuitListener(plugin), plugin)
    }

    override fun forEntity(entity: Entity): CustomNameImpl {
        var customName = CustomNameStorage.getCustomPlayerName(entity.uniqueId)
        if (customName == null) {
            customName = CustomNameImpl(plugin, entity)
            CustomNameStorage.register(entity.uniqueId, customName)

            // Send to trackers
            customName.setHidden(false)
        }

        return customName
    }

    override fun unregister(entity: Entity) {
        val customName = CustomNameStorage.remove(entity.uniqueId)
        customName?.setHidden(true)
    }

}