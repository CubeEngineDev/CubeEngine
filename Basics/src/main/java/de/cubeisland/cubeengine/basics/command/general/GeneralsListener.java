package de.cubeisland.cubeengine.basics.command.general;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsPerm;
import de.cubeisland.cubeengine.basics.storage.BasicUser;
import de.cubeisland.cubeengine.core.bukkit.AfterJoinEvent;
import de.cubeisland.cubeengine.core.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class GeneralsListener implements Listener
{
    private Basics basics;

    public GeneralsListener(Basics basics)
    {
        this.basics = basics;
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            BasicUser bUser = this.basics.getBasicUserManager().getBasicUser((Player)event.getEntity());
            if (bUser.godMode)
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void blockplace(final BlockPlaceEvent event)
    {
        User user = basics.getUserManager().getExactUser(event.getPlayer());
        if (user.getAttribute(basics, "unlimitedItems") != null)
        {
            if ((Boolean)user.getAttribute(basics, "unlimitedItems"))
            {
                ItemStack itemInHand = event.getPlayer().getItemInHand();
                itemInHand.setAmount(itemInHand.getAmount() + 1);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event)
    {
        BasicUser bUser = this.basics.getBasicUserManager().getBasicUser(event.getPlayer());
        if (!BasicsPerm.COMMAND_GOD_KEEP.isAuthorized(event.getPlayer()))
        {
            bUser.godMode = false;
        }
        this.basics.getBasicUserManager().update(bUser); //update godmode
    }

    @EventHandler
    public void onAfterJoin(AfterJoinEvent event)
    {
        User user = basics.getUserManager().getExactUser(event.getPlayer());
        int amount = basics.getMailManager().countMail(user);
        if (amount > 0)
        {
            user.sendMessage("basics", "&aYou have &6%d &anew mails!\n&eUse &6/mail read &eto display them.", amount);
        }
        BasicUser bUser = this.basics.getBasicUserManager().getBasicUser(user);
        if (bUser.godMode == true)
        {
            user.setInvulnerable(true);
        }
    }
}