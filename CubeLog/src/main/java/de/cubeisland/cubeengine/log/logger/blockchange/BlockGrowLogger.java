package de.cubeisland.cubeengine.log.logger.blockchange;

import de.cubeisland.cubeengine.log.logger.SubLogConfig;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.StructureGrowEvent;

import static de.cubeisland.cubeengine.log.logger.blockchange.BlockLogger.BlockChangeCause.GROW;

public class BlockGrowLogger extends BlockLogger<BlockGrowLogger.BlockGrowConfig>
{
    public BlockGrowLogger()
    {
        this.config = new BlockGrowConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event)
    {
        Player player = null;
        if (event.isFromBonemeal())
        {
            player = event.getPlayer();
        }
        for (BlockState block : event.getBlocks())
        {
            if (player == null)
            {
                this.logBlockChange(GROW, null, event.getWorld().getBlockAt(block.getLocation()).getState(), block);
            }
            else
            {
                this.logBlockChange(GROW, player, event.getWorld().getBlockAt(block.getLocation()).getState(), block);
            }
        }
    }

    public static class BlockGrowConfig extends SubLogConfig
    {
        @Override
        public String getName()
        {
            return "block-grow";
        }
    }
}
