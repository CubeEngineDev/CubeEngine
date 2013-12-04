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
package de.cubeisland.engine.stats.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.core.storage.database.TableCreator;
import de.cubeisland.engine.core.storage.database.mysql.Keys;
import de.cubeisland.engine.core.storage.database.mysql.MySQLDatabaseConfiguration;
import de.cubeisland.engine.core.util.Version;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.stats.storage.TableStats.TABLE_STATS;

public class TableStatsData extends TableImpl<StatsDataModel> implements TableCreator<StatsDataModel>
{
    public static TableStatsData TABLE_STATSDATA;

    private TableStatsData(String prefix)
    {
        super(prefix + "statsdata");
        IDENTITY = Keys.identity(this, this.KEY);
        PRIMARY_KEY = Keys.uniqueKey(this, this.KEY);
        FOREIGN_STAT = Keys.foreignKey(TABLE_STATS.PRIMARY_KEY, this, this.STAT);
    }

    public final Identity<StatsDataModel, UInteger> IDENTITY;
    public final UniqueKey<StatsDataModel> PRIMARY_KEY;
    public final ForeignKey<StatsDataModel, StatsModel> FOREIGN_STAT;

    public final TableField<StatsDataModel, UInteger> KEY = createField("key", SQLDataType.INTEGERUNSIGNED, this);
    public final TableField<StatsDataModel, UInteger> STAT = createField("stat", SQLDataType.INTEGERUNSIGNED, this);
    public final TableField<StatsDataModel, Timestamp> TIMESTAMP = createField("timestamp", SQLDataType.TIMESTAMP, this);
    public final TableField<StatsDataModel, String> DATA = createField("data", SQLDataType.VARCHAR.length(64), this);
    
    public static TableStatsData initTable(Database database)
    {
        MySQLDatabaseConfiguration config = (MySQLDatabaseConfiguration)database.getDatabaseConfig();
        TABLE_STATSDATA = new TableStatsData(config.tablePrefix);
        return TABLE_STATSDATA;
    }

    @Override
    public void createTable(Connection connection) throws SQLException
    {
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.getName()+ " (\n" +
                                        "`key` int(10) unsigned NOT NULL AUTO_INCREMENT,\n" +
                                        "`stat` int(10) unsigned NOT NULL,\n" +
                                        "`timestamp` timestamp NOT NULL,\n" +
                                        "`data` varchar(64) DEFAULT NULL,\n" +
                                        "PRIMARY KEY (`key`),\n" +
                                        "FOREIGN KEY `f_stat`(`stat`) REFERENCES " + TABLE_STATS.getName() + "(`key`) ON UPDATE CASCADE ON DELETE CASCADE)" +
                                        "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci\n" +
                                        "COMMENT='1.0.0'").execute();
    }

    private static final Version version = new Version(1,1);

    @Override
    public Version getTableVersion()
    {
        return version;
    }

    @Override
    public Identity<StatsDataModel, UInteger> getIdentity()
    {
        return IDENTITY;
    }

    @Override
    public UniqueKey<StatsDataModel> getPrimaryKey()
    {
        return PRIMARY_KEY;
    }

    @Override
    public List<UniqueKey<StatsDataModel>> getKeys()
    {
        return Arrays.asList(PRIMARY_KEY);
    }

    @Override
    public List<ForeignKey<StatsDataModel, ?>> getReferences() {
        return Arrays.<ForeignKey<StatsDataModel, ?>>asList(FOREIGN_STAT);
    }

    @Override
    public Class<StatsDataModel> getRecordType() {
        return StatsDataModel.class;
    }
}
