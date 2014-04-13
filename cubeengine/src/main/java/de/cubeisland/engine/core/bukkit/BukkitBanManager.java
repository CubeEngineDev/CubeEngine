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
package de.cubeisland.engine.core.bukkit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.server.v1_7_R3.DedicatedPlayerList;
import net.minecraft.server.v1_7_R3.GameProfileBanEntry;
import net.minecraft.server.v1_7_R3.GameProfileBanList;
import net.minecraft.server.v1_7_R3.IpBanEntry;
import net.minecraft.server.v1_7_R3.IpBanList;
import org.bukkit.craftbukkit.v1_7_R3.CraftServer;

import org.bukkit.Bukkit;

import de.cubeisland.engine.core.ban.Ban;
import de.cubeisland.engine.core.ban.BanManager;
import de.cubeisland.engine.core.ban.IpBan;
import de.cubeisland.engine.core.ban.UserBan;
import net.minecraft.util.com.mojang.authlib.GameProfile;

import static de.cubeisland.engine.core.CubeEngine.isMainThread;
import static de.cubeisland.engine.core.contract.Contract.expect;
import static de.cubeisland.engine.core.contract.Contract.expectNotNull;
import static org.bukkit.BanList.Type.IP;
import static org.bukkit.BanList.Type.UUID;

public class BukkitBanManager implements BanManager
{
    private final GameProfileBanList profileBan;
    private final IpBanList ipBans;

    public BukkitBanManager(BukkitCore core)
    {
        final DedicatedPlayerList playerList = ((CraftServer)core.getServer()).getHandle();
        this.profileBan = playerList.getProfileBans();
        this.ipBans = playerList.getIPBans();
    }

    @Override
    public void addBan(Ban ban)
    {
        expectNotNull(ban, "Ban must not be null!");
        expect(isMainThread());

        if (ban.getReason().contains("\n") || ban.getReason().contains("\r"))
        {
            throw new IllegalArgumentException("The ban reason my not contain line breaks (LF or CR)!");
        }

        if (ban instanceof UserBan)
        {
            Bukkit.getBanList(UUID).addBan(ban.getTarget().toString(), ban.getReason(), ban.getExpires(),
                                                ban.getSource());
        }
        else if (ban instanceof IpBan)
        {
            Bukkit.getBanList(IP).addBan(((IpBan)ban).getTarget().getHostAddress(), ban.getReason(), ban.getExpires(),
                                              ban.getSource());
        }
    }

    @Override
    public UserBan getUserBan(UUID uuid)
    {
        expect(isMainThread());
        GameProfileBanEntry entry = (GameProfileBanEntry)this.profileBan.get(new GameProfile(uuid, null));
        if (entry != null)
        {
            return new UserBan(uuid, entry.getSource(), entry.getReason(), entry.getCreated(), entry.getExpires());
        }
        return null;
    }

    @Override
    public IpBan getIpBan(InetAddress address)
    {
        expectNotNull(address, "The address must not be null!");
        expect(isMainThread());

        IpBanEntry entry = (IpBanEntry)this.ipBans.get(address.toString());
        if (entry != null)
        {
            return new IpBan(address, entry.getSource(), entry.getReason(), entry.getCreated(), entry.getExpires());
        }
        return null;
    }

    @Override
    public boolean removeUserBan(UUID uuid)
    {
        expect(isMainThread());
        if (!this.isUserBanned(uuid))
        {
            return false;
        }
        this.profileBan.remove(new GameProfile(uuid, null));
        return true;
    }

    @Override
    public boolean removeIpBan(InetAddress address)
    {
        expectNotNull(address, "The address must not be null!");
        expect(isMainThread());
        if (!this.isIpBanned(address))
        {
            return false;
        }
        this.ipBans.remove(address.getHostAddress());
        return true;
    }

    @Override
    public boolean isUserBanned(UUID uuid)
    {
        expect(isMainThread());

        return this.profileBan.isBanned(new GameProfile(uuid, null));
    }

    @Override
    public boolean isIpBanned(InetAddress address)
    {
        expect(isMainThread());

        return this.ipBans.isBanned(new InetSocketAddress(address, 0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<IpBan> getIpBans()
    {
        expect(isMainThread());

        String[] bannedIps = this.ipBans.getEntries();
        Set<IpBan> bans = new TreeSet<>(new BanComparator());

        for (String bannedIp : bannedIps)
        {
            try
            {
                IpBanEntry entry = (IpBanEntry)this.ipBans.get(bannedIp);
                bans.add(new IpBan(InetAddress.getByName(bannedIp), entry.getSource(), entry.getReason(), entry.getCreated(), entry.getExpires()));
            }
            catch (UnknownHostException e)
            {
                this.ipBans.remove(bannedIp);
            }
        }

        return bans;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<UserBan> getUserBans()
    {
        expect(isMainThread());

        String[] bannedUUIDs = this.profileBan.getEntries();
        Set<UserBan> bans = new TreeSet<>(new BanComparator());

        for (String bannedUUID : bannedUUIDs)
        {
            GameProfileBanEntry entry = (GameProfileBanEntry)this.profileBan.get(bannedUUID);
            bans.add(new UserBan(java.util.UUID.fromString(bannedUUID), entry.getSource(), entry.getReason(), entry.getCreated(), entry.getExpires()));
        }
        return bans;
    }

    private static final class BanComparator implements Comparator<Ban<?>>
    {
        @Override
        public int compare(Ban<?> o1, Ban<?> o2)
        {
            return o1.getCreated().compareTo(o2.getCreated());
        }
    }

    @Override
    public Set<Ban<?>> getBans()
    {
        expect(isMainThread());
        
        Set<Ban<?>> bans = new TreeSet<>(new BanComparator());
        bans.addAll(this.getIpBans());
        bans.addAll(this.getUserBans());
        return bans;
    }

    @Override
    public synchronized void reloadBans()
    {
        expect(isMainThread());
        this.profileBan.load();
        this.ipBans.load();
    }
}
