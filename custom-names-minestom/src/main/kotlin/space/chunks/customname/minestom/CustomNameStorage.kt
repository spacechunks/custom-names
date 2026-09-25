package space.chunks.customname.minestom

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CustomNameStorage {

    private val storage = ConcurrentHashMap<UUID, CustomNameImpl>()

    fun getAll(): Collection<CustomNameImpl> = storage.values

    fun getCustomPlayerName(uuid: UUID): CustomNameImpl? = storage[uuid]

    fun register(entityId: UUID, name: CustomNameImpl) {
        storage[entityId] = name
    }

    fun remove(uuid: UUID): CustomNameImpl? {
        val name = storage.remove(uuid)
        name?.setHidden(true)
        return name
    }

    fun clear() {
        storage.values.forEach { name -> name.setHidden(true) }
        storage.clear()
    }

}
