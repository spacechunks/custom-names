package space.chunks.customname.plugin.util

import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.Pose
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import java.lang.invoke.MethodHandles
import java.util.*

object DataAccessors {

    private val reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar()

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
