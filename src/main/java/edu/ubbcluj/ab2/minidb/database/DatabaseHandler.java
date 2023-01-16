package edu.ubbcluj.ab2.minidb.database;

public interface DatabaseHandler {
    // Return values: -1 = error, 0 = success, 1 = not modified
    int createDatabase(String databaseName);
    int dropDatabase(String databaseName);
    int createTable(String databaseName, String tableName, String primaryKey, String foreignKey, String uniqueKey, String fieldNames, String fieldTypes);
    int dropTable(String databaseName, String tableName);
    int createIndex(String databaseName, String tableName, String fieldName, String indexName);
    int insert(String databaseName, String tableName, String key, String value);
    int delete(String databaseName, String tableName, String key);
    String select(String databaseName, String tableNames, String fieldNames, String conditions);
    String selectWithNestedJoin(String databaseName, String tableName, String fieldNames, String joins, String conditions);
    String selectWithHashJoin(String databaseName, String tableName, String fieldNames, String joins, String conditions, String groupby, String aggregate);
}
