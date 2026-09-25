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
    private val node: EventNode<Event> = EventNode.all("custom-names")
    private var updateTask: Task? = null

    fun getManager(): CustomNameManager<Entity> = customNameManager

    fun enable() {
        PlayerTrackerListener().register(node)
        PlayerInputListener().register(node)
        EntityPassengerListener().register(node)
        PlayerQuitListener().register(node)
        EntityRemoveListener().register(node)

        MinecraftServer.getGlobalEventHandler().addChild(node)
        startUpdateTask()
    }

    fun disable() {
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
            CustomNameStorage.getAll().forEach { name ->
                if (name.getTargetEntity().isRemoved) {
                    CustomNameStorage.remove(name.getTargetEntityId())
                } else {
                    name.update()
                }
            }},
            TaskSchedule.tick(20),
            TaskSchedule.tick(20)
        )
    }

}