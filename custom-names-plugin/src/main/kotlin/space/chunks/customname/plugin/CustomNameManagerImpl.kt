package space.chunks.customname.plugin

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomNameManager
import space.chunks.customname.plugin.listener.EntityPassengerListener
import space.chunks.customname.plugin.listener.EntityRemoveListener
import space.chunks.customname.plugin.listener.PlayerQuitListener
import space.chunks.customname.plugin.listener.PlayerSneakListener
import space.chunks.customname.plugin.listener.PlayerTrackerListener

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

        startUpdateTask()
    }

    fun stop() {
        plugin.server.scheduler.cancelTasks(plugin)
        CustomNameStorage.clear()
    }

    private fun startUpdateTask() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for (customName in CustomNameStorage.getAll()) {
                if (!customName.getTargetEntity().isValid) {
                    CustomNameStorage.remove(customName.getTargetEntityId())
                } else {
                    customName.update()
                }
            }
        }, 20, 20)
    }

    override fun forEntity(entity: Entity): CustomNameImpl {
        val existing = CustomNameStorage.getCustomPlayerName(entity.uniqueId)
        if (existing != null) return existing

        val customName = CustomNameImpl(entity)
        CustomNameStorage.register(entity.uniqueId, customName)
        customName.setHidden(false)

        return customName
    }

    override fun unregister(entity: Entity) {
        CustomNameStorage.remove(entity.uniqueId)
    }

}