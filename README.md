# Bedframe

> [!WARNING]
> Bedframe is in early development, and you should *expect* problems. Batteries not included. See store for details.

Translation layer that converts textured/custom server-side Polymer blocks and items to native Bedrock representations.

> [!NOTE]
> If you are looking for general modding support on bedrock, see if [Hydraulic](https://github.com/GeyserMC/Hydraulic) works for you. Their conversion code is better than mine. Do note that Hydraulic *does not* support Polymer mods at the moment, and I haven't tested if both mods work at the same time.

## Requirements

- [Geyser-Fabric](https://geysermc.org/download)
- [Polymer (or a mod that uses it and its resource pack features)](https://modrinth.com/mod/polymer)

You also probably want Polymer's Auto-Host feature, but Bedframe doesn't require it.

## Compatibility

Polymer mods that use display entities are not supported at the moment. (Geyser simply doesn't support it). So far I've tested the following:

- [Tom's Server Additions](https://modrinth.com/mods?q=Tom%27s+Server+Additions) (EXCLUDING Decorations and Furniture)
- [More Furnaces](https://modrinth.com/mod/morefurnaces)
- [Televator](https://modrinth.com/mod/televator)
- [Navigation Compasses](https://modrinth.com/mod/navigation-compasses) (might be under review)

Bedframe does not touch non-textured blocks (so mods like [Server-Side Waystones](https://modrinth.com/mod/sswaystones) will work as expected)

Feel free to [make an issue](https://github.com/sylvxa/bedframe/issues/new) for incompatible mods.

## License

This mod is available under the CC0 license. Feel free to learn from it and incorporate it in your own project