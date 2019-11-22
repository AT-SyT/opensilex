//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs;

import java.io.*;
import java.nio.file.*;
import java.util.function.*;
import org.opensilex.service.*;
import org.slf4j.*;


public class FileStorageService implements Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    public void listFilesByExtension(String directory, String extensionFilter, Consumer<File> action) throws IOException {
        Path directoryPath = Paths.get(directory);

        LOGGER.debug("Load files by extension: " + directory + " " + extensionFilter);

        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            Files.walk(directoryPath)
                    .filter(Files::isRegularFile)
                    .map(p -> p.toFile())
                    .filter(f -> f.getAbsolutePath().endsWith("." + extensionFilter))
                    .forEach(action);
        }
    }
}
