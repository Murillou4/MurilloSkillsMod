# Forge 1.18.2 Port

Status: blocked-needs-native-forge-port.

The previous `multi-loader-runtime` jar was removed from `dist/1.18.2/forge` because it was not equivalent to the native Fabric mod: it did not contain the real client GUI/keybind implementation and exposed runtime compatibility behavior instead.

A valid Forge 1.18.2 target still needs a native Forge source set/adapter for the full MurilloSkills feature set before it can be built and smoke-tested.
