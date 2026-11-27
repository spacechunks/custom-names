package space.chunks.customname.api

/**
 * Creates and removes the [CustomName]s of entities of type [E].
 */
interface CustomNameManager<E : Any> {

    /**
     * Returns the [CustomName] of the [entity], creating it if it doesn't exist yet.
     */
    fun forEntity(entity: E): CustomName

    /**
     * Removes the [CustomName] of the [entity].
     */
    fun unregister(entity: E)

}
