# Custom Names
This is a Kotlin-Port of [Owen1212055's Custom Names POC](https://github.com/Owen1212055/CustomNames). It allows you to register custom names on top of entities. These entities are fully client side, and corretly synced between players.

## Usage

First, you have to add the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    // Paper
    compileOnly("space.chunks.custom-names:custom-names-api:1.2.0")
    // Minestom
    implementation("space.chunks.custom-names:custom-names-minestom:1.2.0")
}
```

### Paper

You can access the api via the Bukkit Service Manager. Make sure to add `CustomNames` as a plugin dependency in your `paper-plugin.yml` (see the [PaperMC Wiki](https://docs.papermc.io/paper/dev/getting-started/paper-plugins) for more information).

```kotlin
@EventHandler
fun onJoin(event: PlayerJoinEvent) {
    val player = event.player

    // Load this in onEnable and pass it to the listener class. Don't load it every time a player joins
    val customNameManager = Bukkit.getServicesManager().load(CustomNameManager::class.java) 
        ?: throw IllegalStateException("CustomNameManager not loaded")
    
    val customName = customNameManager.forEntity(player)
    customName.setName(Component.text(player.name, TextColor.color(0xFF6FFC)))
}
```

### Minestom

Call `CustomNamesMinestom.init()` once on startup, then access the manager directly:

```kotlin
CustomNamesMinestom.init()

val customName = CustomNamesMinestom.getManager().forEntity(player)
customName.setName(Component.text(player.username, TextColor.color(0xFF6FFC)))
```

## Future Plans
We ported this plugin to Kotlin, because we normally write all our plugins in Kotlin, and we plan to add some more features to this plugin.

Some of the features we plan to add are:
- [ ] Add support for multiple lines of text per entity
- [ ] Build some kind of configuration system to allow for easy configuration of the custom names

## Credits
This plugin was originally created by [Owen1212055](https://github.com/Owen1212055). We just ported it to Kotlin. For more information, check [his Plugin](https://github.com/Owen1212055/CustomNames) or [his gist](https://gist.github.com/Owen1212055/f5d59169d3a6a5c32f0c173d57eb199d)!