# AdvancedChatCore

This is the base mod of all AdvancedChat modules and features. It provides the foundation and
framework the AdvancedChat mods build on, adds the internal features used by other modules, and on
its own adds the ability to display the time a message was sent.

> **Refurbished fork.** DarkKronicle archived the original project. This fork brings AdvancedChatCore
> up to **Minecraft 26.2** and modernises the codebase. Because 26.x ships unobfuscated and dropped
> Yarn/ProGuard mappings, the whole mod was ported to the new Mojang names and the new GUI
> render-state model, and the logging/IO layer was modernised. See the original
> [DarkKronicle/AdvancedChatCore](https://github.com/DarkKronicle/AdvancedChatCore) for history.

## Requirements

| | Version |
| --- | --- |
| Minecraft | **26.2** |
| Java | **25** (required by Minecraft 26.x) |
| Fabric Loader | 0.19.0+ |

## Dependencies

[MaLiLib](https://modrinth.com/mod/malilib) and [Fabric API](https://modrinth.com/mod/fabric-api)
are **required** for this mod to run. For Minecraft 26.x use the
[sakura-ryoko MaLiLib builds](https://modrinth.com/mod/malilib/versions) (masa's original MaLiLib does
not target 26.x).

[Mod Menu](https://modrinth.com/mod/modmenu) is strongly recommended, as it lets you open the
configuration screen easily.

## Configuration

You can either manually edit the config file at `~/.minecraft/config/advancedchat/advancedchatcore.json`,
or open the configuration screen through Mod Menu (see **Dependencies**).

## About AdvancedChat Modules

AdvancedChat splits its features into several different mods, all of which depend on AdvancedChatCore.
This simplifies development and lets users pick and choose the features they want. The main
AdvancedChat mod bundles the modules together.
[Full module list](https://github.com/DarkKronicle/AdvancedChatCore/wiki/Modules-List).

## Developers

To use AdvancedChatCore within your own mod you can pull it from [jitpack](https://jitpack.io/) with
maven:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    modImplementation 'com.github.flyingfinger1:AdvancedChatCore:VERSION'
}
```

To have the core treat your mod as a module, add `"acmodule": true` to the `custom` block of your
`fabric.mod.json`:

```json
{
  "custom": {
    "acmodule": true
  }
}
```

Reference the [example mod](https://github.com/DarkKronicle/AdvancedChatModuleTemplate) for individual
use cases.

## Building

All dependencies are resolved automatically through Gradle. Build with:

```
./gradlew build
```

The build requires a **JDK 25** toolchain (Minecraft 26.x). The output jar is written to
`build/libs/` with the bundled libraries (MathParser, Konstruct, NightConfig, owo, commons-csv)
included as jar-in-jar.

## Credits

- Code & Mastermind: DarkKronicle
- Update to 1.16.3: lmichaelis
- Logo & Proofreading: Chronos22
- 26.2 port & modernisation: community fork

Libraries:
- [MathParser](http://mathparser.org/)
- [owo](https://github.com/maowimpl/owo)
- [Konstruct](https://github.com/DarkKronicle/Konstruct)
