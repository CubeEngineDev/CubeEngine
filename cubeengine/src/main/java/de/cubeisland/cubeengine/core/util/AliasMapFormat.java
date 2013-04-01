package de.cubeisland.cubeengine.core.util;

import de.cubeisland.cubeengine.core.filesystem.FileUtil;
import org.apache.commons.lang.Validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * This class provides methods to convert the AliasMaps used for items.txt etc. into a TreeMap and back.
 */
public class AliasMapFormat
{
    public static boolean parseStringList(File file, TreeMap<String, List<String>> map, boolean update) throws IOException
    {
        return parseStringList(FileUtil.readStringList(file), map, update);
    }

    public static boolean parseStringList(InputStream stream, TreeMap<String, List<String>> map, boolean update) throws IOException
    {
        return parseStringList(FileUtil.readStringList(stream), map, update);
    }

    public static boolean parseStringList(List<String> input, TreeMap<String, List<String>> map, boolean update) throws IOException
    {
        Validate.notNull(input, "Invalid input! File or Reader was null!");
        Validate.notNull(map, "Map to parse into was null!");
        boolean updated = false;
        ArrayList<String> names = new ArrayList<String>();
        for (String line : input)
        {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
            {
                continue;
            }
            if (line.endsWith(":"))
            {
                String key = line.substring(0, line.length() - 1);
                names = new ArrayList<String>();
                if (!update)
                {
                    map.put(key, names);
                }
                else
                {
                    if (map.get(key) == null || map.get(key).isEmpty())
                    {
                        map.put(key, names);
                        updated = true;
                    }
                }
            }
            else
            {
                names.add(line);
            }
        }
        return updated;
    }

    public static void parseAndSaveStringListMap(TreeMap<String, List<String>> map, File file) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet())
        {
            sb.append(key).append(":").append("\n");
            List<String> entitynames = map.get(key);
            for (String entityname : entitynames)
            {
                sb.append("    ").append(entityname).append("\n");
            }
        }
        FileUtil.saveFile(sb.toString(), file);
    }
}