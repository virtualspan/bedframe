package lol.sylvie.bedframe.util;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
    // https://stackoverflow.com/questions/57997257/how-can-i-zip-a-complete-directory-with-all-subfolders-in-java
    public static void zipFolder(Path source, File destination) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination));
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(source.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            zos.close();
        } catch (IOException e) {e.printStackTrace();}
    }
}
