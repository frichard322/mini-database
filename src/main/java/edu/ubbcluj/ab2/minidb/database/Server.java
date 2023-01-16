package edu.ubbcluj.ab2.minidb.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import edu.ubbcluj.ab2.minidb.config.ConfigBean;
import edu.ubbcluj.ab2.minidb.config.ConfigBeanFactory;
import org.bson.Document;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server implements DatabaseHandler {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectInputStream din;
    private ObjectOutputStream dout;
    private final MongoClient CLIENT;

    public Server() {
        ConfigBean configBean = ConfigBeanFactory.getConfigBean();
        CLIENT = MongoClients.create(configBean.getMongoClientURL());

        try{
            serverSocket = new ServerSocket(8888);
            socket = serverSocket.accept();
            dout = new ObjectOutputStream(socket.getOutputStream());
            din = new ObjectInputStream(socket.getInputStream());
            handleClient();
        } catch (Exception e) {
            e.printStackTrace();
            closeConnections();
        }
    }

    private void handleClient() throws IOException, ClassNotFoundException {
        while(true) {
            String[] cmd = ((String) din.readObject()).split(",");
            if(cmd[0].equals("exit")){
                System.out.println("Server exited!");
                closeConnections();
                break;
            }

            switch(cmd[0]) {
                //Client asks for database list
                case "0" -> dout.writeObject(getDatabaseList());
                //Client asks for table list of database
                case "1" -> dout.writeObject(getTableList(cmd[1]));
                //Client asks server to create database
                case "2" -> dout.writeObject(createDatabase(cmd[1]));
                //Client asks server to drop database
                case "3" -> dout.writeObject(dropDatabase(cmd[1]));
                //Client asks server to create table
                case "4" -> dout.writeObject(createTable(cmd[1],cmd[2],cmd[3],cmd[4],cmd[5],cmd[6],cmd[7]));
                //Client asks server to drop table
                case "5" -> dout.writeObject(dropTable(cmd[1],cmd[2]));
                //Client asks server to create index
                case "6" -> dout.writeObject(createIndex(cmd[1],cmd[2],cmd[3],cmd[4]));
                //Client asks for attribute of table
                case "7" -> dout.writeObject(getTableAttribute(cmd[1],cmd[2],cmd[3]));
                case "8" -> dout.writeObject(insert(cmd[1],cmd[2],cmd[3],cmd[4]));
                //Client asks server to delete row from table
                case "9" -> dout.writeObject(delete(cmd[1],cmd[2],cmd[3]));
                //Client asks for primary/unique keys
                case "10" -> dout.writeObject(getTableAttribute(cmd[1],cmd[2],"primaryKey") + getTableAttribute(cmd[1],cmd[2],"uniqueKey"));
                //Client asks for select without joins
                case "11" -> dout.writeObject(select(cmd[1],cmd[2],cmd[3],cmd[4]));
                //Client asks for select with joins
                case "12" -> dout.writeObject(selectWithNestedJoin(cmd[1],cmd[2],cmd[3],cmd[4], cmd[5]));
                //Client asks for select with hash join, group by and aggregated functions
                // TODO: db, tableName, fieldNames, joins, conditions, group by (tabla.field#tabla2.field2#), aggr (name/(min/max/avg/sum/count)/tablename.fieldname)/
                case "13" -> dout.writeObject(selectWithHashJoin(cmd[1],cmd[2],cmd[3],cmd[4], cmd[5], cmd[6], cmd[7]));
                //Unknown command
                default -> System.out.println("Wrong command!");
            }
        }
    }

// Private Methods:
    private void importRows(String fileName, String databaseName, String tableName){
        MongoCollection<Document> table = CLIENT.getDatabase(databaseName).getCollection(tableName);
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach( row -> {
                String[] rowSplitted = row.split("#",2);
                insert(databaseName, tableName, rowSplitted[0], rowSplitted[0] + "#" + rowSplitted[1] + "#");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDatabaseList() {
        return CLIENT.listDatabaseNames()
                .into(new ArrayList<>())
                .stream()
                .filter(db -> !db.equalsIgnoreCase("local")
                        && !db.equalsIgnoreCase("admin")
                        && !db.equalsIgnoreCase("config"))
                .collect(Collectors.joining("#","","#"));
    }

    private String getTableList(String databaseName) {
        return CLIENT.getDatabase(databaseName)
                .listCollectionNames()
                .into(new ArrayList<>())
                .stream()
                .filter(table -> !table.equalsIgnoreCase("Structure") && !table.contains("Index_"))
                .collect(Collectors.joining("#","","#"));
    }

    private String getTableAttribute(String databaseName, String tableName, String attributeName) {
        Document table = CLIENT.getDatabase(databaseName).getCollection("Structure").find(Filters.eq("_id",tableName)).first();
        return (table == null) ? "" : table.getString(attributeName);
    }

    private List<List<String>> cartesianProduct(List<List<String>> lists) {
        List<List<String>> resultLists = new ArrayList<>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            List<String> firstList = lists.get(0);
            List<List<String>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (String condition : firstList) {
                for (List<String> remainingList : remainingLists) {
                    ArrayList<String> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    private <T> void updateIndexCollection(String primaryKey, String operation, MongoCollection<Document> index, String newValue, T id) {
        Document doc = index.find(Filters.eq("_id", id)).first();
        if(operation.equals("insert")) {
            if(doc != null) {
                newValue = doc.getString("value").concat(primaryKey.concat(","));
            } else {
                index.insertOne(new Document().append("_id", id).append("value", primaryKey.concat(",")));
                return;
            }
        } else if(operation.equals("delete")) {
            if(doc != null) {
                newValue = doc.getString("value").replace(primaryKey.concat(","), "");
            }
        }
        if(!newValue.equals("")) {
            index.findOneAndUpdate(Filters.eq("_id", id), Updates.set("value", newValue));
        } else {
            index.findOneAndDelete(Filters.eq("_id", id));
        }
    }

    private void updateIndexes(String databaseName, String tableName, String primaryKey, String value, String operation) {
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        Document tableAttributes = structure.find(Filters.eq("_id", tableName)).first();
        if(tableAttributes != null) {
            ArrayList<String> indexes = tableAttributes.get("indexes", new ArrayList<>());
            indexes.forEach(indexItr -> {
                String[] splitted = indexItr.split("/"); // 0 - indexName, 1 - fieldName (in current table)
                int i = Arrays.asList(tableAttributes.getString("fieldNames").split("#")).indexOf(splitted[1]);
                String type = Arrays.asList(tableAttributes.getString("fieldTypes").split("#")).get(i);
                MongoCollection<Document> index = db.getCollection(splitted[0]);
                String newValue = "";
                if(type.equals("int")) {
                    Integer id = Integer.parseInt(value.split("#")[i]);
                    updateIndexCollection(primaryKey, operation, index, newValue, id);
                } else if(type.equals("float")) {
                    Float id = Float.parseFloat(value.split("#")[i]);
                    updateIndexCollection(primaryKey, operation, index, newValue, id);
                } else {
                    String id = value.split("#")[i];
                    updateIndexCollection(primaryKey, operation, index, newValue, id);
                }
            });
        }
    }

    private List<String> applyingConditions(MongoCollection<Document> structure, String fields, String conditions, List<String> data) {
        for (String c : conditions.split("#")) {
            String[] cond = c.split("/");
            Document tableAttributes = structure.find(Filters.eq("_id", cond[1].split("\\.")[0])).first();
            assert tableAttributes != null;
            String type = Arrays.asList(tableAttributes.getString("fieldTypes").split("#")).get(Arrays.asList(tableAttributes.getString("fieldNames").split("#")).indexOf(cond[1].split("\\.")[1]));
            int i = Arrays.asList(fields.split("#")).indexOf(cond[1]);
            if (cond[0].equals("0")) { // direct value
                if (type.equals("int")) {
                    switch (cond[2]) {
                        case "<" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) < Integer.parseInt(cond[3])).collect(Collectors.toList());
                        case "<=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) <= Integer.parseInt(cond[3])).collect(Collectors.toList());
                        case ">" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) > Integer.parseInt(cond[3])).collect(Collectors.toList());
                        case ">=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) >= Integer.parseInt(cond[3])).collect(Collectors.toList());
                        case "=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) == Integer.parseInt(cond[3])).collect(Collectors.toList());
                    }
                } else if (type.equals("float")) {
                    switch (cond[2]) {
                        case "<" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) < Float.parseFloat(cond[3])).collect(Collectors.toList());
                        case "<=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) <= Float.parseFloat(cond[3])).collect(Collectors.toList());
                        case ">" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) > Float.parseFloat(cond[3])).collect(Collectors.toList());
                        case ">=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) >= Float.parseFloat(cond[3])).collect(Collectors.toList());
                        case "=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) == Float.parseFloat(cond[3])).collect(Collectors.toList());
                    }
                } else {
                    if (cond[2].equals("=")) {
                        data = data.stream().filter(row -> row.split("#")[i].equals(cond[3])).collect(Collectors.toList());
                    }
                }
            } else { // indirect value
                int j = Arrays.asList(fields.split("#")).indexOf(cond[3]);
                if (type.equals("int")) {
                    switch (cond[2]) {
                        case "<" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) < Integer.parseInt((row.split("#")[j]))).collect(Collectors.toList());
                        case "<=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) <= Integer.parseInt((row.split("#")[j]))).collect(Collectors.toList());
                        case ">" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) > Integer.parseInt((row.split("#")[j]))).collect(Collectors.toList());
                        case ">=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) >= Integer.parseInt((row.split("#")[j]))).collect(Collectors.toList());
                        case "=" -> data = data.stream().filter(row -> Integer.parseInt((row.split("#")[i])) == Integer.parseInt((row.split("#")[j]))).collect(Collectors.toList());
                    }
                } else if (type.equals("float")) {
                    switch (cond[2]) {
                        case "<" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) < Float.parseFloat((row.split("#")[j]))).collect(Collectors.toList());
                        case "<=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) <= Float.parseFloat((row.split("#")[j]))).collect(Collectors.toList());
                        case ">" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) > Float.parseFloat((row.split("#")[j]))).collect(Collectors.toList());
                        case ">=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) >= Float.parseFloat((row.split("#")[j]))).collect(Collectors.toList());
                        case "=" -> data = data.stream().filter(row -> Float.parseFloat((row.split("#")[i])) == Float.parseFloat((row.split("#")[j]))).collect(Collectors.toList());
                    }
                } else {
                    if (cond[2].equals("=")) {
                        data = data.stream().filter(row -> row.split("#")[i].equals((row.split("#")[j]))).collect(Collectors.toList());
                    }
                }
            }
        }
        return data;
    }

    private String projection(String fieldNames, String fields, List<String> data, List<Integer> indexesOfFields) {
        StringBuilder result = new StringBuilder();
        StringBuilder tmp = new StringBuilder();
        if(indexesOfFields != null) {
            if (!fieldNames.equals(" ") && indexesOfFields.size() > 0) {
                String[] fieldsArray = fieldNames.split("#");
                String[] fieldsAllArray = fields.split("#");
                indexesOfFields.forEach(i -> tmp.append(fieldsAllArray[i]).append("#"));
                String tester = tmp.toString();
                fieldNames = Arrays.stream(fieldsArray).filter(field -> !tester.contains(field)).collect(Collectors.joining("#", "", "#"));
            }
        }
        if(fieldNames.equals(" ")){
            // SELECT * (ALL TABLES)
            result.append(fields.replaceAll("#","\t")).append("\n");
            for (String row : data) {
                result.append(row.replaceAll("#","\t")).append("\n");
            }
        } else {
            // Removing unselected fields and data
            ArrayList<Integer> indexes = new ArrayList<>();
            String[] fieldList = fields.split("#");
            for (int i = 0; i < fieldList.length; i++) {
                if (fieldNames.contains(fieldList[i])) {
                    indexes.add(i);
                    result.append(fieldList[i]).append("\t");
                }
            }
            result.append("\n");

            for (String row : data) {
                fieldList = row.split("#");
                for (int j = 0; j < fieldList.length; j++) {
                    if (indexes.contains(j)) {
                        result.append(fieldList[j]).append("\t");
                    }
                }
                result.append("\n");
            }
        }
        return result.toString();
    }

    private String aggregation(List<String> listOfFields, List<String> listOfTypes, List<String> data, String groupby, String aggregate) {
        StringBuilder result = new StringBuilder();
        if(aggregate.length() > 0 && !aggregate.equals(" ")) {
            String[] tmp = aggregate.split("/");
            String title = tmp[0];
            String func = tmp[1];
            String paramFieldName = tmp[2];
            int index = listOfFields.indexOf(paramFieldName);
            String paramType = listOfTypes.get(index);
            if(groupby.equals(" ")) {
                result.append(title).append("\n");
                switch (func) {
                    case "count" -> result.append(data.size()).append("\n");
                    case "sum" -> {
                        if(paramType.equals("int")) {
                            int sum = data.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).sum();
                            result.append(sum).append("\n");
                        } else if(paramType.equals("float")) {
                            double sum = data.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).sum();
                            result.append(sum).append("\n");
                        } else {
                            return null;
                        }
                    }
                    case "min" -> {
                        if(paramType.equals("int")) {
                            int min = data.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).min().orElse(Integer.MIN_VALUE);
                            result.append(min).append("\n");
                        } else if(paramType.equals("float")) {
                            double min = data.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).min().orElse(Double.MIN_VALUE);
                            result.append(min).append("\n");
                        } else {
                            return null;
                        }
                    }
                    case "max" -> {
                        if(paramType.equals("int")) {
                            int max = data.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).max().orElse(Integer.MAX_VALUE);
                            result.append(max).append("\n");
                        } else if(paramType.equals("float")) {
                            double max = data.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).max().orElse(Double.MAX_VALUE);
                            result.append(max).append("\n");
                        } else {
                            return null;
                        }
                    }
                    case "avg" -> {
                        if(paramType.equals("int") || paramType.equals("float")) {
                            double avg = data.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).average().orElse(Double.MIN_VALUE);
                            result.append(avg).append("\n");
                        } else {
                            return null;
                        }
                    }
                }
            } else {
                List<String> groupByFields = Arrays.asList(groupby.split("#"));
                List<Integer> indexesOfFields = groupByFields.stream().map(listOfFields::indexOf).collect(Collectors.toList());
                String groupByType;
                if(groupByFields.size() > 1) {
                    groupByType = "string";
                } else {
                    groupByType = listOfTypes.get(listOfFields.indexOf(groupByFields.get(0)));
                }
                groupByFields.forEach(field -> result.append(field).append("\t"));
                result.append(title).append("\n");
                HashMap<Object, ArrayList<String>> hash = new HashMap<>();
                if(groupByType.equals("int")) {
                    data.forEach(row -> {
                        int key = Integer.parseInt(row.split("#")[indexesOfFields.get(0)]);
                        ArrayList<String> array = hash.get(key);
                        if (array == null) {
                            array = new ArrayList<>();
                        }
                        array.add(row);
                        hash.put(key, array);
                    });
                } else if(groupby.equals("float")) {
                    data.forEach(row -> {
                        float key = Float.parseFloat(row.split("#")[indexesOfFields.get(0)]);
                        ArrayList<String> array = hash.get(key);
                        if (array == null) {
                            array = new ArrayList<>();
                        }
                        array.add(row);
                        hash.put(key, array);
                    });
                } else {
                    data.forEach(row -> {
                        String[] fields = row.split("#");
                        StringBuilder key = new StringBuilder();
                        indexesOfFields.forEach(i -> key.append(fields[i]).append("#"));
                        ArrayList<String> array = hash.get(key.toString());
                        if (array == null) {
                            array = new ArrayList<>();
                        }
                        array.add(row);
                        hash.put(key.toString(), array);
                    });
                }
                switch (func) {
                    case "count" -> hash.forEach((k, v) -> {
                            Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                            result.append(v.size()).append("\n");
                        });
                    case "sum" -> {
                        if(paramType.equals("int")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                int sum = v.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).sum();
                                result.append(sum).append("\n");
                            });
                        } else if(paramType.equals("float")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                double sum = v.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).sum();
                                result.append(sum).append("\n");
                            });
                        } else {
                            return null;
                        }
                    }
                    case "min" -> {
                        if(paramType.equals("int")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                int min = v.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).min().orElse(Integer.MIN_VALUE);
                                result.append(min).append("\n");
                            });
                        } else if(paramType.equals("float")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                double min = v.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).min().orElse(Double.MIN_VALUE);
                                result.append(min).append("\n");
                            });
                        } else {
                            return null;
                        }
                    }
                    case "max" -> {
                        if(paramType.equals("int")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                int max = v.stream().mapToInt(row -> Integer.parseInt(row.split("#")[index])).max().orElse(Integer.MAX_VALUE);
                                result.append(max).append("\n");
                            });
                        } else if(paramType.equals("float")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                double max = v.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).max().orElse(Double.MAX_VALUE);
                                result.append(max).append("\n");
                            });
                        } else {
                            return null;
                        }
                    }
                    case "avg" -> {
                        if(paramType.equals("int") || paramType.equals("float")) {
                            hash.forEach((k, v) -> {
                                Arrays.asList(k.toString().split("#")).forEach(str -> result.append(str).append("\t"));
                                double avg = v.stream().mapToDouble(row -> Double.parseDouble(row.split("#")[index])).average().orElse(Double.MIN_VALUE);
                                result.append(avg).append("\n");
                            });
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return result.toString();
    }

    private void closeConnections() {
        try {
            din.close();
            dout.close();
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error: closing connections");
        }
    }

// Public Methods:

    @Override
    public int createDatabase(String databaseName) {
        if (CLIENT.listDatabaseNames().into(new ArrayList<>()).contains(databaseName)) {
            return 1;
        }
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        db.createCollection("Structure");
        return 0;
    }

    @Override
    public int dropDatabase(String databaseName) {
        if(databaseName.equalsIgnoreCase("Structure")){
            return -1;
        }
        for (String db : CLIENT.listDatabaseNames()) {
            if (db.equalsIgnoreCase(databaseName)) {
                CLIENT.getDatabase(databaseName).drop();
                return 0;
            }
        }
        return 1;
    }

    @Override
    public int createTable(String databaseName, String tableName, String primaryKey, String foreignKey, String uniqueKey, String fieldNames, String fieldTypes) {
        if(tableName.equalsIgnoreCase("Structure")) {
            return -1;
        }
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        if(db.listCollectionNames().into(new ArrayList<>()).contains(tableName)) {
            return 1;
        }
        db.createCollection(tableName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        String primaryKeyType = (primaryKey.split("#").length > 1) ? "complex" : Arrays.asList(fieldTypes.split("#")).get(Arrays.asList(fieldNames.split("#")).indexOf(primaryKey.replace("#","")));
        structure.insertOne(new Document()
                .append("_id",tableName)
                .append("primaryKey",primaryKey)
                .append("foreignKey",foreignKey)
                .append("uniqueKey",uniqueKey)
                .append("fieldNames",fieldNames)
                .append("fieldTypes",fieldTypes)
                .append("indexes",new ArrayList<String>())
                .append("primaryKeyType", primaryKeyType)
                .append("used",0)
        );
        if(!foreignKey.equals("")) {
            for (String foreignField : foreignKey.split("#")) {
                String foreignTableName = foreignField.split("/")[1].split("\\.")[0];
                structure.updateOne(Filters.eq("_id",foreignTableName), Updates.inc("used",1));
            }
            Arrays.stream(foreignKey.split("#")).forEach(field -> createIndex(databaseName, tableName, field.split("/")[0], "FK_" + field.split("/")[0]));
        }
        if(!uniqueKey.equals("")) {
            Arrays.stream(uniqueKey.split("#")).forEach(field -> createIndex(databaseName, tableName, field, "UK_" + field));
        }
        return 0;
    }

    @Override
    public int dropTable(String databaseName, String tableName) {
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        String foreignKey = getTableAttribute(databaseName,tableName,"foreignKey");
        if(!db.listCollectionNames().into(new ArrayList<>()).contains(tableName)) {
            return 1;
        }
        Document table = structure.find(Filters.and(Filters.eq("_id",tableName), Filters.eq("used",0))).first();
        if(table != null) {
            db.getCollection(tableName).drop();
            if (!foreignKey.equals("")) {
                for (String foreignField : foreignKey.split("#")) {
                    String foreignTableName = foreignField.split("/")[1].split("\\.")[0];
                    structure.updateOne(Filters.eq("_id",foreignTableName), Updates.inc("used",-1));
                }
            }
            table.get("indexes", new ArrayList<>()).forEach(index -> db.getCollection(((String) index).split("/")[0]).drop());
            structure.deleteOne(table);
            return 0;
        }
        return -1;
    }

    @Override
    public int createIndex(String databaseName, String tableName, String fieldName, String indexName) {
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        Document table = structure.find(Filters.eq("_id",tableName)).first();
        if(table == null) {
            return -1;
        }
        indexName = "Index_".concat(tableName.concat("_")).concat(indexName);
        if(db.listCollectionNames().into(new ArrayList<>()).contains(indexName)) {
           return 1;
        }
        structure.findOneAndUpdate(Filters.eq("_id", tableName),Updates.push("indexes", indexName.concat("/").concat(fieldName)));
        db.createCollection(indexName);
        MongoCollection<Document> indexTable = db.getCollection(indexName);
        int i = Arrays.asList(table.getString("fieldNames").split("#")).indexOf(fieldName);
        if(i == -1) {
            return -1;
        }
        HashMap<String, ArrayList<String>> map = new HashMap<>();
        for (Document row : db.getCollection(tableName).find()) {
            String key = row.getString("value").split("#")[i];
            ArrayList<String> array = map.get(key);
            if (array == null) {
                array = new ArrayList<>();
            }
            array.add(row.getString("primaryKey"));
            map.put(key, array);
        }
        String type = Arrays.asList(table.getString("fieldTypes").split("#")).get(i);
        if (type.equals("int")) {
            map.forEach((primaryKey, array) -> indexTable.insertOne(new Document()
                    .append("_id", Integer.parseInt(primaryKey))
                    .append("value", array.stream().map(Object::toString).collect(Collectors.joining(",", "", ",")))));
        } else if (type.equals("float")) {
            map.forEach((primaryKey, array) -> indexTable.insertOne(new Document()
                    .append("_id", Float.parseFloat(primaryKey))
                    .append("value", array.stream().map(Object::toString).collect(Collectors.joining(",", "", ",")))));
        } else {
            map.forEach((primaryKey, array) -> indexTable.insertOne(new Document()
                    .append("_id", primaryKey)
                    .append("value", array.stream().map(Object::toString).collect(Collectors.joining(",", "", ",")))));
        }
        return 0;
    }

    @Override
    public int insert(String databaseName, String tableName, String primaryKey, String value) {
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        MongoCollection<Document> table =  db.getCollection(tableName);
        Document tableAttributes = structure.find(Filters.eq("_id", tableName)).first();
        if(tableAttributes == null) {
            return -1;
        }
        String primaryKeyType = tableAttributes.getString("primaryKeyType");
        if(primaryKey.contains("?")) {
            if(primaryKeyType.equals("int")){
                primaryKey = String.valueOf(table.find().into(new ArrayList<>()).stream().map(doc -> doc.getInteger("_id")).max(Integer::compare).orElse(-1) + 1);
                value = value.replace("?", primaryKey);
            }
            else {
                return -1;
            }
        }
        if(primaryKeyType.equals("int")) {
            primaryKey = primaryKey.replace("#","");
            if(table.find(Filters.eq("_id",Integer.parseInt(primaryKey))).first() != null){ return 1; }
            table.insertOne(new Document().append("_id", Integer.parseInt(primaryKey)).append("value", value));
        } else if (primaryKeyType.equals("float")) {
            primaryKey = primaryKey.replace("#","");
            if(table.find(Filters.eq("_id",Float.parseFloat(primaryKey))).first() != null){ return 1; }
            table.insertOne(new Document().append("_id", Float.parseFloat(primaryKey.replace("#",""))).append("value", value));
        } else {
            if(table.find(Filters.eq("_id",primaryKey)).first() != null){ return 1; }
            table.insertOne(new Document().append("_id", primaryKey).append("value", value));
        }
        updateIndexes(databaseName, tableName, primaryKey, value, "insert");
        return 0;
    }

    @Override
    public int delete(String databaseName, String tableName, String primaryKey) {
        MongoDatabase db = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> table = db.getCollection(tableName);
        MongoCollection<Document> structure = db.getCollection("Structure");
        Document tableAttributes = structure.find(Filters.eq("_id", tableName)).first();
        if(tableAttributes == null) {
            return -1;
        }
        String primaryKeyType = tableAttributes.getString("primaryKeyType");
        Document doc;
        if(primaryKeyType.equals("int")) {
            primaryKey = primaryKey.replace("#","");
            doc = table.find(Filters.eq("_id", Integer.parseInt(primaryKey))).first();
        } else if (primaryKeyType.equals("float")) {
            primaryKey = primaryKey.replace("#","");
            doc = table.find(Filters.eq("_id", Float.parseFloat(primaryKey))).first();
        } else {
            doc = table.find(Filters.eq("_id", primaryKey)).first();
        }
        if(doc == null) {
            return 1;
        }
        updateIndexes(databaseName, tableName, primaryKey, doc.getString("value"), "delete");
        table.deleteOne(doc);
        return 0;
    }

    @Override
    public String select(String databaseName, String tableNames, String fieldNames, String conditions) {
        MongoDatabase database = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = database.getCollection("Structure");
        // fields = Getting all the fieldnames from tables into a string that looks like <tableName>.<fieldName>
        StringBuilder fields = new StringBuilder();
        Arrays.stream(tableNames.split("#")).forEach( tb ->
                fields.append(Arrays.stream(getTableAttribute(databaseName, tb, "fieldNames").split("#"))
                .map(x -> x = tb.concat(".").concat(x))
                .collect(Collectors.joining("#"))
                .concat("#"))
        );

        ArrayList<MongoCollection<Document>> tables = new ArrayList<>();
        List<List<String>> rawData = new ArrayList<>();
        Arrays.asList(tableNames.split("#")).forEach(tb -> {
            tables.add(database.getCollection(tb));
            List<String> rows = new ArrayList<>();
            tables.get(tables.size() - 1).find().forEach(row -> rows.add(row.getString("value")));
            rawData.add(rows);
        });
        List<String> data = cartesianProduct(rawData).stream().map(row -> String.join("", row)).collect(Collectors.toList());

        if(!conditions.equals(" ")) {
            data = applyingConditions(structure, fields.toString(), conditions, data);
        }
        return projection(fieldNames, fields.toString(), data, null);
    }

    @Override
    public String selectWithNestedJoin(String databaseName, String tableName, String fieldNames, String joins, String conditions) {
        MongoDatabase database = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = database.getCollection("Structure");
        // fields = Getting all the fieldnames from tables into a string that looks like <tableName>.<fieldName>
        StringBuilder fields = new StringBuilder();
        StringBuilder types = new StringBuilder();
        Arrays.stream(tableName.split("#")).forEach( tb -> {
                    fields.append(Arrays.stream(getTableAttribute(databaseName, tb, "fieldNames").split("#"))
                            .map(x -> x = tb.concat(".").concat(x))
                            .collect(Collectors.joining("#"))
                            .concat("#"));
                    types.append(getTableAttribute(databaseName, tb, "fieldTypes"));
                }
        );
        ArrayList<String> joiningTables = new ArrayList<>();
        ArrayList<String> table1Fields = new ArrayList<>();
        ArrayList<String> table2Fields = new ArrayList<>();
        ArrayList<Integer> indexesOfFields = new ArrayList<>();
        if(joins.length() > 0 && !joins.equals(" ")) {
            Arrays.stream(joins.split("#")).forEach(join -> {
                        String[] tmp = join.split("/", 4);
                        joiningTables.add(tmp[0]);
                        table1Fields.add(tmp[1]);
                        table2Fields.add(tmp[2]);
                        int n = fields.toString().split("#").length;
                        AtomicInteger i = new AtomicInteger(n);
                        fields.append(Arrays.stream(getTableAttribute(databaseName, tmp[0], "fieldNames").split("#"))
                                .map(field -> {
                                    if (field.equals(tmp[2].split("\\.")[1])) {
                                        indexesOfFields.add(i.getAndIncrement());
                                    } else {
                                        i.getAndIncrement();
                                    }
                                    field = tmp[0].concat(".").concat(field);
                                    return field;
                                })
                                .collect(Collectors.joining("#"))
                                .concat("#"));
                        types.append(getTableAttribute(databaseName, tmp[0], "fieldTypes"));
                    }
            );
        }
        List<String> listOfFields = Arrays.asList(fields.toString().split("#"));
        List<String> listOfTypes = Arrays.asList(types.toString().split("#"));
        List<String> data = database.getCollection(tableName).find().into(new ArrayList<>()).stream().map(row -> row.getString("value")).collect(Collectors.toList());
        for(int i = 0; i < joiningTables.size(); i++) {
            int firstFieldIndex = listOfFields.indexOf(table1Fields.get(i));
            int secondFieldIndex = listOfFields.indexOf(table2Fields.get(i));
            String nextTable = joiningTables.get(i);
            MongoCollection<Document> nextTableCollection = database.getCollection(nextTable);
            String nextTablePrimaryKeyType = structure.find(Filters.eq("_id", nextTable)).first().getString("primaryKeyType");
            String nextFieldName = listOfFields.get(secondFieldIndex).split("\\.")[1].concat("#");
            boolean isPrimaryKey = (structure.find(Filters.eq("_id", nextTable)).first().getString("primaryKey").equals(nextFieldName));
            boolean isUniqueKey = (structure.find(Filters.eq("_id", nextTable)).first().getString("uniqueKey").equals(nextFieldName));
            MongoCollection<Document> indexCollection = null;
            if(!isPrimaryKey) {
                if (isUniqueKey) {
                    indexCollection = database.getCollection("Index_".concat(nextTable).concat("_UK_").concat(listOfFields.get(secondFieldIndex)));
                } else {
                    String indexName = "Index_".concat(nextTable).concat("_").concat(listOfFields.get(secondFieldIndex));
                    try {
                        indexCollection = database.getCollection(indexName);
                    } catch (IllegalArgumentException e){
                        createIndex(databaseName, nextTable, listOfFields.get(secondFieldIndex), indexName);
                        indexCollection = database.getCollection(indexName);
                    }
                }
            }
            MongoCollection<Document> finalIndexCollection = indexCollection;
            data = data.stream().map(row -> {
                String value = row.split("#")[firstFieldIndex];
                String type = listOfTypes.get(firstFieldIndex);
                if(isPrimaryKey) { // PrimaryKey
                    if (type.equals("int")) {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", Integer.parseInt(value))).first().getString("value"));
                    } else if (type.equals("float")) {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", Float.parseFloat(value))).first().getString("value"));
                    } else {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", value)).first().getString("value"));
                    }
                } else if(isUniqueKey){ // unique key
                    String value2 = "";
                    if(type.equals("int")) {
                        value2 = finalIndexCollection.find(Filters.eq("_id", Integer.parseInt(value))).first().getString("value").split(",")[0];
                    } else if(type.equals("float")) {
                        value2 = finalIndexCollection.find(Filters.eq("_id", Float.parseFloat(value))).first().getString("value").split(",")[0];
                    } else {
                        value2 = finalIndexCollection.find(Filters.eq("_id", value)).first().getString("value").split(",")[0];
                    }
                    if (nextTablePrimaryKeyType.equals("int")) {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", Integer.parseInt(value2))).first().getString("value"));
                    } else if (nextTablePrimaryKeyType.equals("float")) {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", Float.parseFloat(value2))).first().getString("value"));
                    } else {
                        return row.concat(nextTableCollection.find(Filters.eq("_id", value2)).first().getString("value"));
                    }
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
        }

        if(data.get(0) == null) {
            return "[ERROR] The keys in the join are not unique!";
        }

        if(!conditions.equals(" ")) {
            data = applyingConditions(structure, fields.toString(), conditions, data);
        }
        return projection(fieldNames, fields.toString(), data, indexesOfFields);
    }

    @Override
    public String selectWithHashJoin(String databaseName, String tableName, String fieldNames, String joins, String conditions, String groupby, String aggregate) {
        MongoDatabase database = CLIENT.getDatabase(databaseName);
        MongoCollection<Document> structure = database.getCollection("Structure");
        StringBuilder fields = new StringBuilder().append(Arrays.stream(getTableAttribute(databaseName, tableName, "fieldNames").split("#"))
                .map(x -> x = tableName.concat(".").concat(x))
                .collect(Collectors.joining("#"))
                .concat("#"));
        StringBuilder types = new StringBuilder().append(getTableAttribute(databaseName, tableName, "fieldTypes"));
        ArrayList<Integer> numberOfFields = new ArrayList<>();
        ArrayList<String> joiningTables = new ArrayList<>();
        ArrayList<String> table1Fields = new ArrayList<>();
        ArrayList<String> table2Fields = new ArrayList<>();
        if(joins.length() > 0 && !joins.equals(" ")) {
            Arrays.stream(joins.split("#")).forEach(join -> {
                String[] tmp = join.split("/", 4);
                joiningTables.add(tmp[0]);
                table1Fields.add(tmp[1]);
                table2Fields.add(tmp[2]);
                int n = fields.toString().split("#").length;
                numberOfFields.add(n);
                AtomicInteger i = new AtomicInteger(n);
                fields.append(Arrays.stream(getTableAttribute(databaseName, tmp[0], "fieldNames").split("#"))
                        .map(field -> {
                            i.getAndIncrement();
                            field = tmp[0].concat(".").concat(field);
                            return field;
                        })
                        .collect(Collectors.joining("#"))
                        .concat("#"));
                types.append(getTableAttribute(databaseName, tmp[0], "fieldTypes"));
            });
        }
        List<String> listOfFields = Arrays.asList(fields.toString().split("#"));
        if(numberOfFields.size() > 0){
            numberOfFields.add(listOfFields.size() - numberOfFields.get(numberOfFields.size()-1));
        }
        List<String> listOfTypes = Arrays.asList(types.toString().split("#"));
        List<String> data = database.getCollection(tableName).find().into(new ArrayList<>()).stream().map(row -> row.getString("value")).collect(Collectors.toList());

        for(int i = 0; i < joiningTables.size(); i++) {
            int j = listOfFields.indexOf(table2Fields.get(i));
            if(listOfTypes.get(j).equals("int")) {
                HashMap<Integer, String> hash = new HashMap<>();
                int index = j - numberOfFields.get(i);
                database.getCollection(joiningTables.get(i)).find().into(new ArrayList<>()).forEach(doc -> {
                    String value = doc.getString("value");
                    hash.put(Integer.parseInt(value.split("#")[index]), value);
                });
                int index2 = listOfFields.indexOf(table1Fields.get(i));
                data = data.stream().map(row -> row.concat(hash.get(Integer.parseInt(row.split("#")[index2])))).collect(Collectors.toList());
            } else {
                HashMap<String, String> hash = new HashMap<>();
                int index = j - numberOfFields.get(i);
                database.getCollection(joiningTables.get(i)).find().into(new ArrayList<>()).forEach(doc -> {
                    String value = doc.getString("value");
                    hash.put(value.split("#")[index], value);
                });
                int index2 = listOfFields.indexOf(table1Fields.get(i));
                data = data.stream().map(row -> row.concat(hash.get(row.split("#")[index2]))).collect(Collectors.toList());
            }
        }

        if(!conditions.equals(" ")) {
            data = applyingConditions(structure, fields.toString(), conditions, data);
        }

        return aggregation(listOfFields, listOfTypes, data, groupby, aggregate);
    }

    public static void main(String[] args) {
        Server s = new Server();
        //System.out.println(s.selectWithHashJoin("University", "Diakok", " ", " ", " ", "Diakok.OsztalyId#Diakok.Nev#", "Atlag/avg/Diakok.OsztalyId"));
    }
}
