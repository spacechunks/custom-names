package space.chunks.customname.plugin.util

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.Pose
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.util.Optional

object DataAccessors {

    private val reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar()
    private val setPassengersConstructor: Constructor<ClientboundSetPassengersPacket> = initSetPassengersConstructor()

    val DATA_SHARED_FLAGS_ID: EntityDataAccessor<Byte> =
        get(Entity::class.java, "DATA_SHARED_FLAGS_ID")

    val DATA_POSE: EntityDataAccessor<Pose> =
        get(Entity::class.java, "DATA_POSE")

    val DATA_CUSTOM_NAME: EntityDataAccessor<Optional<Component>> =
        get(Entity::class.java, "DATA_CUSTOM_NAME")

    val DATA_CUSTOM_NAME_VISIBLE: EntityDataAccessor<Boolean> =
        get(Entity::class.java, "DATA_CUSTOM_NAME_VISIBLE")

    // Interaction entity

    val DATA_WIDTH_ID: EntityDataAccessor<Float> = get(Interaction::class.java, "DATA_WIDTH_ID")

    val DATA_HEIGHT_ID: EntityDataAccessor<Float> = get(Interaction::class.java, "DATA_HEIGHT_ID")

    fun createSetPassengersPacket(vehicleId: Int, passengerIds: IntArray): ClientboundSetPassengersPacket {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(vehicleId)
        buf.writeVarIntArray(passengerIds)
        return setPassengersConstructor.newInstance(buf)
    }

    private fun initSetPassengersConstructor(): Constructor<ClientboundSetPassengersPacket> {
        val constructor = ClientboundSetPassengersPacket::class.java.getDeclaredConstructor(FriendlyByteBuf::class.java)
        constructor.isAccessible = true
        return constructor
    }

    private fun <T : Any> get(clazz: Class<*>, name: String): EntityDataAccessor<T> {
        try {
            @Suppress("UNCHECKED_CAST")
            return MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
                .findStaticGetter(
                    clazz,
                    reflectionRemapper.remapFieldName(clazz, name),
                    EntityDataAccessor::class.java
                ).invoke() as EntityDataAccessor<T>
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}
