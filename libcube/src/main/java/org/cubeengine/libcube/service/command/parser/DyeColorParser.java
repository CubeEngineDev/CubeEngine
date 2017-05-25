/*
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
package org.cubeengine.libcube.service.command.parser;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.libcube.service.matcher.MaterialDataMatcher;
import org.spongepowered.api.data.type.DyeColor;

public class DyeColorParser implements ArgumentParser<DyeColor>
{
    private MaterialDataMatcher matcher;

    public DyeColorParser(MaterialDataMatcher matcher)
    {

        this.matcher = matcher;
    }

    @Override
    public DyeColor parse(Class type, CommandInvocation invocation) throws ParserException
    {
        return matcher.colorData(invocation.consume(1));
    }
}
