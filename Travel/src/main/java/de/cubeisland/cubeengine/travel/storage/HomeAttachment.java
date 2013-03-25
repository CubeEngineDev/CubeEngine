package de.cubeisland.cubeengine.travel.storage;

import de.cubeisland.cubeengine.core.attachment.Attachment;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.user.UserAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HomeAttachment extends UserAttachment
{
    Map<String, Home> homes;

    public HomeAttachment()
    {
        homes = new HashMap<String, Home>();
    }

    public Home getHome(String name)
    {
        if (name == null)
        {
            return null;
        }

        if (homes.containsKey(name))
        {
            return homes.get(name);
        }
        if (name.contains(":"))
        {
            return homes.get(name.substring(name.lastIndexOf(":") + 1, name.length()));
        }
        return null;
    }

    /**
     * Will check if getHome(name) is not null
     * @param name
     * @return
     */
    public boolean hasHome(String name)
    {
        return getHome(name) != null;
    }

    /**
     * Will find direct matches
     * @param name
     * @return
     */
    public boolean containsHome(String name)
    {
        return homes.containsKey(name);
    }

    public Map<String, Home> allHomes()
    {
        return this.homes;
    }

    public void addHome(String name, Home home)
    {
        homes.put(name, home);
    }

    public void removeHome(String name)
    {
        homes.remove(name);
    }
}