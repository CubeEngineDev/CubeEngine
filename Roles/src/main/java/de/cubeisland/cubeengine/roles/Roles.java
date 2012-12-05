package de.cubeisland.cubeengine.roles;

import de.cubeisland.cubeengine.core.module.Module;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.convert.Convert;
import de.cubeisland.cubeengine.roles.role.RoleManager;
import de.cubeisland.cubeengine.roles.role.RolesEventHandler;
import de.cubeisland.cubeengine.roles.role.config.PermissionTree;
import de.cubeisland.cubeengine.roles.role.config.PermissionTreeConverter;
import de.cubeisland.cubeengine.roles.role.config.Priority;
import de.cubeisland.cubeengine.roles.role.config.PriorityConverter;
import de.cubeisland.cubeengine.roles.role.config.RoleProvider;
import de.cubeisland.cubeengine.roles.role.config.RoleProviderConverter;
import de.cubeisland.cubeengine.roles.storage.AssignedRoleManager;

public class Roles extends Module
{
    private RolesConfig config;
    private RoleManager manager;
    private AssignedRoleManager dbManager;
    private static Roles instance;

    public Roles()
    {
        instance = this; // Needed in configuration loading
        Convert.registerConverter(PermissionTree.class, new PermissionTreeConverter());
        Convert.registerConverter(Priority.class, new PriorityConverter());
        Convert.registerConverter(RoleProvider.class, new RoleProviderConverter());
    }

    @Override
    public void onEnable()
    {
        this.dbManager = new AssignedRoleManager(this.getDatabase());
        this.manager = new RoleManager(this);
        RolesEventHandler rolesEventHandler = new RolesEventHandler(this);
        this.getEventManager().registerListener(this, rolesEventHandler);
        for (User user : this.getUserManager().getOnlineUsers()) // reapply roles on reload
        {
            user.removeAttribute(this, "roleContainer"); // remove potential old calculated roles
            rolesEventHandler.preCalculateRoles(user.getName());
            rolesEventHandler.applyRole(user.getPlayer(), this.getCore().getWorldManager().getWorldId(user.getWorld()));
        }
    }

    @Override
    public void onDisable()
    {
        for (User user : this.getUserManager().getLoadedUsers())
        {
            user.clearAttributes(this);
        }
    }

    public RolesConfig getConfiguration()
    {
        return this.config;
    }

    public AssignedRoleManager getDbManager()
    {
        return dbManager;
    }

    public RoleManager getManager()
    {
        return manager;
    }

    public static Roles getInstance()
    {
        return instance;
    }
}
