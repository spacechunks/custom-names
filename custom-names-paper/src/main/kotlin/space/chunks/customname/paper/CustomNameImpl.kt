package space.chunks.customname.paper

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomName
import space.chunks.customname.paper.util.SkeletonInteraction
import java.util.UUID
import java.util.function.Consumer

class CustomNameImpl(
    private val plugin: JavaPlugin,
    private val targetEntity: Entity
) : CustomName {

    private val interaction = SkeletonInteraction(this)

    // Target entity constants
    private val effectiveHeight: Double
    private val passengerOffset: Double

    // Custom name constants
    private val nametagEntityId: Int

    // States
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

        val nametagOffset = nmsEntity.type.dimensions.height + 0.5f

        // First, negate the riding offset to get to the bounding of the entity's bounding box
        // Negate the natural nametag offset of interaction entities (0.5)
        // Add the actual offset of the nametag
        effectiveHeight = -ridingOffset - 0.5 + nametagOffset
        passengerOffset = ridingOffset
    }

    fun startUpdateTask() {
        // Runs on the thread owning the target entity
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

        val riderPacket: Packet<ClientGamePacketListener> = interaction.getRiderPacket()
        for (player in trackers) {
            (player as CraftPlayer).handle.connection.send(riderPacket)
        }
    }

    override fun setName(nameCallback: (viewer: Audience) -> Component?) {
        this.nameCallback = nameCallback
        syncData()
    }

    override fun setTargetEntitySneaking(targetEntitySneaking: Boolean) {
        this.targetEntitySneaking = targetEntitySneaking
        syncData()
    }

    fun sendToClient(entity: Player) {
        if (!hidden) {
            (entity as CraftPlayer).handle.connection.send(interaction.initialSpawnPacket(entity))
        }
    }

    fun removeFromClient(entity: Player) {
        (entity as CraftPlayer).handle.connection.send(interaction.getRemovePacket())
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        runOnTrackers { player ->
            if (hidden) removeFromClient(player) else sendToClient(player)
        }
    }

    override fun getName(viewer: Audience): Component? = nameCallback(viewer)
    override fun getNametagId(): Int = nametagEntityId
    override fun getTargetEntityId(): UUID = targetEntity.uniqueId
    override fun isTargetEntitySneaking(): Boolean = targetEntitySneaking
    override fun getEffectiveHeight(): Double = effectiveHeight
    override fun getPassengerOffset(): Double = passengerOffset
    override fun isHidden(): Boolean = hidden

    fun getTargetEntity(): Entity = targetEntity

    // Utilities
    private fun syncData() {
        if (hidden) return

        runOnTrackers { player ->
            val packet = interaction.syncDataPacket(player)
            (player as CraftPlayer).handle.connection.send(packet)
        }
    }

    private fun runOnTrackers(consumer: Consumer<Player>) {
        // The trackers may only be accessed by the thread owning the target entity
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