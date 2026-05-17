# Forge 1.20.1 Port

Status: blocked-needs-native-forge-port.

The previous jar was a `multi-loader-runtime` compatibility artifact, not the full MurilloSkills mod. It was removed from `dist` so it cannot be mistaken for a native port.

Native Forge support for `1.20.1` requires a real ForgeGradle/Mojang-mappings port of the Fabric source: Forge entrypoint, events, networking, player data storage/capabilities, keybindings, screens, and mixin descriptors.
