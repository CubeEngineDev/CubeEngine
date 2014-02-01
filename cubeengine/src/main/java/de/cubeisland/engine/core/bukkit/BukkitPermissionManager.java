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
package de.cubeisland.engine.core.bukkit;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import de.cubeisland.engine.core.Core;
import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.logging.LoggingUtil;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.permission.PermDefault;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.permission.PermissionManager;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.logging.Log;
import de.cubeisland.engine.logging.target.file.AsyncFileTarget;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import static de.cubeisland.engine.core.contract.Contract.expectNotNull;
import static de.cubeisland.engine.core.contract.Contract.expect;
import static de.cubeisland.engine.core.permission.Permission.BASE;

public class BukkitPermissionManager implements PermissionManager
{
    private final PluginManager pm;
    private final Map<String, org.bukkit.permissions.Permission> wildcards;
    private final Map<Module, Set<String>> modulePermissionMap;
    private final Log logger;

    private boolean startup;
    private Map<String, org.bukkit.permissions.Permission> permissions;
    private Set<org.bukkit.permissions.Permission> defaultPermTrue;
    private Set<org.bukkit.permissions.Permission> defaultPermFalse;

    @SuppressWarnings("unchecked")
    public BukkitPermissionManager(BukkitCore core)
    {
        this.startup = true;
        this.pm = core.getServer().getPluginManager();
        try
        {
            Field field = SimplePluginManager.class.getDeclaredField("permissions");
            field.setAccessible(true);
            this.permissions = (Map<String, org.bukkit.permissions.Permission>)field.get(this.pm);
            field = SimplePluginManager.class.getDeclaredField("defaultPerms");
            field.setAccessible(true);
            Map<Boolean,Set<org.bukkit.permissions.Permission>> defaultPerms =
                (Map<Boolean, Set<org.bukkit.permissions.Permission>>)field.get(this.pm);
            this.defaultPermTrue = defaultPerms.get(true);
            this.defaultPermFalse =  defaultPerms.get(false);
        }
        catch (Exception ex)
        {
            core.getLog().info("Couldn't access the permission manager internals for fast permission registration, falling back to normal registration.");
            this.startup = false;
        }
        this.wildcards = new THashMap<>(0);
        this.modulePermissionMap = new THashMap<>(0);
        this.logger = core.getLogFactory().getLog(Core.class, "Permissions");
        this.logger.addTarget(new AsyncFileTarget(LoggingUtil.getLogFile(core, "Permissions"),
                                                  LoggingUtil.getFileFormat(false, false),
                                                  false, LoggingUtil.getCycler(),
                                                  core.getTaskManager().getThreadFactory()));
        this.registerPermission(core.getModuleManager().getCoreModule(), Permission.BASE);
    }

    private void registerBukkitPermission(org.bukkit.permissions.Permission permission)
    {
        try
        {
            if (this.startup)
            {
                this.permissions.put(permission.getName().toLowerCase(), permission);
                if ((permission.getDefault() == PermissionDefault.OP) || (permission.getDefault() == PermissionDefault.TRUE))
                {
                    this.defaultPermTrue.add(permission);
                }
                if ((permission.getDefault() == PermissionDefault.NOT_OP) || (permission.getDefault() == PermissionDefault.TRUE))
                {
                    this.defaultPermFalse.add(permission);
                }
            }
            else
            {
                this.pm.addPermission(permission);
            }
            if (permission.getName().endsWith("*"))
            {
                this.wildcards.put(permission.getName(), permission);
            }
            this.logger.debug("successful {} ({})", permission.getName(), permission.getDefault().name());
        }
        catch (IllegalArgumentException ignored)
        {
            this.logger.debug("duplicated {} ({})", permission.getName(), permission.getDefault().name());
        }
    }

    private org.bukkit.permissions.Permission registerWildcard(Module module, String perm, PermDefault def)
    {
        perm += ".*";

        org.bukkit.permissions.Permission wildcard = this.wildcards.get(perm);
        if (wildcard == null)
        {
            this.registerBukkitPermission(wildcard = new org.bukkit.permissions.Permission(perm, def.getValue()));
            this.getPermissions(module).add(perm);
        }

        return wildcard;
    }

    private Set<String> getPermissions(Module module)
    {
        Set<String> perms = this.modulePermissionMap.get(module);
        if (perms == null)
        {
            this.modulePermissionMap.put(module, perms = new THashSet<>(1));
        }
        return perms;
    }

    private org.bukkit.permissions.Permission registerPermission(Module module, String perm, PermDefault permDefault)
    {
        expect(CubeEngine.isMainThread(), "Permissions may only be registered from the main thread!");
        expectNotNull(module, "The module must not be null!");
        expectNotNull(perm, "The permission must not be null!");
        expectNotNull(permDefault, "The permission default must not be null!");

        perm = perm.toLowerCase(Locale.ENGLISH);
        String[] parts = StringUtils.explode(".", perm);
        if (parts.length < 3 || !BASE.getName().equals(parts[0]) || !module.getId().equals(parts[1]))
        {
            throw new IllegalArgumentException("Permissions must start with 'cubeengine.<module>' !");
        }

        this.getPermissions(module).add(perm);

        org.bukkit.permissions.Permission permission = this.pm.getPermission(perm);
        if (permission == null)
        {
            permission = new org.bukkit.permissions.Permission(perm, permDefault.getValue());
            this.registerBukkitPermission(permission);
        }
        return permission;
    }

