package de.cubeisland.cubeengine.roles.provider;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.Pair;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.util.StringUtils;
import de.cubeisland.cubeengine.roles.Roles;
import de.cubeisland.cubeengine.roles.RolesConfig;
import de.cubeisland.cubeengine.roles.role.Role;
import de.cubeisland.cubeengine.roles.config.RoleMirror;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static de.cubeisland.cubeengine.core.logger.LogLevel.DEBUG;

public class WorldRoleProvider extends RoleProvider
{
    private RoleMirror mirrorConfig;
    private Set<Role> defaultRoles = new HashSet<Role>();

    public WorldRoleProvider(Roles module, RoleMirror mirrorConfig)
    {
        super(module, false);
        this.mirrorConfig = mirrorConfig;
        this.createBasePerm();
    }

    public WorldRoleProvider(Roles module, long worldId)
    {
        super(module, false);
        this.mirrorConfig = new RoleMirror(this.module, worldId);
        this.createBasePerm();
    }

    @Override
    public void createBasePerm()
    {
        this.basePerm = this.module.getBasePermission().createAbstractChild("world").createAbstractChild(this.mirrorConfig.mainWorld);
    }

    public TLongObjectHashMap<Pair<Boolean, Boolean>> getWorlds()
    {
        return this.mirrorConfig.getWorlds();
    }



    public Set<Role> getDefaultRoles()
    {
        return this.defaultRoles;
    }

    public void loadDefaultRoles(RolesConfig config)
    {
        List<String> dRoles = config.defaultRoles.get(this.mirrorConfig.mainWorld);
        if (dRoles == null || dRoles.isEmpty())
        {
            module.getLog().log(LogLevel.WARNING, "No default-roles defined for " + this.mirrorConfig.mainWorld);
            return;
        }
        for (String roleName : dRoles)
        {
            Role role = this.roles.get(roleName.toLowerCase(Locale.ENGLISH));
            if (role == null)
            {
                module.getLog().log(LogLevel.WARNING, "Could not find default-role " + roleName);
            }
            this.defaultRoles.add(role);
        }
    }

    @Override
    public void loadInConfigurations(File rolesFolder)
    {
        if (this.init) // provider is already initialized!
        {
            return;
        }
        if (this.folder == null)
        {
            // Sets the folder for this provider
            this.folder = new File(rolesFolder, this.mirrorConfig.mainWorld);
        }
        this.module.getLog().log(DEBUG, "Loading roles for provider of " + this.mirrorConfig.mainWorld + ":");
        super.loadInConfigurations(rolesFolder);
    }

    public String getMainWorld()
    {
        return this.mirrorConfig.mainWorld;
    }

    public boolean toggleDefaultRole(Role role)
    {
        if (this.defaultRoles.contains(role))
        {
            List<String> defaultConfigRoles = this.module.getConfiguration().defaultRoles.get(this.mirrorConfig.mainWorld);
            defaultConfigRoles.remove(role.getName());
            this.defaultRoles.remove(role);
            this.module.getConfiguration().save();
            return false;
        }
        else
        {
            List<String> defaultConfigRoles = this.module.getConfiguration().defaultRoles.get(this.mirrorConfig.mainWorld);
            defaultConfigRoles.add(role.getName());
            this.defaultRoles.add(role);
            this.module.getConfiguration().save();
            return true;
        }
    }
}
