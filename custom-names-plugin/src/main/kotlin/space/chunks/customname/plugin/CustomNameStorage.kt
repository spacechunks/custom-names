package space.chunks.customname.plugin

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CustomNameStorage {

    private val storage: MutableMap<UUID, CustomNameImpl> = ConcurrentHashMap()

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
        for (name in storage.values) {
            name.setHidden(true)
        }
        storage.clear()
    }

}