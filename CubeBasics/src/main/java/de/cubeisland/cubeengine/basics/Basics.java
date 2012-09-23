package de.cubeisland.cubeengine.basics;

import de.cubeisland.cubeengine.basics.cheat.CheatCommands;
import de.cubeisland.cubeengine.core.module.Module;

public class Basics extends Module
{
    @Override
    public void onEnable()
    {
        this.getCore().getPermissionRegistration().registerPermissions(Perm.values());
        this.getCommandManager().registerCommands(this, new CheatCommands());
    }
}
