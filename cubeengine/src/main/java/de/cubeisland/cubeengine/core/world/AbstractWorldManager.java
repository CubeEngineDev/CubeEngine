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
package de.cubeisland.cubeengine.core.world;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.module.Module;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

public abstract class AbstractWorldManager implements WorldManager
{
    protected final WorldStorage storage;
    protected final Map<String, WorldModel> worlds;
    protected final TLongObjectHashMap<World> worldIds;
    private final Map<String, Map<String, ChunkGenerator>> generatorMap;

    public AbstractWorldManager(Core core)
    {
        this.storage = new WorldStorage(core.getDB());
        this.worlds = new THashMap<String, WorldModel>();
        this.worldIds = new TLongObjectHashMap<World>();
        this.generatorMap = new THashMap<String, Map<String, ChunkGenerator>>();
    }

    public synchronized long getWorldId(World world)
    {
        if (world == null)
        {
            throw new IllegalArgumentException("the world given is null!");
        }
        WorldModel model = this.worlds.get(world.getName());
        if (model == null)
        {
            model = this.storage.get(world);
            if (model == null)
            {
                model = new WorldModel(world);
                this.storage.store(model);
            }
            this.worlds.put(world.getName(), model);
            this.worldIds.put(model.key, world);
        }
        return model.key;
    }

    public synchronized Long getWorldId(String name)
    {
        WorldModel model = this.worlds.get(name);
        if (model == null)
        {
            World world = this.getWorld(name);
            if (world == null) return null;
            return this.getWorldId(world);
        }
        return model.key;
    }

    public synchronized long[] getAllWorldIds()
    {
        return this.worldIds.keys();
    }

    public synchronized World getWorld(long id)
    {
        return this.worldIds.get(id);
    }

    public synchronized void registerGenerator(Module module, String id, ChunkGenerator generator)
    {
        assert id != null : "The ID must nto be null!";
        assert generator != null : "The generator must not be null!";

        Map<String, ChunkGenerator> moduleGenerators = this.generatorMap.get(module.getId());
        if (moduleGenerators == null)
        {
            this.generatorMap.put(module.getId(), moduleGenerators = new THashMap<String, ChunkGenerator>(1));
        }
        moduleGenerators.put(id.toLowerCase(Locale.ENGLISH), generator);
    }

    public synchronized ChunkGenerator getGenerator(Module module, String id)
    {
        assert module != null : "The module must not be null!";
        assert id != null : "The ID must nto be null!";

        Map<String, ChunkGenerator> moduleGenerators = this.generatorMap.get(module.getId());
        if (moduleGenerators != null)
        {
            return moduleGenerators.get(id.toLowerCase(Locale.ENGLISH));
        }
        return null;
    }

    public synchronized void removeGenerator(Module module, String id)
    {
        assert module != null : "The module must not be null!";
        assert id != null : "The ID must nto be null!";

        Map<String, ChunkGenerator> moduleGenerators = this.generatorMap.get(module.getId());
        if (moduleGenerators != null)
        {
            moduleGenerators.remove(id.toLowerCase(Locale.ENGLISH));
        }
    }

    @Override
    public synchronized void removeGenerators(Module module)
    {
        this.generatorMap.remove(module.getId());
    }

    @Override
    public boolean unloadWorld(String worldName, boolean save)
    {
        return this.unloadWorld(this.getWorld(worldName), save);
    }

    @Override
    public boolean deleteWorld(String worldName) throws IOException
    {
        return this.deleteWorld(this.getWorld(worldName));
    }


    @Override
    public synchronized void clean()
    {
        this.worlds.clear();
        this.worldIds.clear();
        this.generatorMap.clear();
    }
}