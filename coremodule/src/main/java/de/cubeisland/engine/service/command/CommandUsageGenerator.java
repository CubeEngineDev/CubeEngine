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
package de.cubeisland.engine.service.command;

import de.cubeisland.engine.butler.CommandDescriptor;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.CommandSource;
import de.cubeisland.engine.butler.parameter.FlagParameter;
import de.cubeisland.engine.butler.parameter.Parameter;
import de.cubeisland.engine.butler.parameter.ParameterUsageGenerator;
import de.cubeisland.engine.service.command.exception.PermissionDeniedException;
import de.cubeisland.engine.service.command.property.PermissionProvider;
import de.cubeisland.engine.service.command.property.RawPermission;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.butler.parameter.property.Requirement.isRequired;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.NONE;

public class CommandUsageGenerator extends ParameterUsageGenerator
{
    @Override
    public String generateParameterUsage(CommandInvocation invocation, CommandDescriptor parameters)
    {
        return super.generateParameterUsage(invocation, parameters);
    }

    @Override
    protected String generateFlagUsage(CommandInvocation invocation, FlagParameter parameter)
    {
        if (invocation != null)
        {
            checkPermission(invocation.getCommandSource(), parameter);
        }
        return super.generateFlagUsage(invocation, parameter);
    }

    private void checkPermission(CommandSource source, Parameter parameter)
    {
        if (parameter.hasProperty(PermissionProvider.class) && source instanceof Subject)
        {
            RawPermission rawPerm = parameter.valueFor(PermissionProvider.class);
            if (!((Subject)source).hasPermission(rawPerm.getName()))
            {
                throw new PermissionDeniedException(rawPerm);
            }
        }
    }

    @Override
    protected String generateParameterUsage(CommandInvocation invocation, Parameter parameter)
    {
        if (invocation != null && !isRequired(parameter))
        {
            checkPermission(invocation.getCommandSource(), parameter);
        }
        return super.generateParameterUsage(invocation, parameter);
    }

    @Override
    protected String valueLabel(CommandInvocation invocation, String valueLabel)
    {
        if (invocation != null && invocation.getCommandSource() instanceof CommandSender)
        {
            return Texts.toPlain(((CommandSender)invocation.getCommandSource()).getTranslation(NONE, valueLabel));
        }
        return valueLabel;
    }

    @Override
    protected String getPrefix(CommandInvocation invocation)
    {
        if (invocation != null && invocation.getCommandSource() instanceof User)
        {
            return "/";
        }
        return "";
    }
}