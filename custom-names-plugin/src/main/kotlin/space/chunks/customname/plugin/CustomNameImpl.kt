package space.chunks.customname.plugin

import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import space.chunks.customname.api.CustomName
import space.chunks.customname.plugin.util.SkeletonInteraction
import java.util.function.Consumer

class CustomNameImpl(
    private val plugin: JavaPlugin,
    private val targetEntity: Entity
) : CustomName {

    private val interaction = SkeletonInteraction(this)

    // Target entity constants
    private var effectiveHeight = 0.0
    private var passengerOffset = 0.0

    // Custom name constants
    private var nametagEntityId = 0

    // States
    private var targetEntitySneaking = false

    private var nameCallback: (viewer: Player) -> Component? = { null }
    private var hidden = false

    private var task: BukkitTask? = null

    init {
        val nmsEntity = (targetEntity as CraftEntity).handle
        this.nametagEntityId = nmsEntity.level().nextEntityId

        val ridingOffset = nmsEntity
            .getPassengerRidingPosition(nmsEntity)
            .subtract(nmsEntity.position()).y

        val nametagOffset = nmsEntity.type.dimensions.height + 0.5f

        // First, negate the riding offset to get to the bounding of the entity's bounding box
        // Negate the natural nametag offset of interaction entities (0.5)
        // Add the actual offset of the nametag
        this.effectiveHeight = -ridingOffset - 0.5 + nametagOffset
        this.passengerOffset = ridingOffset

        this.task = object : BukkitRunnable() {
            override fun run() {
                val riderPacket: Packet<ClientGamePacketListener> =
                    this@CustomNameImpl.interaction.getRiderPacket()

                for (player in targetEntity.trackedPlayers) {
                    (player as CraftPlayer).handle.connection.send(riderPacket)
                }
            }
        }.runTaskTimer(plugin, 20, 20)
    }

    override fun setName(nameCallback: (viewer: Player) -> Component?) {
        this.nameCallback = nameCallback
        this.syncData()
    }

    override fun setTargetEntitySneaking(targetEntitySneaking: Boolean) {
        this.targetEntitySneaking = targetEntitySneaking
        this.syncData()
    }

    fun sendToClient(entity: Player) {
        if (!hidden) {
            (entity as CraftPlayer).handle.connection
                .send(interaction.initialSpawnPacket(entity))
        }
    }

    fun removeFromClient(entity: Player) {
        (entity as CraftPlayer).handle.connection.send(interaction.removePacket())
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        this.runOnTrackers { player ->
            if (hidden) removeFromClient(player) else sendToClient(player)
        }
    }

    override fun getName(viewer: Player): Component? = nameCallback(viewer)
    override fun getNametagId(): Int = nametagEntityId
    override fun getTargetEntity(): Entity = targetEntity
    override fun isTargetEntitySneaking(): Boolean = targetEntitySneaking
    override fun getEffectiveHeight(): Double = effectiveHeight
    override fun getPassengerOffset(): Double = passengerOffset
    override fun isHidden(): Boolean = hidden

    // Utilities
    private fun syncData() {
        if (hidden) return

        runOnTrackers { player ->
            val packet = interaction.syncDataPacket(player)
            (player as CraftPlayer).handle.connection.send(packet)
        }
    }

    private fun runOnTrackers(consumer: Consumer<Player>) {
        for (player in targetEntity.trackedBy) {
            consumer.accept(player)
        }
    }

    fun close() {
        task?.cancel()
    }
}
