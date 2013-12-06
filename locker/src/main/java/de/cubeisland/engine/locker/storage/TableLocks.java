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
package de.cubeisland.engine.locker.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import de.cubeisland.engine.core.storage.database.AutoIncrementTable;
import de.cubeisland.engine.core.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;

public class TableLocks extends AutoIncrementTable<LockModel, UInteger>
{
    public static TableLocks TABLE_LOCK;

    public TableLocks(String prefix)
    {
        super(prefix + "locks", new Version(1));
        this.setAIKey(ID);
        this.addUniqueKey(ENTITY_UID_LEAST, ENTITY_UID_MOST);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), OWNER_ID);
        TABLE_LOCK = this;
    }

    @Override
    public void createTable(Connection connection) throws SQLException
    {
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.getName()+ " (\n" +
                                        "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,\n" +
                                        "`owner_id` int(10) unsigned NOT NULL,\n" +
                                        "`flags` smallint NOT NULL,\n" +
                                        "`type` tinyint NOT NULL,\n" +
                                        "`lock_type` tinyint NOT NULL,\n" +
                                        "`password` varbinary(128) NOT NULL,\n" +
                                        "`droptransfer` tinyint(1) NOT NULL,\n" +
                                        "`entity_uid_least` bigint DEFAULT NULL,\n" +
                                        "`entity_uid_most` bigint DEFAULT NULL,\n" +
                                        "`last_access` DATETIME NOT NULL,\n" +
                                        "`created` DATETIME NOT NULL,\n" +
                                        "PRIMARY KEY (`id`),\n" +
                                        "UNIQUE KEY (`entity_uid_least`, `entity_uid_most`),\n" +
                                        "FOREIGN KEY f_owner (`owner_id`) REFERENCES " + TABLE_USER.getName() + " (`key`) ON UPDATE CASCADE ON DELETE CASCADE)\n" +
                                        "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci\n" +
                                        "COMMENT='1.0.0'").execute();
    }

    public final TableField<LockModel, UInteger> ID = createField("id", SQLDataType.INTEGERUNSIGNED, this);
    public final TableField<LockModel, UInteger> OWNER_ID = createField("owner_id", SQLDataType.INTEGERUNSIGNED, this);

    /**
     * Flags see {@link ProtectionFlag}
     */
    public final TableField<LockModel, Short> FLAGS = createField("flags", SQLDataType.SMALLINT, this);

    /**
     * Protected Type see {@link ProtectedType}
     */
    public final TableField<LockModel, Byte> PROTECTED_TYPE = createField("type", SQLDataType.TINYINT, this);

    /**
     * LockType see {@link LockType}
     */
    public final TableField<LockModel, Byte> LOCK_TYPE = createField("lock_type", SQLDataType.TINYINT, this);

    // eg. /cguarded [pass <password>] (flag to create pw book/key?)
    public final TableField<LockModel, byte[]> PASSWORD = createField("password", SQLDataType.VARBINARY.length(128), this);

    // optional for entity protection:
    public final TableField<LockModel, Long> ENTITY_UID_LEAST = createField("entity_uid_least", SQLDataType.BIGINT, this);
    public final TableField<LockModel, Long> ENTITY_UID_MOST = createField("entity_uid_most", SQLDataType.BIGINT, this);

    public final TableField<LockModel, Timestamp> LAST_ACCESS = createField("last_access", SQLDataType.TIMESTAMP, this);
    public final TableField<LockModel, Timestamp> CREATED = createField("created", SQLDataType.TIMESTAMP, this);

    @Override
    public Class<LockModel> getRecordType() {
        return LockModel.class;
    }
}
