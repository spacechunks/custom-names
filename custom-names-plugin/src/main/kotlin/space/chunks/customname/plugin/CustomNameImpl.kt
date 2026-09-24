package space.chunks.customname.plugin

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import space.chunks.customname.api.CustomName
import space.chunks.customname.plugin.util.SkeletonInteraction
import java.util.UUID
import java.util.function.Consumer

class CustomNameImpl(
    private val targetEntity: Entity
) : CustomName {

    private val interaction = SkeletonInteraction(this)

    // Target entity constants
    private val effectiveHeight: Double
    private val passengerOffset: Double

    // Custom name constants
    private val nametagEntityId: Int

    // States
    private var targetEntitySneaking = false

    private var nameCallback: (viewer: Audience) -> Component? = { null }
    private var hidden = false

    init {
        val nmsEntity = (targetEntity as CraftEntity).handle
        this.nametagEntityId = nmsEntity.level().nextEntityId

        val ridingOffset = nmsEntity
            .getPassengerRidingPosition(nmsEntity)
            .subtract(nmsEntity.position()).y

        val nametagOffset = nmsEntity.type.dimensions.height + 0.5f

        // First, negate the riding offset to get to the bounding of the entity's bounding box
        // Negate the natural nametag offset of interaction entities (0.5)
        // Add the actual offset of the nametag
        this.effectiveHeight = -ridingOffset - 0.5 + nametagOffset
        this.passengerOffset = ridingOffset
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
        this.syncData()
    }

    override fun setTargetEntitySneaking(targetEntitySneaking: Boolean) {
        this.targetEntitySneaking = targetEntitySneaking
        this.syncData()
    }

    fun sendToClient(entity: Player) {
        if (!hidden) {
            (entity as CraftPlayer).handle.connection.send(interaction.initialSpawnPacket(entity))
        }
    }

    fun removeFromClient(entity: Player) {
        (entity as CraftPlayer).handle.connection.send(interaction.removePacket())
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        this.runOnTrackers { player ->
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
        for (player in targetEntity.trackedBy) {
            consumer.accept(player)
        }
    }
}