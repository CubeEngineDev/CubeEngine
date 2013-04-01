package de.cubeisland.cubeengine.shout.interactions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import de.cubeisland.cubeengine.core.bukkit.PlayerLanguageReceivedEvent;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.shout.Shout;
import de.cubeisland.cubeengine.shout.announce.AnnouncementManager;

public class ShoutListener implements Listener
{
    private Shout module;
    private AnnouncementManager am;

    public ShoutListener(Shout module)
    {
        this.module = module;
        this.am = module.getAnnouncementManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLanguageReceived(PlayerLanguageReceivedEvent event)
    {
        User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer());

        if (this.module.getCore().isDebug())
        {
            this.module.getLog().log(LogLevel.DEBUG, "Loading user: {0}", user.getName());
        }
        this.am.initializeUser(user);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event)
    {
        this.am.clean(event.getPlayer().getName());
    }
}