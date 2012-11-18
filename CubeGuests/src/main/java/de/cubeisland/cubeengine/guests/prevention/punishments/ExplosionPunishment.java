package de.cubeisland.cubeengine.guests.prevention.punishments;

import de.cubeisland.cubeengine.guests.prevention.Punishment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Creates an explosion on the player's position.
 */
public class ExplosionPunishment implements Punishment
{
    @Override
    public String getName()
    {
        return "explosion";
    }

    @Override
    public void punish(Player player, ConfigurationSection config)
    {
        player.getWorld().createExplosion(player.getLocation(), 0);
        player.damage(config.getInt("damage", 3));
    }
}
