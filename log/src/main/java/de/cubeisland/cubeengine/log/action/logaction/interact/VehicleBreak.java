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
package de.cubeisland.cubeengine.log.action.logaction.interact;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENTITY;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.VEHICLE;

/**
 * Breaking vehicles
 * <p>Events: {@link VehicleDestroyEvent}
 */
public class VehicleBreak extends SimpleLogActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(VEHICLE, PLAYER, ENTITY);
    }

    @Override
    public boolean canRollback()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return "vehicle-break";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleDestroy(final VehicleDestroyEvent event)
    {
        if (this.isActive(event.getVehicle().getWorld()))
        {
            Entity causer = null;
            if (event.getAttacker() != null)
            {
                if (event.getAttacker() instanceof Player)
                {
                    causer = event.getAttacker();
                }
                else if (event.getAttacker() instanceof Projectile)
                {
                    Projectile projectile = (Projectile) event.getAttacker();
                    if (projectile.getShooter() instanceof Player)
                    {
                        causer = projectile.getShooter();
                    }
                    else if (projectile.getShooter() != null)
                    {
                        causer = projectile.getShooter();
                    }
                }
            }
            else if (event.getVehicle().getPassenger() instanceof Player)
            {
                causer = event.getVehicle().getPassenger();
            }
            this.logSimple(event.getVehicle().getLocation(),causer,event.getVehicle(),null);
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&2%s&a broke a &6%s%s&a!",
                            time, logEntry.getCauserUser() == null ?
                            logEntry.getCauserEntity() :
                            logEntry.getCauserUser().getDisplayName(),
                            logEntry.getEntityFromData(),loc);
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.world == other.world
            && logEntry.causer == other.causer
            && logEntry.data == other.data;
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).VEHICLE_BREAK_enable;
    }
}
