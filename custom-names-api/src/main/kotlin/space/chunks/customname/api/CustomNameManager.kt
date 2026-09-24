package space.chunks.customname.api

interface CustomNameManager<E : Any> {

    /**
     * Creates a new custom name for the given entity.
     * @param entity The entity to create a custom name for.
     * @return The custom name.
     */
    fun forEntity(entity: E): CustomName

    /**
     * Unregisters the custom name for the given entity.
     * @param entity The entity to unregister the custom name for.
     */
    fun unregister(entity: E)

}