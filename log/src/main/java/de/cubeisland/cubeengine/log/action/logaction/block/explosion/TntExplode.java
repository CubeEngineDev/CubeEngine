package de.cubeisland.cubeengine.log.action.logaction.block.explosion;

import java.util.EnumSet;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.*;

/**
 * TNT-Explosions
 * <p>Events: {@link ExplodeActionType}</p>
 */
public class TntExplode extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(BLOCK, PLAYER);
    }


    @Override
    public String getName()
    {
        return "tnt-explode";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.hasAttached())
        {
            int amount = logEntry.getAttached().size()+1;
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aA TNT-Explosion induced by &2%s&a got rid of &6%dx %s&a%s!",
                                    time,logEntry.getCauserUser().getDisplayName(),amount,
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aA TNT-Explosion got rid of &6%dx %s&a%s!",
                                    time,amount,logEntry.getOldBlock(),loc);
            }
        }
        else
        {
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aA TNT-Explosion induced by &2%s&a got rid of &6%s&a%s!",
                                    time,logEntry.getCauserUser().getDisplayName(),
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aA TNT-Explosion got rid of &6%s&a%s!",
                                    time,logEntry.getOldBlock(),loc);
            }
        }
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).TNT_EXPLODE_enable;
    }
}