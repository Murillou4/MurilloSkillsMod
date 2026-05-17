# NeoForge 1.21.1 Port

Status: blocked-needs-native-neoforge-port.

The previous jar was a `multi-loader-runtime` compatibility artifact, not the full MurilloSkills mod. It was removed from `dist` so it cannot be mistaken for a native port.

Native NeoForge support for `1.21.1` requires a real NeoForge/Mojang-mappings port of the Fabric source: NeoForge entrypoint, events, networking, player data attachments, keybindings, screens, and mixin descriptors.
