package space.chunks.customname.paper

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.world.entity.EntityAttachment
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomName
import space.chunks.customname.paper.util.CustomNamePackets
import java.util.UUID
import java.util.function.Consumer

class CustomNameImpl(
    private val plugin: JavaPlugin,
    private val targetEntity: Entity,
) : CustomName {

    private val packets = CustomNamePackets(this)

    private val textOffset: Double
    private val passengerOffset: Double
    private val nametagEntityId: Int

    @Volatile
    private var targetEntitySneaking = false

    @Volatile
    private var nameCallback: (viewer: Audience) -> Component? = { null }

    @Volatile
    private var hidden = false

    @Volatile
    private var updateTask: ScheduledTask? = null

    init {
        val nmsEntity = (targetEntity as CraftEntity).handle
        nametagEntityId = nmsEntity.level().nextEntityId

        val ridingOffset = nmsEntity
            .getPassengerRidingPosition(nmsEntity)
            .subtract(nmsEntity.position()).y

        val nametagOffset = nmsEntity.attachments.getClamped(EntityAttachment.NAME_TAG, 0, 0f).y

        // vanilla draws the top of a name tag 0.5 above its attachment, but a text display draws the bottom of its text at its origin
        textOffset = nametagOffset + CustomNamePackets.NAMETAG_OFFSET - CustomNamePackets.LINE_HEIGHT - ridingOffset
        passengerOffset = ridingOffset
    }

    fun startUpdateTask() {
        // runs on the thread owning the target entity, so the trackers can be accessed safely
        updateTask = targetEntity.scheduler.runAtFixedRate(plugin, {
            if (!targetEntity.isValid) {
                CustomNameStorage.remove(targetEntity.uniqueId, this)
            } else {
                update()
            }
        }, { CustomNameStorage.remove(targetEntity.uniqueId, this) }, 20, 20)
    }

    fun cancelUpdateTask() {
        updateTask?.cancel()
    }

    fun update() {
        if (hidden) return

        val trackers = targetEntity.trackedBy
        if (trackers.isEmpty()) return

        val riderPacket = packets.getRiderPacket()
        for (player in trackers) {
            (player as CraftPlayer).handle.connection.send(riderPacket)
        }
    }

    override fun setName(nameCallback: (viewer: Audience) -> Component?) {
        this.nameCallback = nameCallback
        syncData()
    }

    fun setTargetEntitySneaking(targetEntitySneaking: Boolean) {
        this.targetEntitySneaking = targetEntitySneaking
        syncData()
    }

    fun sendToClient(entity: Player) {
        if (!hidden) {
            (entity as CraftPlayer).handle.connection.send(packets.initialSpawnPacket(entity))
        }
    }

    fun removeFromClient(entity: Player) {
        (entity as CraftPlayer).handle.connection.send(packets.getRemovePacket())
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        runOnTrackers { player ->
            if (hidden) removeFromClient(player) else sendToClient(player)
        }
    }

    override fun getName(viewer: Audience): Component? = nameCallback(viewer)
    override fun getTargetEntityId(): UUID = targetEntity.uniqueId
    override fun isHidden(): Boolean = hidden

    fun getNametagId(): Int = nametagEntityId
    fun isTargetEntitySneaking(): Boolean = targetEntitySneaking
    fun getTextOffset(): Double = textOffset
    fun getPassengerOffset(): Double = passengerOffset
    fun getTargetEntity(): Entity = targetEntity

    private fun syncData() {
        if (hidden) return

        runOnTrackers { player ->
            (player as CraftPlayer).handle.connection.send(packets.syncDataPacket(player))
        }
    }

    private fun runOnTrackers(consumer: Consumer<Player>) {
        // trackers may only be accessed by the thread owning the target entity, so we hop over to it first
        if (!Bukkit.isOwnedByCurrentRegion(targetEntity)) {
            if (plugin.isEnabled) {
                targetEntity.scheduler.run(plugin, { runOnTrackers(consumer) }, null)
            }
            return
        }

        for (player in targetEntity.trackedBy) {
            consumer.accept(player)
        }
    }
}
