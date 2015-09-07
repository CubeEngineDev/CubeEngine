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
package org.cubeengine.service.command.readers;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import org.cubeengine.service.i18n.I18n;


public class BooleanReader implements ArgumentReader<Boolean>
{
    private final Set<String> yesStrings;
    private final Set<String> noStrings;
    private final I18n i18n;

    public BooleanReader(I18n i18n)
    {
        this.i18n = i18n;
        this.yesStrings = new HashSet<>();
        this.yesStrings.add("yes");
        this.yesStrings.add("+");
        this.yesStrings.add("1");
        this.yesStrings.add("true");

        this.noStrings = new HashSet<>();
        this.noStrings.add("no");
        this.noStrings.add("-");
        this.noStrings.add("0");
        this.noStrings.add("false");
    }

    @Override
    public Boolean read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String arg = invocation.consume(1);
        Locale locale = invocation.getLocale();
        arg = arg.trim().toLowerCase(locale);
        if (this.yesStrings.contains(arg))
        {
            return true;
        }
        else if (this.noStrings.contains(arg))
        {
            return false;
        }
        else
        {
            String word = i18n.translate(locale, "yes");
            if (arg.equalsIgnoreCase(word))
            {
                return true;
            }
            word = i18n.translate(locale, "no");
            if (arg.equalsIgnoreCase(word))
            {
                return false;
            }
        }
        return Boolean.parseBoolean(arg);
    }
}