package de.cubeisland.cubeengine.log.action.logaction.block.interaction;

import java.util.EnumSet;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;

/**
 * Eating cake
 * <p>Events: {@link RightClickActionType}</p>
 */
public class CakeEat extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(BLOCK, PLAYER);
    }

    @Override
    public String getName()
    {
        return "cake-eat";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        int piecesLeft = 6 - logEntry.getNewBlock().data;
        if (piecesLeft == 0)
        {
            user.sendTranslated("%s&aThe cake is a lie%s&a! Ask &2%s &ahe knows it!",
                                time,loc,logEntry.getCauserUser().getDisplayName());
        }
        else
        {
            user.sendTranslated("%s&2%s &aate a piece of cake%s&a!",
                                time,logEntry.getCauserUser().getDisplayName(),loc);
        }
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).CAKE_EAT_enable;
    }
}