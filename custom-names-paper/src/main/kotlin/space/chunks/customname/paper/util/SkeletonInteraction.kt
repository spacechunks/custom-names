package space.chunks.customname.paper.util

import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import space.chunks.customname.paper.CustomNameImpl
import java.util.Optional
import java.util.UUID

/**
 * This classed is used for sending packets related to the interaction entity
 * sent to the client.
 */
class SkeletonInteraction(
    private val customName: CustomNameImpl
) {

    fun removePacket(): Packet<ClientGamePacketListener> =
        ClientboundRemoveEntitiesPacket(this.customName.getNametagId())

    fun syncDataPacket(viewer: Player): Packet<ClientGamePacketListener> {
        val data: MutableList<DataValue<*>> = ArrayList()
        data.add(
            ofData(
                DataAccessors.DATA_CUSTOM_NAME,
                Optional.ofNullable(PaperAdventure.asVanilla(this.customName.getName(viewer)))
            )
        )

        val sneakFlag = if (this.customName.isTargetEntitySneaking()) 1 shl 1 else 0
        val value = (sneakFlag or 0x20).toByte()
        data.add(ofData(DataAccessors.DATA_SHARED_FLAGS_ID, value))

        return ClientboundSetEntityDataPacket(this.customName.getNametagId(), data)
    }

    fun getRiderPacket(): Packet<ClientGamePacketListener> =
        DataAccessors.createSetPassengersPacket(
            this.customName.getTargetEntity().entityId,
            this.passengerIds()
        )

    private fun passengerIds(): IntArray {
        val passengers: List<Entity> = this.customName.getTargetEntity().passengers
        val includeNametag = !this.customName.isHidden()
        val size = passengers.size + (if (includeNametag) 1 else 0)
        val passengerIds = IntArray(size)

        for (i in passengers.indices) {
            passengerIds[i] = passengers[i].entityId
        }
        if (includeNametag) {
            passengerIds[passengers.size] = this.customName.getNametagId()
        }
        return passengerIds
    }

    fun initialSpawnPacket(viewer: Player): Packet<*> {
        val initialCreatePacket = ClientboundSetEntityDataPacket(
            this.customName.getNametagId(), listOf<DataValue<*>>(
                ofData(DataAccessors.DATA_WIDTH_ID, 0.6f),
                ofData<Float>(DataAccessors.DATA_HEIGHT_ID, this.customName.getEffectiveHeight().toFloat()),
                ofData(DataAccessors.DATA_POSE, Pose.CROAKING),
                ofData(DataAccessors.DATA_CUSTOM_NAME_VISIBLE, true)
            )
        )
        val syncData = syncDataPacket(viewer)

        return ClientboundBundlePacket(
            listOf<Packet<in ClientGamePacketListener>>(
                createPacket(),
                initialCreatePacket,
                syncData,
                this.getRiderPacket()
            )
        )
    }

    private fun createPacket(): Packet<ClientGamePacketListener> {
        val location: Location = this.customName.getTargetEntity().location

        return ClientboundAddEntityPacket(
            this.customName.getNametagId(),
            UUID.randomUUID(),
            location.x(),
            location.y() + this.customName.getPassengerOffset(),
            location.z(),
            0f,
            0f,
            EntityTypes.INTERACTION,
            0,
            Vec3.ZERO,
            0.0,
        )
    }

    private fun <T : Any> ofData(data: EntityDataAccessor<T>, value: T): DataValue<T> =
        DataItem(data, value).value()

}