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
package de.cubeisland.engine.log.action.newaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.bigdata.ReflectedDBObject;
import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.world.ConfigWorld;
import de.cubeisland.engine.log.storage.ShowParameter;
import de.cubeisland.engine.reflect.Section;

/**
 * The Base for any Loggable Action
 * <p>The ListenerType will listen for given action
 */
public abstract class ActionTypeBase<ListenerType> extends ReflectedDBObject implements Comparable<ActionTypeBase>
{
    public Date date = new Date();
    public Coordinate coord;
    private transient List<ActionTypeBase> attached;

    protected int countAttached()
    {
        int count = 1;
        if (this.hasAttached())
        {
            count += this.getAttached().size();
        }
        return count;
    }

    public final void setLocation(Location loc)
    {
        this.coord = new Coordinate(loc);
    }

    public final List<ActionTypeBase> getAttached()
    {
        return attached;
    }

    public final boolean hasAttached()
    {
        return !(this.attached == null || this.attached.isEmpty());
    }

    public abstract boolean canAttach(ActionTypeBase action);

    public final void attach(ActionTypeBase action)
    {
        if (this.attached == null)
        {
            this.attached = new ArrayList<>();
        }
        this.attached.add(action);
    }

    public final void showAction(User user, ShowParameter show)
    {
        String msg = translateAction(user);
        user.sendMessage(msg);
        // TODO loc & time
    }

    /*

    private static final SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd");

    String time = "";
        if (show.showDate)
        {
            if (logEntry.hasAttached())
            {
                Timestamp first = logEntry.getTimestamp();
                Timestamp last = logEntry.getAttached().last().getTimestamp();
                if (first.getTime() > last.getTime())
                {
                    first = last;
                    last = logEntry.getTimestamp();
                }
                String fDate = dateOnly.format(first);
                if (dateOnly.format(last).equals(fDate)) // Same day
                {
                    time = ChatFormat.GREY + fDate + " " + timeOnly.format(first) + " " + ChatFormat.GOLD + "-" +
                        ChatFormat.WHITE + timeOnly.format(last) + " - ";
                }
                else
                {
                    time = ChatFormat.GREY + fDate + " " + timeOnly.format(first) + " " + ChatFormat.GOLD + "-" +
                        ChatFormat.GREY + dateOnly.format(last) + " " + timeOnly.format(last) + " - ";
                }
            }
            else
            {
                time = ChatFormat.GREY + dateOnly.format(logEntry.getTimestamp()) + " " +
                    timeOnly.format(logEntry.getTimestamp()) + " - ";
            }
        }
        String loc = "";
        if (show.showCoords)
        {
            loc =  "\n";
            if (logEntry.hasAttached())
            {
                int xMin = logEntry.getVector().x;
                int yMin = logEntry.getVector().y;
                int zMin = logEntry.getVector().z;
                int xMax = logEntry.getVector().x;
                int yMax = logEntry.getVector().y;
                int zMax = logEntry.getVector().z;
                for (LogEntry entry : logEntry.getAttached())
                {
                    if (entry.getVector().x < xMin)
                    {
                        xMin = entry.getVector().x;
                    }
                    else if (entry.getVector().x > xMax)
                    {
                        xMax = entry.getVector().x;
                    }
                    if (entry.getVector().y < yMin)
                    {
                        yMin = entry.getVector().y;
                    }
                    else if (entry.getVector().y > yMax)
                    {
                        yMax = entry.getVector().y;
                    }
                    if (entry.getVector().z < zMin)
                    {
                        zMin = entry.getVector().z;
                    }
                    else if (entry.getVector().z > zMax)
                    {
                        zMax = entry.getVector().z;
                    }
                }
                if (xMax == xMin && yMax == yMin && zMax == zMin)
                {
                    loc += user.getTranslation(MessageType.POSITIVE, "   at {vector} in {world}", new BlockVector3(xMax, yMax, zMax), logEntry
                        .getWorld());
                }
                else
                {
                    loc += user.getTranslation(MessageType.POSITIVE, "   in between {vector} nd {vector} in {world}", new BlockVector3(xMin, yMin, zMin), new BlockVector3(xMax, yMax, zMax), logEntry
                        .getWorld());
                }
            }
            else
            {
                loc += user.getTranslation(MessageType.POSITIVE, "   at {vector} in {world}", new BlockVector3(logEntry
                                                                                                                   .getVector().x, logEntry
                                                                                                                   .getVector().y, logEntry
                                                                                                                   .getVector().z), logEntry
                                               .getWorld());
            }
        }
        this.showLogEntry(user,logEntry,time,loc);
     */

    public abstract String translateAction(User user);

    public final boolean isNearTimeFrame(TimeUnit unit, int i, ActionTypeBase action)
    {
        return unit.toMillis(i) > Math.abs(this.date.getTime() - action.date.getTime());
    }

    public static class Coordinate implements Section
    {
        public ConfigWorld world;
        public UUID worldUUID;
        public Integer[] xz;
        public int y;

        public Coordinate()
        {
        }

        public Coordinate(Location loc)
        {
            this.world = new ConfigWorld(CubeEngine.getCore().getWorldManager(), loc.getWorld());
            this.worldUUID = loc.getWorld().getUID();
            this.xz = new Integer[2];
            this.xz[0] = loc.getBlockX();
            this.y = loc.getBlockY();
            this.xz[1] = loc.getBlockZ();
        }

        public World getWorld()
        {
            return world.getWorld();
        }

        public BlockVector3 toBlockVector()
        {
            return new BlockVector3(xz[0], y, xz[1]);
        }

        public boolean equals(Coordinate coordinate)
        {
            return coordinate.worldUUID.equals(this.worldUUID) && Arrays.equals(coordinate.xz, this.xz)
                && coordinate.y == this.y;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            Coordinate that = (Coordinate)o;

            if (y != that.y)
            {
                return false;
            }
            if (!worldUUID.equals(that.worldUUID))
            {
                return false;
            }
            if (!Arrays.equals(xz, that.xz))
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = worldUUID.hashCode();
            result = 31 * result + Arrays.hashCode(xz);
            result = 31 * result + y;
            return result;
        }
    }

    @Override
    public int compareTo(ActionTypeBase action)
    {
        return this.date.compareTo(action.date);
    }
}
