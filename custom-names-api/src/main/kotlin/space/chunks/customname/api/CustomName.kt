package space.chunks.customname.api

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import java.util.UUID

/**
 * A name shown above an entity.
 */
interface CustomName {

    /**
     * Sets the [name] shown to every viewer, or removes it if `null`.
     */
    fun setName(name: Component?) = setName { name }

    /**
     * Sets a name per viewer. The [nameCallback] returns the name for a viewer, or `null` to show none.
     */
    fun setName(nameCallback: (viewer: Audience) -> Component?)

    /**
     * Hides the name for every viewer if [hidden] is `true`, otherwise shows it again.
     */
    fun setHidden(hidden: Boolean)

    /**
     * Returns the name shown to the [viewer], or `null` if none is shown.
     */
    fun getName(viewer: Audience): Component?

    /**
     * Returns the [UUID] of the entity this name is shown above.
     */
    fun getTargetEntityId(): UUID

    /**
     * Returns `true` if the name is hidden.
     */
    fun isHidden(): Boolean

}
