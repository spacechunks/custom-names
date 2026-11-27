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

    fun syncDataPacket(viewer: Player): Packet<ClientGamePacketListener> =
        ClientboundBundlePacket(syncDataPackets(viewer))

    fun getRemovePacket(): Packet<ClientGamePacketListener> =
        ClientboundRemoveEntitiesPacket(customName.getSeeThroughDisplayId(), customName.getTextDisplayId())

    fun getRiderPacket(): Packet<ClientGamePacketListener> =
        DataAccessors.createSetPassengersPacket(customName.getTargetEntity().entityId, passengerIds())

    fun initialSpawnPacket(viewer: Player): Packet<*> =
        ClientboundBundlePacket(
            listOf<Packet<in ClientGamePacketListener>>(
                createPacket(customName.getSeeThroughDisplayId()),
                initialDataPacket(customName.getSeeThroughDisplayId(), SEE_THROUGH_TEXT_OPACITY),
                createPacket(customName.getTextDisplayId()),
                initialDataPacket(customName.getTextDisplayId(), TEXT_OPACITY),
            ) + syncDataPackets(viewer) + getRiderPacket(),
        )

    // vanilla draws a name tag twice, a faded pass with background through walls and a solid pass on top, sneaking only keeps the faded one
    private fun syncDataPackets(viewer: Player): List<Packet<in ClientGamePacketListener>> {
        val name = customName.getName(viewer)
        val sneaking = customName.isTargetEntitySneaking()

        val text = if (name == null) Component.empty() else PaperAdventure.asVanilla(name)
        val backgroundFlag = if (name == null) 0 else Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND.toInt()
        val seeThroughFlag = if (sneaking) 0 else Display.TextDisplay.FLAG_SEE_THROUGH.toInt()

        return listOf(
            ClientboundSetEntityDataPacket(
                customName.getSeeThroughDisplayId(),
                listOf<DataValue<*>>(
                    ofData(DataAccessors.DATA_TEXT_ID, text),
                    ofData(DataAccessors.DATA_STYLE_FLAGS_ID, (backgroundFlag or seeThroughFlag).toByte()),
                ),
            ),
            ClientboundSetEntityDataPacket(
                customName.getTextDisplayId(),
                listOf<DataValue<*>>(
                    ofData(DataAccessors.DATA_TEXT_ID, if (sneaking) Component.empty() else text),
                ),
            ),
        )
    }

    private fun passengerIds(): IntArray {
        val passengerIds = customName.getTargetEntity().passengers.map { passenger -> passenger.entityId }
        if (!customName.isVisible()) return passengerIds.toIntArray()
        return (passengerIds + customName.getSeeThroughDisplayId() + customName.getTextDisplayId()).toIntArray()
    }

    private fun initialDataPacket(entityId: Int, opacity: Byte): Packet<ClientGamePacketListener> =
        ClientboundSetEntityDataPacket(
            entityId,
            listOf<DataValue<*>>(
                ofData(DataAccessors.DATA_TRANSLATION_ID, Vector3f(0f, customName.getTextOffset().toFloat(), 0f)),
                ofData(DataAccessors.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, BILLBOARD_CENTER),
                ofData(DataAccessors.DATA_LINE_WIDTH_ID, Int.MAX_VALUE),
                ofData(DataAccessors.DATA_BACKGROUND_COLOR_ID, 0),
                ofData(DataAccessors.DATA_TEXT_OPACITY_ID, opacity),
            ),
        )

    private fun createPacket(entityId: Int): Packet<ClientGamePacketListener> {
        val location = customName.getTargetEntity().location

        return ClientboundAddEntityPacket(
            entityId,
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
        private const val TEXT_OPACITY: Byte = -1
        private const val SEE_THROUGH_TEXT_OPACITY: Byte = -128
    }
}
