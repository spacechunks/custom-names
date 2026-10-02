package space.chunks.customname.minestom

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityPose
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.MetadataDef
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.BundlePacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import space.chunks.customname.api.CustomName
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

class CustomNameImpl(
    private val targetEntity: Entity
) : CustomName {

    // Target entity constants
    private val effectiveHeight: Double
    private val passengerOffset: Double

    // Custom name constants
    private val nametagEntityId: Int = Entity.generateId()

    // States
    private val trackedViewers = HashSet<Player>()
    private var targetEntitySneaking = false
    private var nameCallback: (viewer: Audience) -> Component? = { null }
    private var hidden = false

    init {
        val entityHeight = targetEntity.boundingBox.height()
        val ridingOffset = entityHeight * 0.75
        val nametagOffset = entityHeight + 0.5f

        effectiveHeight = -ridingOffset - 0.5 + nametagOffset
        passengerOffset = ridingOffset
    }

    fun update() {
        if (hidden) return
        if (trackedViewers.isEmpty()) return

        val riderPacket = getRiderPacket()
        for (player in trackedViewers) {
            player.sendPacket(riderPacket)
        }
    }

    fun updateViewers() {
        val currentViewers = targetEntity.viewers
        for (viewer in currentViewers) {
            if (trackedViewers.add(viewer)) {
                if (!hidden) {
                    sendSpawnPackets(viewer)
                }
            }
        }

        val iterator = trackedViewers.iterator()
        while (iterator.hasNext()) {
            val viewer = iterator.next()
            if (!currentViewers.contains(viewer)) {
                sendDestroyPacket(viewer)
                iterator.remove()
            }
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

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        for (player in trackedViewers) {
            if (hidden) {
                sendDestroyPacket(player)
            } else {
                sendSpawnPackets(player)
            }
        }
    }

    override fun getName(viewer: Audience): Component? = nameCallback(viewer)
    override fun getNametagId(): Int = nametagEntityId
    override fun getTargetEntityId(): UUID = targetEntity.uuid
    override fun isTargetEntitySneaking(): Boolean = targetEntitySneaking
    override fun getEffectiveHeight(): Double = effectiveHeight
    override fun getPassengerOffset(): Double = passengerOffset
    override fun isHidden(): Boolean = hidden

    // Utilities
    private fun syncData() {
        if (hidden) return

        for (player in trackedViewers) {
            player.sendPacket(syncDataPacket(player))
        }
    }

    private fun sendDestroyPacket(player: Player) {
        player.sendPacket(DestroyEntitiesPacket(nametagEntityId))
    }

    fun getTargetEntity(): Entity = targetEntity

    fun getRiderPacket(): SetPassengersPacket {
        val passengers = targetEntity.passengers
        val includeNametag = !hidden
        val size = passengers.size + (if (includeNametag) 1 else 0)
        val passengerIds = ArrayList<Int>(size)

        for (passenger in passengers) {
            passengerIds.add(passenger.entityId)
        }
        if (includeNametag) {
            passengerIds.add(nametagEntityId)
        }

        return SetPassengersPacket(targetEntity.entityId, passengerIds)
    }

    private fun syncDataPacket(viewer: Player): EntityMetaDataPacket {
        val entries = HashMap<Int, Metadata.Entry<*>>()
        entries[MetadataDef.CUSTOM_NAME.index()] = Metadata.OptComponent(getName(viewer))

        val sneakFlag = if (targetEntitySneaking) 1 shl 1 else 0
        val flagValue = (sneakFlag or 0x20).toByte()
        entries[MetadataDef.ENTITY_FLAGS.index()] = Metadata.Byte(flagValue)

        return EntityMetaDataPacket(nametagEntityId, entries)
    }

    private fun sendSpawnPackets(viewer: Player) {
        val location = targetEntity.position

        val spawnPacket = SpawnEntityPacket(
            nametagEntityId,
            UUID.randomUUID(),
            EntityType.INTERACTION,
            location.add(0.0, passengerOffset, 0.0),
            0f,
            0,
            Vec.ZERO
        )

        val metaEntries = HashMap<Int, Metadata.Entry<*>>()
        metaEntries[MetadataDef.Interaction.WIDTH.index()] = Metadata.Float(0.6f)
        metaEntries[MetadataDef.Interaction.HEIGHT.index()] = Metadata.Float(effectiveHeight.toFloat())
        metaEntries[MetadataDef.POSE.index()] = Metadata.Pose(EntityPose.CROAKING)
        metaEntries[MetadataDef.CUSTOM_NAME_VISIBLE.index()] = Metadata.Boolean(true)
        val initialCreatePacket = EntityMetaDataPacket(nametagEntityId, metaEntries)

        val syncData = syncDataPacket(viewer)
        val riderPacket = getRiderPacket()

        viewer.sendPackets(
            BundlePacket(),
            spawnPacket,
            initialCreatePacket,
            syncData,
            riderPacket,
            BundlePacket()
        )
    }

}
