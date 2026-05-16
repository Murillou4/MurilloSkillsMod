package com.murilloskills.core.platform;

import java.nio.file.Path;

public interface ConfigPathPort {
    Path getConfigDirectory();

    Path getWorldDirectory();
}
