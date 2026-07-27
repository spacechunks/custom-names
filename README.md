# Custom Names
This is a Kotlin-Port of [Owen1212055's Custom Names POC](https://github.com/Owen1212055/CustomNames). It allows you to register custom names on top of entities. These entities are fully client side, and corretly synced between players.

## Usage

First, you have to add the plugin to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("space.chunks.custom-names:custom-names-api:1.0.6")
}
```

You can access the api via the Bukkit Service Manager, but first you have to make sure, that your plugin loads after the CustomNames plugin. You can do this by adding the following to your `paper-plugin.yml`:

```yaml
# paper-plugin.yml
dependencies:
  server:
    CustomNames:
        load: BEFORE
        required: true
        join-classpath: true
```

*For more information on the `paper-plugin.yml`, check the [PaperMC Wiki](https://docs.papermc.io/paper/dev/getting-started/paper-plugins).*

Here is an example of how to use it inside the `PlayerJoinEvent`:

```kotlin
@EventHandler
fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    
    // Load this in the onEnable and pass it to the listener class. Don't load it every time a player joins
    val customNameManager = Bukkit.getServicesManager().load(CustomNameManager::class.java)?: throw IllegalStateException("CustomNameManager not loaded")
    
    val name = customNameManager.forEntity(entity)
    name.setName(Component.text(player.name, 0xFF6FFC))
}
```

## Future Plans
We ported this plugin to Kotlin, because we normally write all our plugins in Kotlin, and we plan to add some more features to this plugin.

Some of the features we plan to add are:
- [ ] Add support for multiple lines of text per entity
- [ ] Build some kind of configuration system to allow for easy configuration of the custom names

## Credits
This plugin was originally created by [Owen1212055](https://github.com/Owen1212055). We just ported it to Kotlin. For more information, check [his Plugin](https://github.com/Owen1212055/CustomNames) or [his gist](https://gist.github.com/Owen1212055/f5d59169d3a6a5c32f0c173d57eb199d)!