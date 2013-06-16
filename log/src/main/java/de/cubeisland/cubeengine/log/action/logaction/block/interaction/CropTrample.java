/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.cubeengine.log.action.logaction.block.interaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.ActionTypeCategory;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.PLAYER;


/**
 * Trampling Crops
 * <p>Events: {@link RightClickActionType}</p>
 */
public class CropTrample extends BlockActionType

{
    @Override
    protected Set<ActionTypeCategory> getCategories()
    {
        return new HashSet<ActionTypeCategory>(Arrays.asList(BLOCK, PLAYER));
    }

    @Override
    public String getName()
    {
        return "crop-trample";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.hasAttached())
        {
            if (logEntry.getOldBlock().material.equals(Material.SOIL))
            {
                logEntry = logEntry.getAttached().first(); // replacing SOIL log with the crop log as the destroyed SOIL is implied
            }
        }
        user.sendTranslated("%s&2%s &atrampeled down &6%s%s",
                            time, logEntry.getCauserUser().getDisplayName(),
                            logEntry.getOldBlock(), loc);
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).CROP_TRAMPLE_enable;
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        if (logEntry.actionType == other.actionType
            && logEntry.world == other.world
            && logEntry.causer == other.causer
            && logEntry.additional == other.additional
            && nearTimeFrame(logEntry, other))
        {
            Location loc1 = logEntry.getLocation();
            Location loc2 = other.getLocation();
            if (loc1.getX() == loc2.getX() && loc1.getZ() == loc2.getZ()
                && Math.abs(loc1.getBlockY() - loc2.getBlockY()) == 1)
            {
                return true;
            }
         }
        return false;

    }

    @Override
    protected boolean nearTimeFrame(LogEntry logEntry, LogEntry other)
    {
        return Math.abs(logEntry.timestamp.getTime() - other.timestamp.getTime()) < 50;
    }
}
