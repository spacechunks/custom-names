package space.chunks.customname.paper.util

import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import org.joml.Vector3f
import space.chunks.customname.paper.CustomNameImpl
import java.util.UUID

class CustomNamePackets(
    private val customName: CustomNameImpl,
) {

    fun syncDataPacket(viewer: Player): Packet<ClientGamePacketListener> {
        val name = customName.getName(viewer)
        val sneaking = customName.isTargetEntitySneaking()

        val text = if (name == null) Component.empty() else PaperAdventure.asVanilla(name)
        val backgroundFlag = if (name == null) 0 else Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND.toInt()
        val seeThroughFlag = if (sneaking) 0 else Display.TextDisplay.FLAG_SEE_THROUGH.toInt()
        val opacity = if (sneaking) SNEAKING_TEXT_OPACITY else DEFAULT_TEXT_OPACITY

        return ClientboundSetEntityDataPacket(
            customName.getNametagId(),
            listOf<DataValue<*>>(
                ofData(DataAccessors.DATA_TEXT_ID, text),
                ofData(DataAccessors.DATA_STYLE_FLAGS_ID, (backgroundFlag or seeThroughFlag).toByte()),
                ofData(DataAccessors.DATA_TEXT_OPACITY_ID, opacity),
            ),
        )
    }

    fun getRemovePacket(): Packet<ClientGamePacketListener> =
        ClientboundRemoveEntitiesPacket(customName.getNametagId())

    fun getRiderPacket(): Packet<ClientGamePacketListener> =
        DataAccessors.createSetPassengersPacket(customName.getTargetEntity().entityId, passengerIds())

    private fun passengerIds(): IntArray {
        val passengerIds = customName.getTargetEntity().passengers.map { passenger -> passenger.entityId }
        if (customName.isHidden()) return passengerIds.toIntArray()
        return (passengerIds + customName.getNametagId()).toIntArray()
    }

    fun initialSpawnPacket(viewer: Player): Packet<*> {
        val initialCreatePacket = ClientboundSetEntityDataPacket(
            customName.getNametagId(),
            listOf<DataValue<*>>(
                ofData(DataAccessors.DATA_TRANSLATION_ID, Vector3f(0f, customName.getTextOffset().toFloat(), 0f)),
                ofData(DataAccessors.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, BILLBOARD_CENTER),
                ofData(DataAccessors.DATA_LINE_WIDTH_ID, Int.MAX_VALUE),
                ofData(DataAccessors.DATA_BACKGROUND_COLOR_ID, 0),
            ),
        )

        return ClientboundBundlePacket(
            listOf<Packet<in ClientGamePacketListener>>(
                createPacket(),
                initialCreatePacket,
                syncDataPacket(viewer),
                getRiderPacket(),
            ),
        )
    }

    private fun createPacket(): Packet<ClientGamePacketListener> {
        val location = customName.getTargetEntity().location

        return ClientboundAddEntityPacket(
            customName.getNametagId(),
            UUID.randomUUID(),
            location.x(),
            location.y() + customName.getPassengerOffset(),
            location.z(),
            0f,
            0f,
            EntityTypes.TEXT_DISPLAY,
            0,
            Vec3.ZERO,
            0.0,
        )
    }

    private fun <T : Any> ofData(data: EntityDataAccessor<T>, value: T): DataValue<T> =
        DataItem(data, value).value()

    companion object {
        const val NAMETAG_OFFSET = 0.5
        const val LINE_HEIGHT = 9 * 0.025
        private const val BILLBOARD_CENTER: Byte = 3
        private const val DEFAULT_TEXT_OPACITY: Byte = -1
        private const val SNEAKING_TEXT_OPACITY: Byte = -128
    }
}
