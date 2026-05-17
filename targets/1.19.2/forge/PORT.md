# Forge 1.19.2 Port

Status: blocked-needs-native-forge-port.

The previous `dist/1.19.2/forge/murilloskills-1.2.74+mc1.19.2-forge.jar` was removed because it was a runtime compatibility artifact and contained `RuntimeClientBridge` / `RuntimeSkillsScreenModern`.

A full native Forge target is not present yet. It needs a real Forge source set/build using Forge mappings and Forge-side equivalents for the Fabric entrypoints, networking, attachment/save data, keybindings, events, and mixin bootstrap before a jar can be published here.
