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
package de.cubeisland.engine.basics.command.moderation.kit;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import de.cubeisland.engine.basics.Basics;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;
import gnu.trove.map.hash.THashMap;

import static de.cubeisland.engine.core.filesystem.FileExtensionFilter.YAML;

public class KitManager implements Listener
{
   private final Basics module;

    public KitManager(Basics module)
    {
        this.module = module;
        this.module.getCore().getEventManager().registerListener(module, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event)
    {
        if (!event.getPlayer().hasPlayedBefore())
        {
            for (Kit kit : kitMap.values())
            {
                if (kit.isGiveKitOnFirstJoin())
                {
                    User user = module.getCore().getUserManager().getExactUser(event.getPlayer().getName());
                    kit.give(null, user, true);
                }
            }
        }
    }

    private THashMap<String, Kit> kitMap = new THashMap<>();
    private THashMap<Kit, KitConfiguration> kitConfigMap = new THashMap<>();

    public Kit getKit(String name)
    {
        if (name == null) return null;
        Set <String> match = Match.string().getBestMatches(name.toLowerCase(Locale.ENGLISH), kitMap.keySet(), 2);
        if (match.isEmpty())
        {
            return null;
        }
        return kitMap.get(match.iterator().next());
    }

    public void saveKit(Kit kit)
    {
        KitConfiguration config = kitConfigMap.get(kit);
        if (config == null)
        {
            config = new KitConfiguration();
            kitConfigMap.put(kit, config);
            kitMap.put(kit.getKitName(), kit);
        }
        kit.applyToConfig(config);
        config.save(module.getFolder().resolve("kits").resolve(config.kitName + ".yml").toFile());
    }

    public void loadKit(Path file)
    {
        try
        {
            KitConfiguration config = this.module.getCore().getConfigurationFactory().load(KitConfiguration.class, file.toFile());
            config.kitName = StringUtils.stripFileExtension(file.getFileName().toString());
            Kit kit = config.getKit(module);
            kitConfigMap.put(kit, config);
            kitMap.put(config.kitName.toLowerCase(Locale.ENGLISH), kit);
            if (kit.getPermission() != null)
            {
                this.module.getCore().getPermissionManager().registerPermission(this.module,kit.getPermission());
            }
        }
        catch (Exception ex)
        {
            module.getLog().warn(ex, "Could not load the kit configuration!");
        }
    }

    public void loadKits()
    {
        Path folder = this.module.getFolder().resolve("kits");
        try
        {
            Files.createDirectories(folder);
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(folder, YAML))
            {
                for (Path file : directory)
                {
                    loadKit(file);
                }
            }
        }
        catch (IOException ex)
        {
            this.module.getLog().warn(ex, "Failed load the modules!");
        }
    }

    public Set<String> getKitsNames()
    {
        return this.kitMap.keySet();
    }
}
