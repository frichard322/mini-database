package edu.ubbcluj.ab2.minidb.client.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerController {
    private Socket socket;
    private ObjectInputStream din;
    private ObjectOutputStream dout;

    public ServerController() {
        try {
            socket = new Socket("localhost",8888);
            dout = new ObjectOutputStream(socket.getOutputStream());
            din = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("ERROR: Couldn't connect to server.");
        }
    }

    public void closeConnection() {
        try {
            dout.writeObject("exit");
            socket.close();
            din.close();
            dout.close();
        } catch (IOException e) {
            System.out.println("Client: Error(Closing)");
        }
    }

    public String[] getDatabases() {
        String[] dbs;

        try {
            // Send "0,"
            String cmd = "0,";
            dout.writeObject(cmd);

            // Waiting for server message - db list
            dbs = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return dbs;
    }

    public String[] getTables(String databaseName) {
        String[] tables;

        try {
            // Send "1,databaseName"
            String cmd = "1," + databaseName;
            dout.writeObject(cmd);

            // Waiting for server message - table list
            tables = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return tables;
    }

    public int createDatabase(String databaseName) {
        try {
            // Send "2,databaseName"
            String cmd = "2," + databaseName;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return -1;
        }
    }

    public int dropDatabase(String databaseName) {
        try {
            // Send "3,databaseName"
            String cmd = "3," + databaseName;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int createTable(String databaseName, String tableName, String primaryKey, String foreignKey, String uniqueKey, String fieldNames, String fieldTypes) {
        try {
            // Send "4,databaseName,tableName,primaryKey,foreignKey,uniqueKey,fieldNames,fieldTypes"
            String cmd = "4," + databaseName + "," + tableName + "," + primaryKey + ',' + foreignKey + ',' + uniqueKey + ',' + fieldNames + "," + fieldTypes;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return -1;
        }
    }

    public int dropTable(String databaseName, String tableName) {
        try {
            // Send "5,dbName,tableName"
            String cmd = "5," + databaseName + "," + tableName;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int createIndex(String databaseName, String tableName, String fieldName, String indexName) {
        try {
            // Send "6,databaseName,tableName,fieldName,indexName"
            String cmd = "6," + databaseName + "," + tableName + "," + fieldName + ',' + indexName;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String[] getKeysFromTable(String databaseName, String tableName, String keyType) {
        String[] tables;

        try {
            // Send "7,databaseName,tableName"
            String cmd = "7," + databaseName + ',' + tableName + ',' + keyType;
            dout.writeObject(cmd);

            // Waiting for server message - table list
            tables = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return tables;
    }

    public String[] getFieldsFromTable(String databaseName, String tableName) {
        String[] tables;

        try {
            // Send "7,databaseName,tableName"
            String cmd = "7," + databaseName + ',' + tableName + ',' + "fieldNames";
            dout.writeObject(cmd);

            // Waiting for server message - table list
            tables = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return tables;
    }

    public String[] getFieldTypesFromTable(String databaseName, String tableName) {
        String[] tables;

        try {
            // Send "7,databaseName,tableName"
            String cmd = "7," + databaseName + ',' + tableName + ',' + "fieldTypes";
            dout.writeObject(cmd);

            // Waiting for server message - table list
            tables = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return tables;
    }

    public int insertRow(String databaseName, String tableName, String key, String value) {
        try {
            // Send "8,databaseName,tableName,keyName"
            String cmd = "8," + databaseName + "," + tableName + "," + key + ',' + value;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return -1;
        }
    }

    public int deleteRow(String databaseName, String tableName, String key) {
        try {
            // Send "9,databaseName,tableName,keyName"
            String cmd = "9," + databaseName + "," + tableName + "," + key;
            dout.writeObject(cmd);

            // Waiting for server message
            return (int) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String[] getForeignFieldCandidates(String databaseName, String tableName) {
        String[] tables;

        try {
            // Send "10,databaseName,tableName"
            String cmd = "10," + databaseName + ',' + tableName;
            dout.writeObject(cmd);

            // Waiting for server message - table list
            tables = ((String) din.readObject()).split("#");
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return tables;
    }

    public String selectFrom(String databaseName, String tableNames, String fieldNames, String conditions) {
        try {
            // Send "11,..."
            String cmd = "11," +
                    databaseName + "," +
                    tableNames + "," +
                    fieldNames + "," +
                    conditions;
            dout.writeObject(cmd);

            // Waiting for server message
            return (String) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public String selectFromWithJoin(String databaseName, String tableNames, String fieldNames, String tableJoins, String conditions) {
        // Send "12,..."
        try {
            String cmd = "12," +
                    databaseName + "," +
                    tableNames + "," +
                    fieldNames + "," +
                    tableJoins + "," +
                    conditions;
            dout.writeObject(cmd);

            // Waiting for server message
            return (String) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public String selectFromWithGroupBy(String databaseName, String tableNames, String fieldNames,
                                     String tableJoins, String conditions, String groupBy, String aggrFunctions) {
        // Send "13,..."
        try {
            String cmd = "13," +
                    databaseName + "," +
                    tableNames + "," +
                    fieldNames + "," +
                    tableJoins + "," +
                    conditions + "," +
                    groupBy + "," +
                    aggrFunctions;
            dout.writeObject(cmd);

            // Waiting for server message
            return (String) din.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
