package space.chunks.customname.minestom

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.MetadataDef
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.network.packet.server.play.BundlePacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import space.chunks.customname.api.CustomName
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class CustomNameImpl(
    private val targetEntity: Entity,
) : CustomName {

    private val textOffset = AtomicReference(calculateTextOffset())
    private val seeThroughDisplayId: Int = Entity.generateId()
    private val textDisplayId: Int = Entity.generateId()

    private val trackedViewers = ConcurrentHashMap.newKeySet<Player>()
    private val nameCallback = AtomicReference<(viewer: Audience) -> Component?>({ null })
    private val targetEntitySneaking = AtomicBoolean(targetEntity.isSneaking && targetEntity.vehicle == null)
    private val carryingPassengers = AtomicBoolean(targetEntity.hasPassenger())
    private val targetEntityInvisible = AtomicBoolean(isTargetEntityInvisible())
    private val hidden = AtomicBoolean()

    fun update() {
        if (!isVisible()) return
        if (trackedViewers.isEmpty()) return

        val riderPacket = getRiderPacket()
        for (player in trackedViewers) {
            player.sendPacket(riderPacket)
        }
    }

    fun updateViewers() {
        val currentViewers = targetEntity.viewers
        for (viewer in currentViewers) {
            if (trackedViewers.add(viewer) && isVisible()) {
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

    // the attachments scale with the entity, so the offset changes along with the scale attribute
    fun updateTextOffset() {
        val offset = calculateTextOffset()
        if (textOffset.getAndSet(offset) == offset) return
        if (!isVisible()) return

        for (player in trackedViewers) {
            player.sendPackets(
                BundlePacket(),
                translationPacket(seeThroughDisplayId),
                translationPacket(textDisplayId),
                BundlePacket(),
            )
        }
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden.set(hidden)
        syncVisibility()
    }

    override fun getName(viewer: Audience): Component? = nameCallback.get()(viewer)
    override fun getTargetEntityId(): UUID = targetEntity.uuid
    override fun isHidden(): Boolean = hidden.get()

    fun isVisible(): Boolean = !hidden.get() && !carryingPassengers.get() && !targetEntityInvisible.get()
    fun getTargetEntity(): Entity = targetEntity

    // vanilla hides the name tag of invisible entities and spectators
    fun isTargetEntityInvisible(): Boolean =
        targetEntity.isInvisible || targetEntity is Player && targetEntity.gameMode == GameMode.SPECTATOR

    fun getRiderPacket(): SetPassengersPacket {
        val passengerIds = targetEntity.passengers.map { passenger -> passenger.entityId }
        if (!isVisible()) return SetPassengersPacket(targetEntity.entityId, passengerIds)
        return SetPassengersPacket(targetEntity.entityId, passengerIds + seeThroughDisplayId + textDisplayId)
    }

    // vanilla draws the top of a name tag 0.5 above its attachment, but a text display draws the bottom of its text at its origin
    private fun calculateTextOffset(): Double =
        attachmentOffset("NAME_TAG") + NAMETAG_OFFSET - LINE_HEIGHT - attachmentOffset("PASSENGER")

    // minecraft falls back to the entity height for missing attachments, so we do the same
    private fun attachmentOffset(attachment: String): Double {
        val entityType = targetEntity.entityType
        val offset = entityType.entityAttachments(attachment)?.firstOrNull()?.y() ?: entityType.height()
        val scale = (targetEntity as? LivingEntity)?.getAttributeValue(Attribute.SCALE) ?: 1.0
        return offset * scale
    }

    private fun syncData() {
        if (!isVisible()) return

        for (player in trackedViewers) {
            player.sendPackets(BundlePacket(), *syncDataPackets(player), BundlePacket())
        }
    }

    private fun syncVisibility() {
        for (player in trackedViewers) {
            if (isVisible()) sendSpawnPackets(player) else sendDestroyPacket(player)
        }
    }

    private fun sendDestroyPacket(player: Player) {
        player.sendPacket(DestroyEntitiesPacket(listOf(seeThroughDisplayId, textDisplayId)))
    }

    // vanilla draws a name tag twice, a faded pass with background through walls and a solid pass on top, sneaking only keeps the faded one
    private fun syncDataPackets(viewer: Player): Array<EntityMetaDataPacket> {
        val name = getName(viewer)
        val sneaking = targetEntitySneaking.get()
        val text = name ?: Component.empty()
        val backgroundFlag = if (name == null) 0 else DEFAULT_BACKGROUND_FLAG
        val seeThroughFlag = if (sneaking) 0 else SEE_THROUGH_FLAG

        val seeThroughEntries = HashMap<Int, Metadata.Entry<*>>()
        seeThroughEntries[MetadataDef.TextDisplay.TEXT.index()] = Metadata.Component(text)
        seeThroughEntries[MetadataDef.TextDisplay.TEXT_DISPLAY_FLAGS.index()] =
            Metadata.Byte((backgroundFlag or seeThroughFlag).toByte())

        val textEntries = HashMap<Int, Metadata.Entry<*>>()
        textEntries[MetadataDef.TextDisplay.TEXT.index()] = Metadata.Component(if (sneaking) Component.empty() else text)

        return arrayOf(
            EntityMetaDataPacket(seeThroughDisplayId, seeThroughEntries),
            EntityMetaDataPacket(textDisplayId, textEntries),
        )
    }

    private fun sendSpawnPackets(viewer: Player) {
        viewer.sendPackets(
            BundlePacket(),
            spawnPacket(seeThroughDisplayId),
            initialDataPacket(seeThroughDisplayId, SEE_THROUGH_TEXT_OPACITY),
            spawnPacket(textDisplayId),
            initialDataPacket(textDisplayId, TEXT_OPACITY),
            *syncDataPackets(viewer),
            getRiderPacket(),
            BundlePacket(),
        )
    }

    private fun spawnPacket(entityId: Int): SpawnEntityPacket =
        SpawnEntityPacket(
            entityId,
            UUID.randomUUID(),
            EntityType.TEXT_DISPLAY,
            targetEntity.position.add(0.0, attachmentOffset("PASSENGER"), 0.0),
            0f,
            0,
            Vec.ZERO,
        )

    private fun initialDataPacket(entityId: Int, opacity: Byte): EntityMetaDataPacket {
        val entries = HashMap<Int, Metadata.Entry<*>>()
        entries[MetadataDef.Display.TRANSLATION.index()] = Metadata.Vector3(Vec(0.0, textOffset.get(), 0.0))
        entries[MetadataDef.Display.BILLBOARD_CONSTRAINTS.index()] = Metadata.Byte(BILLBOARD_CENTER)
        entries[MetadataDef.TextDisplay.LINE_WIDTH.index()] = Metadata.VarInt(Int.MAX_VALUE)
        entries[MetadataDef.TextDisplay.BACKGROUND_COLOR.index()] = Metadata.VarInt(0)
        entries[MetadataDef.TextDisplay.TEXT_OPACITY.index()] = Metadata.Byte(opacity)
        return EntityMetaDataPacket(entityId, entries)
    }

    private fun translationPacket(entityId: Int): EntityMetaDataPacket {
        val entries = HashMap<Int, Metadata.Entry<*>>()
        entries[MetadataDef.Display.TRANSLATION.index()] = Metadata.Vector3(Vec(0.0, textOffset.get(), 0.0))
        return EntityMetaDataPacket(entityId, entries)
    }

    private companion object {
        const val NAMETAG_OFFSET = 0.5
        const val LINE_HEIGHT = 9 * 0.025
        const val BILLBOARD_CENTER: Byte = 3
        const val SEE_THROUGH_FLAG = 2
        const val DEFAULT_BACKGROUND_FLAG = 4
        const val TEXT_OPACITY: Byte = -1
        const val SEE_THROUGH_TEXT_OPACITY: Byte = -128
    }
}
