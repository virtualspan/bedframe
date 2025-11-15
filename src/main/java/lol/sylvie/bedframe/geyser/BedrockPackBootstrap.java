package lol.sylvie.bedframe.geyser;

import lol.sylvie.bedframe.util.BedframeConstants;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public final class BedrockPackBootstrap {
    private BedrockPackBootstrap() {}

    public static void run(Path packPath, File outputZip, List<Translator> translators) {
        try {
            new PackGenerator().generatePack(packPath, outputZip, translators);
            BedframeConstants.LOGGER.info("Bedrock resource pack generated at {}", outputZip.getAbsolutePath());
        } catch (Exception e) {
            BedframeConstants.LOGGER.error("Failed to generate Bedrock resource pack", e);
        }
    }
}
