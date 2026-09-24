package space.chunks.customname.minestom.listener

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.timer.TaskSchedule
import space.chunks.customname.minestom.CustomNameStorage

class PlayerQuitListener {

    fun register(node: EventNode<Event>) {
        node.addListener(PlayerDisconnectEvent::class.java) { event ->
            val uuid = event.player.uuid
            MinecraftServer.getSchedulerManager().scheduleTask({
                CustomNameStorage.remove(uuid)
                },
                TaskSchedule.tick(5),
                TaskSchedule.stop()
            )
        }
    }

}
