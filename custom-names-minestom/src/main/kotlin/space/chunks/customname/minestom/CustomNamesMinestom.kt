package space.chunks.customname.minestom

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.event.EventNode
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import space.chunks.customname.api.CustomNameManager
import space.chunks.customname.minestom.listener.EntityInvisibilityListener
import space.chunks.customname.minestom.listener.EntityPassengerListener
import space.chunks.customname.minestom.listener.EntityRemoveListener
import space.chunks.customname.minestom.listener.EntityScaleListener
import space.chunks.customname.minestom.listener.EntitySneakListener
import space.chunks.customname.minestom.listener.PlayerQuitListener
import space.chunks.customname.minestom.listener.PlayerTrackerListener
import java.util.concurrent.atomic.AtomicReference

object CustomNamesMinestom {

    private val customNameManager = CustomNameManagerImpl()
    private val node = EventNode.all("custom-names")
    private val updateTask = AtomicReference<Task?>()

    fun getManager(): CustomNameManager<Entity> = customNameManager

    fun init() {
        if (node.parent != null) return

        PlayerTrackerListener.register(node)
        EntitySneakListener.register(node)
        EntityPassengerListener.register(node)
        EntityInvisibilityListener.register(node)
        EntityScaleListener.register(node)
        PlayerQuitListener.register(node)
        EntityRemoveListener.register(node)

        MinecraftServer.getGlobalEventHandler().addChild(node)
        startUpdateTask()
    }

    fun shutdown() {
        if (node.parent == null) return

        updateTask.getAndSet(null)?.cancel()
        MinecraftServer.getGlobalEventHandler().removeChild(node)
        CustomNameStorage.clear()
    }

    private fun startUpdateTask() {
        val task = MinecraftServer.getSchedulerManager()
            .buildTask {
                CustomNameStorage.getAll().forEach { name ->
                    if (name.getTargetEntity().isRemoved) {
                        CustomNameStorage.remove(name.getTargetEntityId())
                    } else {
                        name.update()
                    }
                }
            }
            .delay(TaskSchedule.tick(20))
            .repeat(TaskSchedule.tick(20))
            .schedule()
        updateTask.set(task)
    }

}
