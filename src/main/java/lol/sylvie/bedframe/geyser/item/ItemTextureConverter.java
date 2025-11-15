package lol.sylvie.bedframe.geyser.item;

import lol.sylvie.bedframe.util.BedframeConstants;
import lol.sylvie.bedframe.util.PathHelper;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Exports Java (Polymer) item textures into Bedrock pack textures/items/,
 * records atlas keys, and guarantees a placeholder if the PNG isn't found.
 */
public final class ItemTextureConverter {
    private static final Set<String> EXPORTED_ICON_KEYS = new HashSet<>();
    private static Path packTexturesItemsDir;

    private ItemTextureConverter() {}

    /**
     * Initialize output directory: <packRoot>/textures/items
     */
    public static void init(Path packRoot) throws IOException {
        Path texturesDir = packRoot.resolve("textures");
        PathHelper.createDirectoryOrThrow(texturesDir); // ensure parent exists
        packTexturesItemsDir = texturesDir.resolve("items");
        PathHelper.createDirectoryOrThrow(packTexturesItemsDir);
        BedframeConstants.LOGGER.info("ItemTextureConverter initialized at {}", packTexturesItemsDir);
    }

    /**
     * Export an item's PNG. Returns the atlas key (path) or a placeholder key.
     */
    public static Optional<String> exportItemTexture(Identifier javaId) {
        String namespace = javaId.getNamespace();
        String path = javaId.getPath();

        // Primary: assets/<ns>/textures/item/<path>.png
        String javaTexturePath = "assets/" + namespace + "/textures/item/" + path + ".png";
        if (copyIfPresent(javaTexturePath, path)) {
            return Optional.of(path);
        }

        // Fallback: assets/<ns>/textures/items/<path>.png
        String altJavaTexturePath = "assets/" + namespace + "/textures/items/" + path + ".png";
        if (copyIfPresent(altJavaTexturePath, path)) {
            return Optional.of(path);
        }

        // Mod container file lookup
        Optional<ModContainer> modContainerOpt = FabricLoader.getInstance().getModContainer(namespace);
        if (modContainerOpt.isPresent()) {
            ModContainer mod = modContainerOpt.get();
            Optional<Path> filePath = mod.findPath("assets/" + namespace + "/textures/item/" + path + ".png");
            if (filePath.isEmpty()) {
                filePath = mod.findPath("assets/" + namespace + "/textures/items/" + path + ".png");
            }
            if (filePath.isPresent()) {
                try {
                    Path out = packTexturesItemsDir.resolve(path + ".png");
                    Files.copy(filePath.get(), out, StandardCopyOption.REPLACE_EXISTING);
                    EXPORTED_ICON_KEYS.add(path);
                    BedframeConstants.LOGGER.info("Exported mod file texture for {} -> {}", javaId, out);
                    return Optional.of(path);
                } catch (IOException e) {
                    BedframeConstants.LOGGER.warn("Failed to export texture from mod path for {}: {}", javaId, e.getMessage());
                }
            }
        }

        // Final fallback: placeholder
        String key = ensurePlaceholder(path);
        BedframeConstants.LOGGER.warn("Using placeholder icon for {}", javaId);
        return Optional.of(key);
    }

    private static boolean copyIfPresent(String resourcePath, String key) {
        try (InputStream stream = ResourceHelper.getResource(resourcePath)) {
            if (stream != null) {
                Path out = packTexturesItemsDir.resolve(key + ".png");
                Files.copy(stream, out, StandardCopyOption.REPLACE_EXISTING);
                EXPORTED_ICON_KEYS.add(key);
                BedframeConstants.LOGGER.info("Exported item texture {} -> {}", resourcePath, out);
                return true;
            }
        } catch (Exception e) {
            // swallow and log; we'll try other sources or placeholder
            BedframeConstants.LOGGER.debug("Texture not found at {}: {}", resourcePath, e.getMessage());
        }
        return false;
    }

    /**
     * Ensure a small transparent PNG exists so Bedrock keeps the definition.
     */
    public static String ensurePlaceholder(String key) {
        try {
            Path out = packTexturesItemsDir.resolve(key + ".png");
            if (!Files.exists(out)) {
                Files.write(out, PlaceholderPng.transparent16x16());
            }
            EXPORTED_ICON_KEYS.add(key);
            return key;
        } catch (IOException e) {
            BedframeConstants.LOGGER.warn("Failed to write placeholder icon {}: {}", key, e.getMessage());
            return key;
        }
    }

    /**
     * Read-only set of atlas keys exported or ensured.
     */
    public static Set<String> getExportedIconKeys() {
        return Collections.unmodifiableSet(EXPORTED_ICON_KEYS);
    }

    /**
     * Minimal transparent 16×16 RGBA PNG placeholder bytes.
     */
    private static final class PlaceholderPng {
        static byte[] transparent16x16() {
            return new byte[]{
                    (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
                    0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
                    0x00,0x00,0x00,0x10,0x00,0x00,0x00,0x10,0x08,0x06,0x00,0x00,0x00,(byte)0x1F,(byte)0xF3,(byte)0xFF,(byte)0x61,
                    0x00,0x00,0x00,0x0A,0x49,0x44,0x41,0x54,0x78,(byte)0x9C,
                    0x63,0x00,0x01,0x00,0x00,0x05,0x00,0x01,(byte)0x0D,(byte)0x0A,(byte)0x2D,(byte)0xB4,
                    0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82
            };
        }
    }
}
