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
package de.cubeisland.engine.module.core;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Desc;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.service.ban.BanManager;
import de.cubeisland.engine.module.service.ban.IpBan;
import de.cubeisland.engine.module.service.ban.UserBan;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.annotation.CommandPermission;
import de.cubeisland.engine.module.service.command.annotation.Unloggable;
import de.cubeisland.engine.module.service.permission.PermDefault;
import de.cubeisland.engine.module.core.sponge.CoreModule;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserList;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.text.Text.Literal;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;

public class AuthCommands
{
    private final CoreModule core;
    private final BanManager banManager;
    private final UserManager um;

    private final ConcurrentHashMap<UUID, Long> fails = new ConcurrentHashMap<>();

    public AuthCommands(CoreModule core, BanManager bm, UserManager um)
    {
        this.core = core;
        this.banManager = bm;
        this.um = um;
    }

    @Unloggable
    @Command(alias = "setpw", desc = "Sets your password.")
    public void setPassword(CommandContext context, String password, @Default User player)
    {
        if ((context.getSource().equals(player)))
        {
            um.setPassword(player, password);
            context.sendTranslated(POSITIVE, "Your password has been set!");
            return;
        }
        context.ensurePermission(core.perms().COMMAND_SETPASSWORD_OTHER);
        um.setPassword(player, password);
        context.sendTranslated(POSITIVE, "{user}'s password has been set!", player);
    }

    @Command(alias = "clearpw", desc = "Clears your password.")
    public void clearPassword(CommandContext context,
                              @Optional @Desc("* or a list of Players delimited by ,") UserList players)
    {
        CommandSender sender = context.getSource();
        if (players == null)
        {
            if (!(sender instanceof User))
            {
                throw new TooFewArgumentsException();
            }
            this.um.resetPassword((User)sender);
            sender.sendTranslated(POSITIVE, "Your password has been reset!");
            return;
        }
        if (players.isAll())
        {
            context.ensurePermission(core.perms().COMMAND_CLEARPASSWORD_ALL);
            um.resetAllPasswords();
            sender.sendTranslated(POSITIVE, "All passwords reset!");
            return;
        }
        User target = context.get(0);
        if (!target.equals(context.getSource()))
        {
            context.ensurePermission(core.perms().COMMAND_CLEARPASSWORD_OTHER);
        }
        this.um.resetPassword(target);
        sender.sendTranslated(POSITIVE, "{user}'s password has been reset!", target.getName());
    }

    @Unloggable
    @Command(desc = "Logs you in with your password!")
    @CommandPermission(permDefault = PermDefault.TRUE)
    @Restricted(value = User.class, msg = "Only players can log in!")
    public void login(User context, String password)
    {
        if (context.isLoggedIn())
        {
            context.sendTranslated(POSITIVE, "You are already logged in!");
            return;
        }
        boolean isLoggedIn = um.login(context, password);
        if (isLoggedIn)
        {
            context.sendTranslated(POSITIVE, "You logged in successfully!");
            return;
        }
        context.sendTranslated(NEGATIVE, "Wrong password!");
        if (this.core.getConfiguration().security.fail2ban)
        {
            if (fails.get(context.getUniqueId()) != null)
            {
                if (fails.get(context.getUniqueId()) + TimeUnit.SECONDS.toMillis(10) > System.currentTimeMillis())
                {
                    Literal msg = Texts.of(context.getTranslation(NEGATIVE, "Too many wrong passwords!") + "\n"
                                    + context.getTranslation(NEUTRAL, "For your security you were banned 10 seconds."));
                    this.banManager.addBan(new UserBan(context.getOfflinePlayer(), context.getPlayer().get(), msg, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                        this.core.getConfiguration().security.banDuration))));

                    if (!core.getGame().getServer().getOnlineMode())
                    {
                        this.banManager.addBan(new IpBan(context.getAddress().getAddress(), context.getPlayer().get(),
                                                         msg, new Date(
                            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                                this.core.getConfiguration().security.banDuration))));
                    }
                    context.kick(msg);
                }
            }
            fails.put(context.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Command(desc = "Logs you out!")
    @Restricted(value = User.class, msg = "You might use /stop for this.")
    public void logout(User context)
    {
        if (context.isLoggedIn())
        {
            context.logout();
            context.sendTranslated(POSITIVE, "You're now logged out.");
            return;
        }
        context.sendTranslated(NEUTRAL, "You're not logged in!");
    }
}