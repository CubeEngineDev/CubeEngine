package de.cubeisland.cubeengine.log.logger.config;

import de.cubeisland.cubeengine.core.config.annotations.Option;
import de.cubeisland.cubeengine.log.LoggerConfig;
import org.bukkit.Material;

import java.util.Collection;
import java.util.LinkedList;

public  class BlockPlaceConfig extends LoggerConfig
{
    public BlockPlaceConfig()
    {
      super(true);
    }

    @Option("no-logging")
    public Collection<Material> noLogging = new LinkedList<Material>();

    @Override
    public String getName()
    {
        return "block-place";
    }
}