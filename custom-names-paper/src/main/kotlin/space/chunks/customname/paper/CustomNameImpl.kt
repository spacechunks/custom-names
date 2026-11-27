package space.chunks.customname.paper

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.world.entity.EntityAttachment
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomName
import space.chunks.customname.paper.util.CustomNamePackets
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class CustomNameImpl(
    private val plugin: JavaPlugin,
    private val targetEntity: Entity,
) : CustomName {

    private val packets = CustomNamePackets(this)

    private val textOffset: Double
    private val passengerOffset: Double
    private val seeThroughDisplayId: Int
    private val textDisplayId: Int

    private val nameCallback = AtomicReference<(viewer: Audience) -> Component?>({ null })
    private val targetEntitySneaking = AtomicBoolean(targetEntity.isSneaking && !targetEntity.isInsideVehicle)
    private val carryingPassengers = AtomicBoolean(targetEntity.passengers.isNotEmpty())
    private val targetEntityInvisible = AtomicBoolean(isTargetEntityInvisible())
    private val hidden = AtomicBoolean()
    private val updateTask = AtomicReference<ScheduledTask?>()

    init {
        val nmsEntity = (targetEntity as CraftEntity).handle
        seeThroughDisplayId = nmsEntity.level().nextEntityId
        textDisplayId = nmsEntity.level().nextEntityId

        val ridingOffset = nmsEntity
            .getPassengerRidingPosition(nmsEntity)
            .subtract(nmsEntity.position()).y

        val nametagOffset = nmsEntity.attachments.getClamped(EntityAttachment.NAME_TAG, 0, 0f).y

        // vanilla draws the top of a name tag 0.5 above its attachment, but a text display draws the bottom of its text at its origin
        textOffset = nametagOffset + CustomNamePackets.NAMETAG_OFFSET - CustomNamePackets.LINE_HEIGHT - ridingOffset
        passengerOffset = ridingOffset
    }

    fun startUpdateTask() {
        val task = targetEntity.scheduler.runAtFixedRate(plugin, {
            // players stay the same entity on death, respawn and world change, they're only gone once they quit
            if (targetEntity !is Player && !targetEntity.isValid) {
                CustomNameStorage.remove(targetEntity.uniqueId, this)
            } else {
                update()
            }
        }, { CustomNameStorage.remove(targetEntity.uniqueId, this) }, 1, 1)
        updateTask.set(task)
    }

    fun cancelUpdateTask() {
        updateTask.get()?.cancel()
    }

    fun update() {
        setTargetEntityInvisible(isTargetEntityInvisible())

        if (targetEntity.ticksLived % RIDER_SYNC_INTERVAL != 0) return
        if (!isVisible()) return

        val trackers = targetEntity.trackedBy
        if (trackers.isEmpty()) return

        val riderPacket = packets.getRiderPacket()
        for (player in trackers) {
            (player as CraftPlayer).handle.connection.send(riderPacket)
        }
    }

    override fun setName(nameCallback: (viewer: Audience) -> Component?) {
        this.nameCallback.set(nameCallback)
        syncData()
    }

    fun setTargetEntitySneaking(sneaking: Boolean) {
        if (targetEntitySneaking.getAndSet(sneaking) == sneaking) return
        syncData()
    }

    fun setCarryingPassengers(carrying: Boolean) {
        if (carryingPassengers.getAndSet(carrying) == carrying) return
        syncVisibility()
    }

    fun setTargetEntityInvisible(invisible: Boolean) {
        if (targetEntityInvisible.getAndSet(invisible) == invisible) return
        syncVisibility()
    }

    fun sendToClient(entity: Player) {
        if (isVisible()) {
            (entity as CraftPlayer).handle.connection.send(packets.initialSpawnPacket(entity))
        }
    }

    fun removeFromClient(entity: Player) {
        (entity as CraftPlayer).handle.connection.send(packets.getRemovePacket())
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden.set(hidden)
        syncVisibility()
    }

    override fun getName(viewer: Audience): Component? = nameCallback.get()(viewer)
    override fun getTargetEntityId(): UUID = targetEntity.uniqueId
    override fun isHidden(): Boolean = hidden.get()

    fun isVisible(): Boolean = !hidden.get() && !carryingPassengers.get() && !targetEntityInvisible.get()
    fun getSeeThroughDisplayId(): Int = seeThroughDisplayId
    fun getTextDisplayId(): Int = textDisplayId
    fun isTargetEntitySneaking(): Boolean = targetEntitySneaking.get()
    fun getTextOffset(): Double = textOffset
    fun getPassengerOffset(): Double = passengerOffset
    fun getTargetEntity(): Entity = targetEntity

    // vanilla hides the name tag of invisible entities and spectators
    private fun isTargetEntityInvisible(): Boolean =
        targetEntity.isInvisible || targetEntity is Player && targetEntity.gameMode == GameMode.SPECTATOR

    private fun syncData() {
        if (!isVisible()) return

        runOnTrackers { player ->
            (player as CraftPlayer).handle.connection.send(packets.syncDataPacket(player))
        }
    }

    private fun syncVisibility() {
        runOnTrackers { player ->
            if (isVisible()) sendToClient(player) else removeFromClient(player)
        }
    }

    private fun runOnTrackers(consumer: Consumer<Player>) {
        // trackers may only be accessed by the region owning the target entity, so we switch over to it first
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

    private companion object {
        const val RIDER_SYNC_INTERVAL = 20
    }
}
