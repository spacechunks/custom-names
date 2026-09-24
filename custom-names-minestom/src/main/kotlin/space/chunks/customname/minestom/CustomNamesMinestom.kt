package space.chunks.customname.minestom

import net.minestom.server.entity.Entity
import space.chunks.customname.api.CustomNameManager

object CustomNamesMinestom {

    private val customNameManager = CustomNameManagerImpl()

    fun getManager(): CustomNameManager<Entity> = customNameManager

    fun enable() {
        customNameManager.registerListeners()
    }

    fun disable() {
        customNameManager.stop()
    }

}