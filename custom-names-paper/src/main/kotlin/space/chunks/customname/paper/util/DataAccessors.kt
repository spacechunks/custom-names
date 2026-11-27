package space.chunks.customname.paper.util

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.Display
import org.joml.Vector3fc
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor

object DataAccessors {

    private val reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar()
    private val setPassengersConstructor: Constructor<ClientboundSetPassengersPacket> = initSetPassengersConstructor()

    val DATA_TRANSLATION_ID: EntityDataAccessor<Vector3fc> =
        get(Display::class.java, "DATA_TRANSLATION_ID")

    val DATA_BILLBOARD_RENDER_CONSTRAINTS_ID: EntityDataAccessor<Byte> =
        get(Display::class.java, "DATA_BILLBOARD_RENDER_CONSTRAINTS_ID")

    val DATA_TEXT_ID: EntityDataAccessor<Component> =
        get(Display.TextDisplay::class.java, "DATA_TEXT_ID")

    val DATA_LINE_WIDTH_ID: EntityDataAccessor<Int> =
        get(Display.TextDisplay::class.java, "DATA_LINE_WIDTH_ID")

    val DATA_BACKGROUND_COLOR_ID: EntityDataAccessor<Int> =
        get(Display.TextDisplay::class.java, "DATA_BACKGROUND_COLOR_ID")

    val DATA_TEXT_OPACITY_ID: EntityDataAccessor<Byte> =
        get(Display.TextDisplay::class.java, "DATA_TEXT_OPACITY_ID")

    val DATA_STYLE_FLAGS_ID: EntityDataAccessor<Byte> =
        get(Display.TextDisplay::class.java, "DATA_STYLE_FLAGS_ID")

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
                    EntityDataAccessor::class.java,
                ).invoke() as EntityDataAccessor<T>
        } catch (e: Throwable) {
            throw IllegalStateException("Couldn't access data accessor $name of ${clazz.simpleName}", e)
        }
    }
}
