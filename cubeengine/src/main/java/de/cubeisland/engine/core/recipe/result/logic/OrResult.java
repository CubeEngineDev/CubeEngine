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
package de.cubeisland.engine.core.recipe.result.logic;

import java.util.LinkedList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import de.cubeisland.engine.core.recipe.condition.ingredient.MaterialProvider;

public class OrResult extends Result implements MaterialProvider
{
    private Result result1;
    private Result result2;

    OrResult(Result result1, Result result2)
    {
        this.result1 = result1;
        this.result2 = result2;
    }

    @Override
    public ItemStack getResult(Player player, ItemStack itemStack)
    {
        ItemStack result = result1.getResult(player, itemStack);
        if (result == null)
        {
            return result2.getResult(player, itemStack);
        }
        return result;
    }

    @Override
    public LinkedList<MaterialData> getMaterials(LinkedList<MaterialData> list)
    {
        if (result1 instanceof MaterialProvider)
        {
            list = ((MaterialProvider)result1).getMaterials(list);
        }
        if (result2 instanceof MaterialProvider)
        {
            list = ((MaterialProvider)result2).getMaterials(list);
        }
        return list;
    }
}
