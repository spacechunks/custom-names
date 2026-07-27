package space.chunks.customname.plugin

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CustomNameStorage {

    private val customPlayerNameMap: MutableMap<UUID, CustomNameImpl> = ConcurrentHashMap()

    fun getCustomPlayerName(uuid: UUID): CustomNameImpl? {
        return customPlayerNameMap[uuid]
    }

    fun registerNew(entityId: UUID, name: CustomNameImpl) {
        customPlayerNameMap[entityId] = name
    }

    fun remove(uuid: UUID): CustomNameImpl? {
        val name = customPlayerNameMap.remove(uuid)
        name?.close()

        return name
    }

}