package space.chunks.customname.minestom

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
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
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

class CustomNameImpl(
    private val targetEntity: Entity,
) : CustomName {

    private val textOffset: Double
    private val passengerOffset: Double
    private val nametagEntityId: Int = Entity.generateId()

    private val trackedViewers = HashSet<Player>()
    private var targetEntitySneaking = false
    private var nameCallback: (viewer: Audience) -> Component? = { null }
    private var hidden = false

    init {
        // vanilla draws the top of a name tag 0.5 above the entity, but a text display draws the bottom of its text at its origin
        textOffset = NAMETAG_OFFSET - LINE_HEIGHT
        passengerOffset = targetEntity.boundingBox.height()
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
            if (trackedViewers.add(viewer) && !hidden) {
                sendSpawnPackets(viewer)
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

    fun setTargetEntitySneaking(targetEntitySneaking: Boolean) {
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
    override fun getTargetEntityId(): UUID = targetEntity.uuid
    override fun isHidden(): Boolean = hidden

    fun getTargetEntity(): Entity = targetEntity

    fun getRiderPacket(): SetPassengersPacket {
        val passengerIds = targetEntity.passengers.map { passenger -> passenger.entityId }
        if (hidden) return SetPassengersPacket(targetEntity.entityId, passengerIds)
        return SetPassengersPacket(targetEntity.entityId, passengerIds + nametagEntityId)
    }

    private fun syncData() {
        if (hidden) return

        for (player in trackedViewers) {
            player.sendPacket(syncDataPacket(player))
        }
    }

    private fun sendDestroyPacket(player: Player) {
        player.sendPacket(DestroyEntitiesPacket(nametagEntityId))
    }

    private fun syncDataPacket(viewer: Player): EntityMetaDataPacket {
        val name = getName(viewer)
        val backgroundFlag = if (name == null) 0 else DEFAULT_BACKGROUND_FLAG
        val seeThroughFlag = if (targetEntitySneaking) 0 else SEE_THROUGH_FLAG
        val opacity = if (targetEntitySneaking) SNEAKING_TEXT_OPACITY else DEFAULT_TEXT_OPACITY

        val entries = HashMap<Int, Metadata.Entry<*>>()
        entries[MetadataDef.TextDisplay.TEXT.index()] = Metadata.Component(name ?: Component.empty())
        entries[MetadataDef.TextDisplay.TEXT_DISPLAY_FLAGS.index()] = Metadata.Byte((backgroundFlag or seeThroughFlag).toByte())
        entries[MetadataDef.TextDisplay.TEXT_OPACITY.index()] = Metadata.Byte(opacity)

        return EntityMetaDataPacket(nametagEntityId, entries)
    }

    private fun sendSpawnPackets(viewer: Player) {
        val spawnPacket = SpawnEntityPacket(
            nametagEntityId,
            UUID.randomUUID(),
            EntityType.TEXT_DISPLAY,
            targetEntity.position.add(0.0, passengerOffset, 0.0),
            0f,
            0,
            Vec.ZERO,
        )

        val metaEntries = HashMap<Int, Metadata.Entry<*>>()
        metaEntries[MetadataDef.Display.TRANSLATION.index()] = Metadata.Vector3(Vec(0.0, textOffset, 0.0))
        metaEntries[MetadataDef.Display.BILLBOARD_CONSTRAINTS.index()] = Metadata.Byte(BILLBOARD_CENTER)
        metaEntries[MetadataDef.TextDisplay.LINE_WIDTH.index()] = Metadata.VarInt(Int.MAX_VALUE)
        metaEntries[MetadataDef.TextDisplay.BACKGROUND_COLOR.index()] = Metadata.VarInt(0)

        viewer.sendPackets(
            BundlePacket(),
            spawnPacket,
            EntityMetaDataPacket(nametagEntityId, metaEntries),
            syncDataPacket(viewer),
            getRiderPacket(),
            BundlePacket(),
        )
    }

    private companion object {
        const val NAMETAG_OFFSET = 0.5
        const val LINE_HEIGHT = 9 * 0.025
        const val BILLBOARD_CENTER: Byte = 3
        const val SEE_THROUGH_FLAG = 2
        const val DEFAULT_BACKGROUND_FLAG = 4
        const val DEFAULT_TEXT_OPACITY: Byte = -1
        const val SNEAKING_TEXT_OPACITY: Byte = -128
    }
}
