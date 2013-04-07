package de.cubeisland.cubeengine.log.action.logaction.interact;

import java.util.EnumSet;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENTITY;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;

/**
 * Filling soup with mooshrooms
 * <p>Events: {@link InteractEntityActionType}</p>
 */
public class SoupFill extends SimpleLogActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(PLAYER, ENTITY);
    }

    @Override
    public boolean canRollback()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return "soup-fill";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&2%s &amade soup with a mooshroom!",
                            time, logEntry.getCauserUser().getDisplayName(),loc);
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.world == other.world
            && logEntry.causer == other.causer;
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).BOWL_FILL_SOUP;
    }
}