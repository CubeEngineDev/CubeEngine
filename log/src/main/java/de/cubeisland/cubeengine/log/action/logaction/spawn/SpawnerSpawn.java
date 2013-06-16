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
package de.cubeisland.cubeengine.log.action.logaction.spawn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.ActionTypeCategory;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.ENTITY;
import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.SPAWN;

/**
 * spawner spawning
 * <p>Events: {@link EntitySpawnActionType}</p>
 */
public class SpawnerSpawn extends SimpleLogActionType
{
    @Override
    protected Set<ActionTypeCategory> getCategories()
    {
        return new HashSet<ActionTypeCategory>(Arrays.asList(SPAWN, ENTITY));
    }

    @Override
    public boolean canRollback()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return "spawner-spawn";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&6%s &aspawned from a spawner%s",
                            time,logEntry.getCauserEntity(),loc);
    }
    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        if (!super.isSimilar(logEntry, other)) return false;
        return logEntry.causer == other.causer
            && logEntry.world == other.world
            && logEntry.location.equals(other.location);
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).SPAWNER_SPAWN_enable;
    }
}
