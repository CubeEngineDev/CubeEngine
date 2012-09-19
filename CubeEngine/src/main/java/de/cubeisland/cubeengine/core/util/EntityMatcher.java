package de.cubeisland.cubeengine.core.util;

import de.cubeisland.cubeengine.core.CoreResource;
import de.cubeisland.cubeengine.core.CubeEngine;
import gnu.trove.map.hash.TShortObjectHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Anselm Brehme
 */
public class EntityMatcher
{
    private static EntityMatcher instance = null;

    public EntityMatcher()
    {
        TShortObjectHashMap<List<String>> entityList = this.readEntities();
        for (short id : entityList.keys())
        {
            try
            {
                EntityType.fromId(id).registerName(entityList.get(id));
            }
            catch (NullPointerException e)
            {
                CubeEngine.getLogger().log(Level.WARNING, "Unknown Entity ID: " + id + " " + entityList.get(id).get(0));
            }
        }
    }

    public static EntityMatcher get()
    {
        if (instance == null)
        {
            instance = new EntityMatcher();
        }
        return instance;
    }

    public EntityType matchEntity(String name)
    {
        Map<String, EntityType> entities = EntityType.getNameSets();
        String s = name.toLowerCase(Locale.ENGLISH);
        EntityType entity = entities.get(s);
        try
        {
            short entityId = Short.parseShort(s);
            return EntityType.fromId(entityId);
        }
        catch (NumberFormatException e)
        {
        }
        if (entity == null)
        {
            if (s.length() < 4)
            {
                return null;
            }
            String t_key = StringUtils.matchString(name, entities.keySet());
            if (t_key != null)
            {
                return entities.get(t_key);
            }
        }
        return entity;
    }

    public EntityType matchMob(String s)
    {
        EntityType type = this.matchEntity(s);
        if (type.isAlive())
        {
            return type;
        }
        return null;
    }

    public EntityType matchSpawnEggMobs(String s)
    {
        EntityType type = this.matchMob(s);
        if (type.canBeSpawnedBySpawnEgg())
        {
            return type;
        }
        return null;
    }

    public EntityType matchMonster(String s)
    {
        EntityType type = this.matchEntity(s);
        if (type.isMonster())
        {
            return type;
        }
        return null;
    }

    public EntityType matchFriendlyMob(String s)
    {
        EntityType type = this.matchEntity(s);
        if (type.isFriendly())
        {
            return type;
        }
        return null;
    }

    public EntityType matchProjectile(String s)
    {
        EntityType type = this.matchEntity(s);
        if (type.isProjectile())
        {
            return type;
        }
        return null;
    }

    private TShortObjectHashMap<List<String>> readEntities()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(new File(CubeEngine.getFileManager().getDataFolder(), CoreResource.ENTITIES.getTarget())));
            TShortObjectHashMap<List<String>> entityList = new TShortObjectHashMap<List<String>>();
            String line;
            ArrayList<String> names = new ArrayList<String>();
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }
                if (line.endsWith(":"))
                {
                    short id = Short.parseShort(line.substring(0, line.length() - 1));
                    names = new ArrayList<String>();
                    entityList.put(id, names);
                }
                else
                {
                    names.add(line);
                }
            }
            return entityList;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalStateException("enchantments.txt is corrupted!", ex);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Error while reading enchantments.txt", ex);
        }
    }
}