    @Override
    public void registerPermission(Module module, Permission permission)
    {
        org.bukkit.permissions.Permission mainBPerm;
        org.bukkit.permissions.Permission mainBWCPerm = null;
        if (permission.isWildcard())
        {
            mainBPerm = this.registerWildcard(module, permission.getName(), permission.getDefault());
            mainBWCPerm = mainBPerm;
        }
        else
        {
            mainBPerm = this.registerPermission(module, permission.getName(), permission.getDefault());
            if (permission.hasChildren()) // create wildcard perm-name.* (will contain perm-name)
            {
                mainBWCPerm = this.registerWildcard(module, permission.getName(), permission.getDefault());
                mainBPerm.addParent(mainBWCPerm, true);
            }
        }
        // search/register direct parents and add parent to bukkitperm
        for (Permission parentPerm : permission.getParents())
        {
            org.bukkit.permissions.Permission bPerm;
            if (parentPerm.isWildcard())
            {
                bPerm = this.registerWildcard(module, parentPerm.getName(), parentPerm.getDefault());
            }
            else
            {
                bPerm = this.registerPermission(module, parentPerm.getName(), parentPerm.getDefault());
            }
            if (mainBWCPerm == null)
            {
                mainBPerm.addParent(bPerm, true);
            }
            else
            {
                mainBWCPerm.addParent(bPerm, true);
            }
            if (!module.getBasePermission().equals(parentPerm))
            {
                this.registerPermission(module, parentPerm);
            }
        }
        for (Permission attached : permission.getAttached()) // make sure attached permissions are attached
        {
            org.bukkit.permissions.Permission bukkitPerm = pm.getPermission(attached.getName() + (attached.isWildcard() ? ":*" : ""));
            if (bukkitPerm != null)
            {
                bukkitPerm.addParent(mainBPerm, true);
            }
            // else Permission not registered yet -> will register itself
        }
    }

    @Override
    public void registerPermissions(Module module, Permission[] permissions)
    {
        for (Permission permission : permissions)
        {
            this.registerPermission(module, permission);
        }
    }

    public void removePermission(Module module, String perm)
    {
        expectNotNull(module, "The module must not be null!");
        expectNotNull(perm, "The permission must not be null!");
        expect(!perm.equals(Permission.BASE.getName() + ".*"), "The CubeEngine wildcard permission must not be unregistered!");

        Set<String> perms = this.modulePermissionMap.get(module);
        if (perms != null && perms.remove(perm))
        {
            this.pm.removePermission(perm);
            if (perm.endsWith("*"))
            {
                this.wildcards.remove(perm);
            }
        }
    }

    @Override
    public void removePermission(Module module, Permission permission)
    {
        this.removePermission(module, permission.getName());
    }

    public void removePermissions(Module module)
    {
        expectNotNull(module, "The module must not be null!");

        Set<String> removedPerms = this.modulePermissionMap.remove(module);
        if (removedPerms != null)
        {
            for (String perm : removedPerms)
            {
                this.pm.removePermission(perm);
                if (perm.endsWith("*"))
                {
                    this.wildcards.remove(perm);
                }
            }
        }
    }

    public void removePermissions()
    {
        Iterator<Entry<Module, Set<String>>> modulesIter = this.modulePermissionMap.entrySet().iterator();
        Entry<Module, Set<String>> entry;

        while (modulesIter.hasNext())
        {
            entry = modulesIter.next();
            modulesIter.remove();
            for (String perm : entry.getValue())
            {
                this.pm.removePermission(perm);
            }
        }
    }

    public PermDefault getDefaultFor(String permission)
    {
        if (permission == null)
        {
            throw new NullPointerException("The permission must not be null!");
        }
        org.bukkit.permissions.Permission perm = this.pm.getPermission(permission);
        if (perm == null)
        {
            return null;
        }
        switch (perm.getDefault())
        {
            case TRUE:
                return PermDefault.TRUE;
            case FALSE:
                return PermDefault.FALSE;
            case OP:
                return PermDefault.OP;
            case NOT_OP:
                return PermDefault.NOT_OP;
            default:
                return null;
        }
    }

    public void clean()
    {
        this.removePermissions();
        this.wildcards.clear();
        this.modulePermissionMap.clear();
    }

    void calculatePermissions()
    {
        for (Permissible permissible : this.pm.getDefaultPermSubscriptions(true))
        {
            permissible.recalculatePermissions();
        }
        for (Permissible permissible : this.pm.getDefaultPermSubscriptions(false))
        {
            permissible.recalculatePermissions();
        }
        this.startup = false;
    }
}
