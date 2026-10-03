package space.chunks.customname.minestom.listener

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.timer.TaskSchedule
import space.chunks.customname.minestom.CustomNameStorage

object PlayerQuitListener {

    fun register(node: EventNode<Event>) {
        node.addListener(PlayerDisconnectEvent::class.java) { event ->
            val uuid = event.player.uuid
            val name = CustomNameStorage.getCustomPlayerName(uuid) ?: return@addListener

            MinecraftServer.getSchedulerManager()
                .buildTask { CustomNameStorage.remove(uuid, name) }
                .delay(TaskSchedule.tick(5))
                .schedule()
        }
    }

}
