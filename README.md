# MCglTF

A 3D model loader library which load glTF format file and prepare the required techniques to render the model for
Minecraft Modding enviroment.
Various features from glTF spec are available but still remain a good compatibility and performance.

[![](https://cf.way2muchnoise.eu/title/mcgltf.svg)](https://www.curseforge.com/minecraft/mc-mods/mcgltf) [![](https://cf.way2muchnoise.eu/versions/mcgltf.svg)](https://www.curseforge.com/minecraft/mc-mods/mcgltf) [![](https://cf.way2muchnoise.eu/mcgltf.svg)](https://www.curseforge.com/minecraft/mc-mods/mcgltf)

## Usages

The example codes for rendering Block, Item, and Entity

- https://github.com/ModularMods/MCglTF-Example

## Features

- [x] GLTF format (Embedded resources or via ResourceLocation)
- [x] GLB format
- [x] UVs
- [x] Normals
- [x] Tangents
- [x] Vertex colors
- [x] Materials (
  Require [OptiFine](https://github.com/ModularMods/MCglTF/wiki/How-to-make-PBR-Materials-working-with-OptiFine)
  or [Iris Shaders](https://github.com/ModularMods/MCglTF/wiki/How-to-make-PBR-Materials-working-with-Iris-Shaders) and
  supported ShaderPack for PBR and Normal map)
- [x] Textures
- [ ] Mutiple texture coordinates (For compatibility reason with Vanilla)
- [x] Rig
- [x] Animations (multiple)
- [x] Morph targets
- [x] Zero-scale node culling (https://github.com/KhronosGroup/glTF/pull/2059)

## Credit

- JglTF by javagl : https://github.com/javagl/JglTF
- Mikk Tangent Generator by jMonkeyEngine : https://github.com/jMonkeyEngine/jmonkeyengine