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
import space.chunks.customname.minestom.listener.PlayerInputListener
import space.chunks.customname.minestom.listener.PlayerTrackerListener

object CustomNamesMinestom {

    private val customNameManager = CustomNameManagerImpl()
    private var eventNode: EventNode<Event>? = null
    private var updateTask: Task? = null

    fun getManager(): CustomNameManager<Entity> = customNameManager

    fun init() {
        if (eventNode != null) return

        val node = EventNode.all("custom-names")
        PlayerTrackerListener.register(node)
        PlayerInputListener.register(node)
        EntityPassengerListener.register(node)
        PlayerQuitListener.register(node)
        EntityRemoveListener.register(node)

        MinecraftServer.getGlobalEventHandler().addChild(node)
        eventNode = node
        startUpdateTask()
    }

    fun shutdown() {
        val task = updateTask
        task?.cancel()
        updateTask = null

        val eventNode = eventNode ?: return
        MinecraftServer.getGlobalEventHandler().removeChild(eventNode)
        CustomNamesMinestom.eventNode = null
        CustomNameStorage.clear()
    }

    private fun startUpdateTask() {
        updateTask = MinecraftServer.getSchedulerManager()
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
    }

}