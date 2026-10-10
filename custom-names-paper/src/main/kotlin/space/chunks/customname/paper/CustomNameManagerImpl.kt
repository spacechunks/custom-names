package space.chunks.customname.paper

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomNameManager
import space.chunks.customname.paper.listener.EntityPassengerListener
import space.chunks.customname.paper.listener.EntityRemoveListener
import space.chunks.customname.paper.listener.PlayerQuitListener
import space.chunks.customname.paper.listener.PlayerSneakListener
import space.chunks.customname.paper.listener.PlayerTrackerListener

class CustomNameManagerImpl(
    private val plugin: JavaPlugin
) : CustomNameManager<Entity> {

    fun registerListeners() {
        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(PlayerTrackerListener(plugin), plugin)
        pluginManager.registerEvents(PlayerSneakListener(), plugin)
        pluginManager.registerEvents(EntityPassengerListener(plugin), plugin)
        pluginManager.registerEvents(PlayerQuitListener(plugin), plugin)
        pluginManager.registerEvents(EntityRemoveListener(), plugin)
    }

    fun stop() {
        CustomNameStorage.clear()
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
    }

    override fun forEntity(entity: Entity): CustomNameImpl {
        val existing = CustomNameStorage.getCustomPlayerName(entity.uniqueId)
        if (existing != null) return existing

        val customName = CustomNameImpl(plugin, entity)
        CustomNameStorage.register(entity.uniqueId, customName)
        customName.setHidden(false)
        customName.startUpdateTask()

        return customName
    }

    override fun unregister(entity: Entity) {
        CustomNameStorage.remove(entity.uniqueId)
    }

}