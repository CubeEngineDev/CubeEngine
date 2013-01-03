package de.cubeisland.cubeengine.core.storage.database.querybuilder;

public interface QueryBuilder
{
    /**
     * Starts an INSERT query.
     *
     * @return the InsertBuilder
     */
    public InsertBuilder insert();

    /**
     * Starts an MERGE query.
     *
     * @return the MergeBuilder
     */
    public MergeBuilder merge();

    /**
     * Starts an SELECT query.
     *
     * @param cols the tables to select from
     * @return the SelectBuilder
     */
    public SelectBuilder select(String... cols);

    /**
     * Starts an UPDATE query.
     *
     * @param tables the tables to update from
     * @return the UpdateBuilder
     */
    public UpdateBuilder update(String... tables);

    /**
     * Starts an DELETE query.
     *
     * @return the DeleteBuilder
     */
    public DeleteBuilder deleteFrom(String table);

    /**
     * STARTS a CREATE TABLE query
     *
     * @param name the table name
     * @param ifNoExist whether to only create the table if it doesn't already exist
     * @return the TableBuilder
     */
    public TableBuilder createTable(String name, boolean ifNoExist);

    /**
     * Clears the table
     *
     * @param table the table to truncate
     * @return fluent interface
     */
    public QueryBuilder truncateTable(String table);

    /**
     * Drops the table
     *
     * @param tables the tables to drop
     * @return fluent interface
     */
    public QueryBuilder dropTable(String... tables);

    /**
     * Starts a LOCK query
     *
     * @return the LockBuilder
     */
    public LockBuilder lock();

    /**
     * Starts a transaction
     *
     * @return fluent interface
     */
    public QueryBuilder startTransaction();

    /**
     * Commits transactions
     *
     * @return fluent interface
     */
    public QueryBuilder commit();

    /**
     * Rollbacks transactions
     *
     * @return fluent interface
     */
    public QueryBuilder rollback();

    /**
     * Unlocks tables.
     *
     * @return fluent interface
     */
    public QueryBuilder unlockTables();

    /**
     * Starts an other query.
     *
     * @return fluent interface
     */
    public QueryBuilder nextQuery();

    /**
     * Starts an ALTER TABLE query.
     *
     * @return the AlterTableBuilder
     */
    public AlterTableBuilder alterTable(String table);

    /**
     * Returns the finished query.
     *
     * @return the query as String
     */
    public String end();
    //TODO create DB
    //TODO create Index
}
