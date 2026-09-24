package space.chunks.customname.minestom

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import space.chunks.customname.api.CustomNameManager
import space.chunks.customname.minestom.listener.EntityPassengerListener
import space.chunks.customname.minestom.listener.EntityRemoveListener
import space.chunks.customname.minestom.listener.PlayerQuitListener
import space.chunks.customname.minestom.listener.PlayerSneakListener
import space.chunks.customname.minestom.listener.PlayerTrackerListener

class CustomNameManagerImpl : CustomNameManager<Entity> {

    private val node: EventNode<Event> = EventNode.all("custom-names")
    private var updateTask: Task? = null

    fun registerListeners() {
        PlayerTrackerListener().register(node)
        PlayerSneakListener().register(node)
        EntityPassengerListener().register(node)
        PlayerQuitListener().register(node)
        EntityRemoveListener().register(node)

        MinecraftServer.getGlobalEventHandler().addChild(node)
        startUpdateTask()
    }

    fun stop() {
        val task = updateTask
        if (task != null) {
            task.cancel()
            updateTask = null
        }
        MinecraftServer.getGlobalEventHandler().removeChild(node)
        CustomNameStorage.clear()
    }

    private fun startUpdateTask() {
        updateTask = MinecraftServer.getSchedulerManager().scheduleTask({
                for (customName in CustomNameStorage.getAll()) {
                    if (customName.getTargetEntity().isRemoved) {
                        CustomNameStorage.remove(customName.getTargetEntityId())
                    } else {
                        customName.update()
                    }
                }
            },
            TaskSchedule.tick(20),
            TaskSchedule.tick(20)
        )
    }

    override fun forEntity(entity: Entity): CustomNameImpl {
        val existing = CustomNameStorage.getCustomPlayerName(entity.uuid)
        if (existing != null) return existing

        val customName = CustomNameImpl(entity)
        CustomNameStorage.register(entity.uuid, customName)
        customName.setHidden(false)

        return customName
    }

    override fun unregister(entity: Entity) {
        CustomNameStorage.remove(entity.uuid)
    }

}
