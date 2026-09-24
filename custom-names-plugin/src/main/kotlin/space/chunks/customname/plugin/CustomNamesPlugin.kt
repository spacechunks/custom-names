package space.chunks.customname.plugin

import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import space.chunks.customname.api.CustomNameManager

class CustomNamesPlugin : JavaPlugin() {

    private val customNameManager = CustomNameManagerImpl(this)

    override fun onEnable() {
        customNameManager.registerListeners()

        Bukkit.getServicesManager().register(CustomNameManager::class.java, customNameManager, this, ServicePriority.Normal)
    }

    override fun onDisable() {
        customNameManager.stop()
    }

}