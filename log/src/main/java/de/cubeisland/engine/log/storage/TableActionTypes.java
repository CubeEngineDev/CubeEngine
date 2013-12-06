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
package de.cubeisland.engine.log.storage;

import de.cubeisland.engine.core.storage.database.AutoIncrementTable;
import de.cubeisland.engine.core.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;

public class TableActionTypes extends AutoIncrementTable<ActionTypeModel, UInteger>
{
    public static TableActionTypes TABLE_ACTION_TYPE;

    public TableActionTypes(String prefix)
    {
        super(prefix + "log_actiontypes", new Version(1));
        this.setAIKey(ID);
        this.addUniqueKey(NAME);
        this.addFields(ID, NAME);
        TABLE_ACTION_TYPE = this;
    }

    public final TableField<ActionTypeModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<ActionTypeModel, String> NAME = createField("name", SQLDataType.VARCHAR.length(32).nullable(false), this);

    @Override
    public Class<ActionTypeModel> getRecordType() {
        return ActionTypeModel.class;
    }
}
