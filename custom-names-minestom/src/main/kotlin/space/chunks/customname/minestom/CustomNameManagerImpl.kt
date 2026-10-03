package space.chunks.customname.minestom

import net.minestom.server.entity.Entity
import space.chunks.customname.api.CustomNameManager

class CustomNameManagerImpl : CustomNameManager<Entity> {

    override fun forEntity(entity: Entity): CustomNameImpl {
        val existing = CustomNameStorage.getCustomPlayerName(entity.uuid)
        if (existing != null) return existing

        val customName = CustomNameImpl(entity)
        CustomNameStorage.register(entity.uuid, customName)
        customName.setHidden(false)

        return customName
    }

    override fun unregister(entity: Entity) {
        CustomNameStorage.remove(entity.uuid)
    }

}
