package de.cubeisland.cubeengine.core.module;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.logger.CubeFileHandler;
import de.cubeisland.cubeengine.core.logger.CubeLogger;
import de.cubeisland.cubeengine.core.logger.LogLevel;

import java.io.File;
import java.util.Locale;
import java.util.logging.LogRecord;

/**
 * This logger is used to log module messages.
 */
public class ModuleLogger extends CubeLogger
{
    private final String prefix;

    public ModuleLogger(Core core, ModuleInfo info)
    {
        super(info.getName(), core.getLog());
        this.prefix = "[" + info.getName() + "] ";
        try
        {
            this.addHandler(new CubeFileHandler(LogLevel.ALL, new File(core.getFileManager().getLogDir(), info.getName().toLowerCase(Locale.ENGLISH)).toString()));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void log(LogRecord record)
    {
        record.setMessage(this.prefix + record.getMessage());
        super.log(record);
    }
}
