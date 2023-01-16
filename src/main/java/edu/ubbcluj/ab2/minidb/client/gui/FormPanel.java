package edu.ubbcluj.ab2.minidb.client.gui;

import edu.ubbcluj.ab2.minidb.client.controller.ServerController;
import edu.ubbcluj.ab2.minidb.client.gui.model.TextFieldModel;
import edu.ubbcluj.ab2.minidb.client.gui.model.TypeBox;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FormPanel extends JPanel {
    private final ServerController serverController;
    private final DbPanel dbPanel;
    private final OpPanel opPanel;
    private final ConsolePanel consolePanel;
    private Pattern pattern;
    private Matcher matcher;

    public FormPanel(ServerController serverController, ConsolePanel consolePanel, DbPanel dbPanel, OpPanel opPanel) {
        this.serverController = serverController;
        this.consolePanel = consolePanel;
        this.dbPanel = dbPanel;
        this.opPanel = opPanel;

        setLayout(new BorderLayout());
        setBackground(new Color(214, 217, 223));
//        setBackground(Color.LIGHT_GRAY);
    }

    private void sendStatusMessage(int status) {
        switch (status) {
            case 0 -> consolePanel.displayText("Operation has been completed successfully.");
            case 1 -> consolePanel.displayText("Operation had no effect.");
            // If error occurred
            case -1 -> consolePanel.displayText("Operation caused an error.");
        }
    }

    // Show create and drop database too
    public void showDatabase(int op) {
        removeAll();

        if(op == 0 || dbPanel.getDbCount() > 0) {
            JTextField databaseField = new JTextField("Database name");
            JButton submitButton = new JButton("Submit");
            JPanel innerPane = new JPanel(new GridLayout(0, 2));
            innerPane.setBackground(this.getBackground());
            innerPane.add(databaseField);
            innerPane.add(submitButton);
            add(innerPane, BorderLayout.PAGE_START);

            databaseField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (databaseField.getText().equals("Database name")) {
                        databaseField.setText("");
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (databaseField.getText().equals("")) {
                        databaseField.setText("Database name");
                    }
                }
            });

            submitButton.addActionListener(e -> {
                pattern = Pattern.compile("^[A-Za-z_#]+[0-9]*$");
                matcher = pattern.matcher(databaseField.getText());
                int status = 1;
                if (matcher.matches()) {
                    if (op == 0) {
                        status = serverController.createDatabase(databaseField.getText());
                        sendStatusMessage(status);
                    } else {
                        status = serverController.dropDatabase(databaseField.getText());
                        sendStatusMessage(status);
                    }
                } else {
                    status = -1;
                    consolePanel.displayText("[ERROR] Wrong database name format.");
                }

                if(status == 0) {
                    dbPanel.loadDatabases();
                    opPanel.clearCommandSelection();
                }

                revalidate();
                repaint();
            });
        }

        revalidate();
        repaint();
    }

    public void showCreateTable() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null) {
            revalidate();
            repaint();
            return;
        }

        TextFieldModel tableField = new TextFieldModel("Table name");
        JButton addFieldButton = new JButton("Add field");
        JButton submitButton = new JButton("Submit");

        JPanel innerPane = new JPanel(new GridLayout(0, 8));
        innerPane.setBackground(this.getBackground());
        innerPane.add(tableField);
        innerPane.add(addFieldButton);
        innerPane.add(submitButton);
        // dummy panels
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        add(innerPane, BorderLayout.PAGE_START);

        ArrayList<JButton> deleteButtonList = new ArrayList<>();
        ArrayList<JTextField> fieldTextList = new ArrayList<>();
        ArrayList<TypeBox> typeList = new ArrayList<>();
        ArrayList<JCheckBox> primaryKeyList = new ArrayList<>();
        ArrayList<JCheckBox> foreignKeyList = new ArrayList<>();
        ArrayList<JCheckBox> uniqueList = new ArrayList<>();
        ArrayList<JComboBox<String>> foreignTableList = new ArrayList<>();
        ArrayList<JComboBox<String>> foreignFieldList = new ArrayList<>();

        addFieldButton.addActionListener(e -> {
            deleteButtonList.add(new JButton("x"));
            deleteButtonList.get(deleteButtonList.size()-1).setBackground(Color.LIGHT_GRAY);
            fieldTextList.add(new TextFieldModel("Field name"));
            typeList.add(new TypeBox());
            primaryKeyList.add(new JCheckBox("Primary"));
            foreignKeyList.add(new JCheckBox("Foreign"));
            uniqueList.add(new JCheckBox("Unique"));
            foreignTableList.add(new JComboBox<>());
            foreignFieldList.add(new JComboBox<>());

            foreignKeyList.get(foreignKeyList.size() - 1).addActionListener(e2 -> {
                JCheckBox tempCheckBox = (JCheckBox) e2.getSource();

                int indexOfE2 = foreignKeyList.indexOf(tempCheckBox);
                int indexReplace = (indexOfE2 + 2) * 8 - 2;
                if (tempCheckBox.isSelected() && dbPanel.getTableModel().getSize() > 0) {
                    innerPane.remove(indexReplace);
                    foreignTableList.get(indexOfE2).setModel(dbPanel.getTableModel());
                    innerPane.add(foreignTableList.get(indexOfE2), indexReplace);
                } else {
                    innerPane.remove(indexReplace);
                    innerPane.add(new JPanel(), indexReplace);
                }

                indexReplace = (indexOfE2 + 2) * 8 - 1;
                if (tempCheckBox.isSelected() && dbPanel.getTableModel().getSize() > 0) {
                    innerPane.remove(indexReplace);
                    innerPane.add(foreignFieldList.get(indexOfE2), indexReplace);

                    foreignTableList.get(indexOfE2).addActionListener(e3 -> {
                        String[] fieldsTable = serverController.getForeignFieldCandidates(dbPanel.getSelectedDatabase(), (String) foreignTableList.get(indexOfE2).getSelectedItem());
                        DefaultComboBoxModel<String> tableFieldsModel = new DefaultComboBoxModel<>();
                        Arrays.asList(fieldsTable).forEach(tableFieldsModel::addElement);
                        foreignFieldList.get(indexOfE2).setModel(tableFieldsModel);
                        revalidate();
                        repaint();
                    });
                    foreignTableList.get(indexOfE2).setSelectedIndex(0);
                } else {
                    innerPane.remove(indexReplace);
                    innerPane.add(new JPanel(), indexReplace);
                }

                revalidate();
                repaint();
            });

            deleteButtonList.get(deleteButtonList.size()-1).addActionListener(e2 -> {
                JButton tempButton = (JButton) e2.getSource();

                int indexOfE2 = deleteButtonList.indexOf(tempButton);
                deleteButtonList.remove(indexOfE2);
                fieldTextList.remove(indexOfE2);
                typeList.remove(indexOfE2);
                primaryKeyList.remove(indexOfE2);
                foreignKeyList.remove(indexOfE2);
                uniqueList.remove(indexOfE2);
                foreignTableList.remove(indexOfE2);
                foreignFieldList.remove(indexOfE2);

                // Keplet
                for (int i = 0; i < 8; i++) {
                    innerPane.remove((indexOfE2+1) * 8);
                }
                revalidate();
                repaint();
            });

            innerPane.add(deleteButtonList.get(deleteButtonList.size() - 1));
            innerPane.add(fieldTextList.get(fieldTextList.size() - 1));
            innerPane.add(typeList.get(typeList.size() - 1));
            innerPane.add(primaryKeyList.get(primaryKeyList.size() - 1));
            innerPane.add(uniqueList.get(uniqueList.size() - 1));
            innerPane.add(foreignKeyList.get(foreignKeyList.size() - 1));
            innerPane.add(new JPanel());
            innerPane.add(new JPanel());
            revalidate();
            repaint();
        });

        submitButton.addActionListener(e -> {
            if (fieldTextList.size() <= 0) {
                consolePanel.displayText("[ERROR] Table needs to have at least 1 field added.");
            } else {
                pattern = Pattern.compile("^[A-Za-z_#]+[0-9]*$");
                matcher = pattern.matcher(tableField.getText());

                if (matcher.matches()) {
                    StringBuilder fieldNames = new StringBuilder();
                    StringBuilder fieldTypes = new StringBuilder();
                    StringBuilder primaryKey = new StringBuilder();
                    StringBuilder foreignKey = new StringBuilder();
                    StringBuilder uniqueKey = new StringBuilder();

                    for (int i = 0; i < fieldTextList.size(); i++) {
                        fieldNames.append(fieldTextList.get(i).getText()).append("#");
                        fieldTypes.append(Objects.requireNonNull(typeList.get(i).getSelectedItem())).append("#");
                        if (primaryKeyList.get(i).isSelected()) {
                            primaryKey.append(fieldTextList.get(i).getText()).append("#");
                        }

                        if (foreignKeyList.get(i).isSelected()) {
                            foreignKey.append(fieldTextList.get(i).getText())
                                    .append("/")
                                    .append(foreignTableList.get(i).getSelectedItem())
                                    .append(".")
                                    .append(foreignFieldList.get(i).getSelectedItem())
                                    .append("#");
                        }

                        if (uniqueList.get(i).isSelected()) {
                            uniqueKey.append(fieldTextList.get(i).getText()).append("#");
                        }
                    }

                    if (primaryKey.length() == 0) {
                        primaryKey.append("Id#");
                        fieldNames.insert(0, "Id#");
                        fieldTypes.insert(0, "int#");
                    }

                    int status = serverController.createTable(dbPanel.getSelectedDatabase(), tableField.getText(), primaryKey.toString(), foreignKey.toString(), uniqueKey.toString(), fieldNames.toString(), fieldTypes.toString());
                    sendStatusMessage(status);

                    // Refresh table list
                    if (status == 0) {
                        dbPanel.loadTables();
                        opPanel.clearCommandSelection();
                    }
                } else {
                    consolePanel.displayText("[ERROR] Wrong table name format.");
                }
            }
        });

        revalidate();
        repaint();
    }

    public void showDropTable() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null || dbPanel.getSelectedTable() == null) {
            revalidate();
            repaint();
            return;
        }

        JButton submitButton = new JButton("Drop table");
        add(submitButton, BorderLayout.PAGE_START);

        submitButton.addActionListener(e -> {
            int status = serverController.dropTable(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable());
            sendStatusMessage(status);

            if (status == 0) {
                dbPanel.loadTables();
                opPanel.clearCommandSelection();
            }
        });

        revalidate();
        repaint();
    }

    public void showCreateIndex() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null || dbPanel.getSelectedTable() == null) {
            revalidate();
            repaint();
            return;
        }

        TextFieldModel indexName = new TextFieldModel("Index name");
        JButton submitButton = new JButton("Submit");
        JComboBox<String> fieldSelector = new JComboBox<>();
        // Get fields, then add them to the JComboBox

        String[] fieldsFromTable = serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable());
        for (String field : fieldsFromTable) {
            fieldSelector.addItem(field);
        }

        submitButton.addActionListener(e -> {
            pattern = Pattern.compile("^[A-Za-z_./#]+[0-9]*$");
            matcher = pattern.matcher(indexName.getText());
            int status = 1;
            if (matcher.matches()) {
                status = serverController.createIndex(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable(), (String) fieldSelector.getSelectedItem(), indexName.getText());
                sendStatusMessage(status);
            } else {
                consolePanel.displayText("[ERROR] Wrong index name format.");
            }
            if(status == 0){
                opPanel.clearCommandSelection();
            }
        });

        JPanel innerPane = new JPanel(new GridLayout(0, 3));
        innerPane.setBackground(this.getBackground());
        innerPane.add(indexName);
        innerPane.add(fieldSelector);
        innerPane.add(submitButton);
        add(innerPane, BorderLayout.PAGE_START);

        revalidate();
        repaint();
    }

    public void showSelect() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null) {
            revalidate();
            repaint();
            return;
        }

        // UI
        JPanel innerPane = new JPanel(new GridLayout(0, 5));
        innerPane.setBackground(this.getBackground());

        // SELECT BUTTON
        JPopupMenu selectMenu = new JPopupMenu();
        JButton selectButton = new JButton();
        selectButton.setAction(new AbstractAction("SELECT") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectMenu.show(selectButton, 0, selectButton.getHeight());
            }
        });

        innerPane.add(selectButton);
        JButton addWhereConditionButton = new JButton("Add where");
        innerPane.add(addWhereConditionButton);
        JButton submitButton = new JButton("Submit");
        innerPane.add(submitButton);
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());


        // WHERE ARRAYLISTS
        ArrayList<JButton> whereDeleteButtonList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereOperationList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereFirstFieldList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereComparatorList = new ArrayList<>();
        ArrayList<JTextField> whereSecondFieldListField = new ArrayList<>();
        ArrayList<JComboBox<String>> whereSecondFieldListCombo = new ArrayList<>();

        // FROM BUTTON
        ArrayList<String> selectMenuAllFieldNames = new ArrayList<>();
        ArrayList<String> fromMenuAllTableNames = new ArrayList<>();
        ArrayList<JCheckBoxMenuItem> selectMenuAllComponents = new ArrayList<>();
        String[] tableList = serverController.getTables(dbPanel.getSelectedDatabase());
        JPopupMenu fromMenu = new JPopupMenu();
        for (String table : tableList) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(table);
            item.addActionListener(e -> {
                AbstractButton aButton = (AbstractButton) e.getSource();
                if (aButton.getModel().isSelected()) {
                    fromMenuAllTableNames.add(aButton.getText());

                    String[] fieldNames = serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), aButton.getText());
                    for (String field : fieldNames) {
                        field = aButton.getText() + "." + field;
                        JCheckBoxMenuItem newItem = new JCheckBoxMenuItem(field);
                        selectMenu.add(newItem);
                        selectMenuAllFieldNames.add(field);
                        selectMenuAllComponents.add(newItem);
                    }

                    // Add new fieldnames to existing where JComboBoxes
                    whereFirstFieldList.forEach(combo -> {
                        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
                        for (String field : fieldNames) {
                            comboModel.addElement(aButton.getText() + "." + field);
                        }
                        combo.setModel(comboModel);
                    });
                    whereSecondFieldListCombo.forEach(combo -> {
                        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
                        for (String field : fieldNames) {
                            comboModel.addElement(aButton.getText() + "." + field);
                        }
                        combo.setModel(comboModel);
                    });
                } else {
                    fromMenuAllTableNames.remove(aButton.getText());

                    int index_to_delete = -1, number_of_deletions = 0;
                    for (String field : selectMenuAllFieldNames) {
                        if (field.contains(aButton.getText() + ".")) {
                            if (index_to_delete == -1)
                                index_to_delete = selectMenuAllFieldNames.indexOf(field);
                            number_of_deletions++;
                        }
                    }

                    for (int i = 0; i < number_of_deletions; i++) {
                        selectMenuAllFieldNames.remove(index_to_delete);
                        selectMenuAllComponents.remove(index_to_delete);
                        selectMenu.remove(index_to_delete);
                    }

                    // Remove fieldnames from existing where JComboBoxes
                    int finalIndex_to_delete = index_to_delete;
                    int finalNumber_of_deletions = number_of_deletions;
                    whereFirstFieldList.forEach(combo -> {
                        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
                        for (int j = 0; j < finalNumber_of_deletions; j++) {
                            comboModel.removeElementAt(finalIndex_to_delete);
                        }
                        combo.setModel(comboModel);
                    });
                    whereSecondFieldListCombo.forEach(combo -> {
                        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
                        for (int j = 0; j < finalNumber_of_deletions; j++) {
                            comboModel.removeElementAt(finalIndex_to_delete);
                        }
                        combo.setModel(comboModel);
                    });
                }
            });
            fromMenu.add(item);
        }
        JButton fromButton = new JButton();
        fromButton.setAction(new AbstractAction("FROM") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fromMenu.show(fromButton, 0, fromButton.getHeight());
            }
        });

        // WHERE
        addWhereConditionButton.addActionListener(e -> {
            whereDeleteButtonList.add(new JButton("x"));
            whereOperationList.add(new JComboBox<>());
            whereFirstFieldList.add(new JComboBox<>());
            whereComparatorList.add(new JComboBox<>());
            whereSecondFieldListField.add(new JTextField());
            whereSecondFieldListCombo.add(new JComboBox<>());

            String[] op = {"Field-Value", "Field-Field"};
            String[] comp = {"=", "<", ">", "<=", ">="};
            whereOperationList.get(whereOperationList.size()-1).setModel(new DefaultComboBoxModel<>(op));
            whereFirstFieldList.get(whereFirstFieldList.size()-1).setModel(new DefaultComboBoxModel<>(selectMenuAllFieldNames.toArray(new String[selectMenuAllFieldNames.size()])));
            whereComparatorList.get(whereComparatorList.size()-1).setModel(new DefaultComboBoxModel<>(comp));
            whereSecondFieldListCombo.get(whereSecondFieldListCombo.size()-1).setModel(new DefaultComboBoxModel<>(selectMenuAllFieldNames.toArray(new String[selectMenuAllFieldNames.size()])));

            whereDeleteButtonList.get(whereDeleteButtonList.size()-1).addActionListener(e2 -> {
                JButton tempButton = (JButton) e2.getSource();

                int myIndex = whereDeleteButtonList.indexOf(tempButton);
                whereDeleteButtonList.remove(myIndex);
                whereOperationList.remove(myIndex);
                whereFirstFieldList.remove(myIndex);
                whereComparatorList.remove(myIndex);
                whereSecondFieldListField.remove(myIndex);
                whereSecondFieldListCombo.remove(myIndex);

                // Keplet
                for (int i = 0; i < 5; i++) {
                    innerPane.remove((myIndex + 2) * 5);
                }
                revalidate();
                repaint();
            });

            whereOperationList.get(whereOperationList.size()-1).addActionListener(e2 -> {
                JComboBox combo = (JComboBox) e2.getSource();
                int myIndex = whereOperationList.indexOf(combo);
                switch (combo.getSelectedIndex()) {
                    case 0 -> {
                        innerPane.remove(whereSecondFieldListCombo.get(myIndex));
                        innerPane.add(whereSecondFieldListField.get(myIndex), (2 * 5) + myIndex * 5 + 4); // KEPLET
                    }
                    case 1 -> {
                        innerPane.remove(whereSecondFieldListField.get(myIndex));
                        innerPane.add(whereSecondFieldListCombo.get(myIndex), (2 * 5) + myIndex * 5 + 4); // KEPLET
                    }
                }
                revalidate();
                repaint();
            });

            innerPane.add(whereDeleteButtonList.get(whereDeleteButtonList.size()-1));
            innerPane.add(whereOperationList.get(whereOperationList.size()-1));
            innerPane.add(whereFirstFieldList.get(whereFirstFieldList.size()-1));
            innerPane.add(whereComparatorList.get(whereComparatorList.size()-1));
            innerPane.add(whereSecondFieldListField.get(whereSecondFieldListField.size()-1));

            revalidate();
            repaint();
        });

        submitButton.addActionListener(e -> {
            StringBuilder tableNames = new StringBuilder();
            StringBuilder fieldNames = new StringBuilder();
            StringBuilder conditions = new StringBuilder();

            fromMenuAllTableNames.forEach(s -> {
               tableNames.append(s).append("#");
            });
            if (tableNames.toString().equals("")) {
                return;
            }

            for (int i = 0; i < selectMenuAllComponents.size(); i++) {
                if (selectMenuAllComponents.get(i).isSelected()) {
                    fieldNames.append(selectMenuAllFieldNames.get(i)).append("#");
                }
            }
            if (fieldNames.toString().equals("")) {
                fieldNames.append(" ");
            }

            for (int i = 0; i < whereOperationList.size(); i++) {
                int selected_op = whereOperationList.get(i).getSelectedIndex();
                conditions.append(selected_op).append("/");
                conditions.append(whereFirstFieldList.get(i).getSelectedItem()).append("/");
                conditions.append(whereComparatorList.get(i).getSelectedItem()).append("/");
                if (selected_op == 0) {
                    conditions.append(whereSecondFieldListField.get(i).getText());
                } else if (selected_op == 1) {
                    conditions.append(whereSecondFieldListCombo.get(i).getSelectedItem());
                }
                conditions.append("#");
            }
            if (conditions.toString().equals("")) {
                conditions.append(" ");
            } else {
                // Validate the correctness of the fields
                List<String> fieldTypesList = new ArrayList<>();
                List<String> fieldNamesList = new ArrayList<>();
                Arrays.asList(tableNames.toString().split("#")).forEach(table -> {
                    fieldNamesList.addAll(Arrays.asList(serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), table)));
                    fieldTypesList.addAll(Arrays.asList(serverController.getFieldTypesFromTable(dbPanel.getSelectedDatabase(), table)));
                });

                String[] condsArray = conditions.toString().split("#");
                for (String str: condsArray) {
                    String[] partsOfCond = str.split("/");
                    if (partsOfCond[0].equals("0")) {
                        String currentType = fieldTypesList.get(fieldNamesList.indexOf(partsOfCond[1].split("\\.")[1]));
                        if (!checkRegex(partsOfCond[3], currentType)) {
                            consolePanel.displayText("[ERROR] Field types in where conditions don't match.");
                            return;
                        }
                    } else if (partsOfCond[0].equals("1")) {
                        int indexFirstField = fieldNamesList.indexOf(partsOfCond[1].split("\\.")[1]);
                        int indexSecondField = fieldNamesList.indexOf(partsOfCond[3].split("\\.")[1]);
                        if (!fieldTypesList.get(indexFirstField).equals(fieldTypesList.get(indexSecondField))) {
                            consolePanel.displayText("[ERROR] Field types in where conditions don't match.");
                            return;
                        }
                    }
                }
            }

            String result = serverController.selectFrom(dbPanel.getSelectedDatabase(), tableNames.toString(), fieldNames.toString(), conditions.toString());

            // Refresh table list
            if (result != null) {
                opPanel.clearCommandSelection();
                String[] tmp = result.split("\n", 2);
                consolePanel.displayData(tmp[0], tmp[1]);
            }
        });

        innerPane.add(fromButton);
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        innerPane.add(new JPanel());
        add(innerPane, BorderLayout.PAGE_START);

        revalidate();
        repaint();
    }

    public void showSelectWithJoin() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null) {
            revalidate();
            repaint();
            return;
        }

        // UI
        JPanel innerPane = new JPanel(new BorderLayout());
        innerPane.setBackground(this.getBackground());

        // SELECT BUTTON
        JPopupMenu selectMenu = new JPopupMenu();
        JButton selectButton = new JButton();
        selectButton.setAction(new AbstractAction("SELECT") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectMenu.show(selectButton, 0, selectButton.getHeight());
            }
        });

        // aggrFieldSelect
        JComboBox<String> aggrTableFieldComboBox = new JComboBox<>();

        // GROUP BY BUTTON
        JPopupMenu groupByMenu = new JPopupMenu();
        JButton groupByButton = new JButton();
        groupByButton.setAction(new AbstractAction("GROUP BY") {
            @Override
            public void actionPerformed(ActionEvent e) {
                groupByMenu.show(groupByButton, 0, groupByButton.getHeight());
            }
        });

        JPanel controlPanel = new JPanel(new GridLayout(0, 3));
        JPanel joinPanel = new JPanel(new GridLayout(0, 4));
        JPanel wherePanel = new JPanel(new GridLayout(0, 5));

        JButton addWhereButton = new JButton("Add where");
        JButton addJoinButton = new JButton("Add join");
        JButton submitButton = new JButton("Submit");
        // 1st row
        controlPanel.add(selectButton);
        controlPanel.add(addWhereButton);
        controlPanel.add(addJoinButton);

        // 2nd row
        controlPanel.add(groupByButton);
        controlPanel.add(submitButton);
        controlPanel.add(new JPanel()); // dummy

        // WHERE ARRAYLISTS
        ArrayList<JButton> whereDeleteButtonList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereOperationList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereFirstFieldList = new ArrayList<>();
        ArrayList<JComboBox<String>> whereComparatorList = new ArrayList<>();
        ArrayList<JTextField> whereSecondFieldListField = new ArrayList<>();
        ArrayList<JComboBox<String>> whereSecondFieldListCombo = new ArrayList<>();

        ArrayList<String> selectMenuAllFieldNames = new ArrayList<>(); // stores the current table fieldnames + joined tables fieldnames
        ArrayList<String> fromComboAllFieldNames = new ArrayList<>(); // stores the current table fieldnames
        // FROM
        JComboBox<String> fromComboBox = new JComboBox<>();
        fromComboBox.setModel(new DefaultComboBoxModel<>(serverController.getTables(dbPanel.getSelectedDatabase())));

        fromComboBox.addActionListener(e -> {
            String tableSelected = (String) fromComboBox.getSelectedItem();

            // Delete FROM previous fields from select JMenu
            fromComboAllFieldNames.forEach(selectMenuAllFieldNames::remove);
            fromComboAllFieldNames.clear();

            // Add new from fields to selectMenuAllFieldsNames
            Arrays.asList(serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), tableSelected)).forEach(field -> {
                String fieldToAdd = tableSelected + "." + field;
                selectMenuAllFieldNames.add(fieldToAdd);
                fromComboAllFieldNames.add(fieldToAdd);
            });

            // Reload the selectMenu elements
            selectMenu.removeAll();
            groupByMenu.removeAll();
            aggrTableFieldComboBox.removeAll();
            DefaultComboBoxModel<String> aggrNewModel = new DefaultComboBoxModel<>();
            selectMenuAllFieldNames.forEach(field -> {
                selectMenu.add(new JCheckBoxMenuItem(field));
                groupByMenu.add(new JCheckBoxMenuItem(field));
                aggrNewModel.addElement(field);
            });
            aggrTableFieldComboBox.setModel(aggrNewModel);
        });
        fromComboBox.setSelectedIndex(0); // trigger the actionlistener, so we load items into the select JMenu

        // JOIN
        // WHERE ARRAYLISTS
        ArrayList<JButton> joinDeleteButtonList = new ArrayList<>();
        ArrayList<JComboBox<String>> joinFirstFieldList = new ArrayList<>();
        ArrayList<JComboBox<String>> joinSecondTableList = new ArrayList<>();
        ArrayList<JComboBox<String>> joinSecondFieldList = new ArrayList<>();
        addJoinButton.addActionListener(e -> {
            joinDeleteButtonList.add(new JButton("x"));
            joinFirstFieldList.add(new JComboBox<>());
            joinSecondTableList.add(new JComboBox<>());
            joinSecondFieldList.add(new JComboBox<>());

            // Set models
            joinSecondTableList.get(joinSecondTableList.size()-1).setModel(new DefaultComboBoxModel<>(serverController.getTables(dbPanel.getSelectedDatabase())));

            if (joinFirstFieldList.size() == 1) {
                joinFirstFieldList.get(0).setModel(new DefaultComboBoxModel<>(fromComboAllFieldNames.toArray(new String[0])));

                fromComboBox.setEnabled(false);
            } else if (joinFirstFieldList.size() > 1) {
                if (joinSecondTableList.get(joinSecondTableList.size()-1).getSelectedItem() != null) {
                    DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>(fromComboAllFieldNames.toArray(new String[0]));

                    for (int i = 0; i < joinSecondFieldList.size()-1; i++) {
                        DefaultComboBoxModel<String> comboBoxModel = (DefaultComboBoxModel<String>) joinSecondFieldList.get(i).getModel();

                        for (int j = 0; j < comboBoxModel.getSize(); j++) {
                            newModel.addElement(comboBoxModel.getElementAt(j));
                        }
                    }

                    joinFirstFieldList.get(joinFirstFieldList.size()-1).setModel(newModel);
                }

                joinDeleteButtonList.get(joinDeleteButtonList.size()-2).setEnabled(false);
                joinFirstFieldList.get(joinFirstFieldList.size()-2).setEnabled(false);
                joinSecondTableList.get(joinSecondTableList.size()-2).setEnabled(false);
                joinSecondFieldList.get(joinSecondFieldList.size()-2).setEnabled(false);
            }

            joinSecondTableList.get(joinSecondTableList.size()-1).addActionListener(e2 -> {
                DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>();

                Arrays.asList(serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), (String) joinSecondTableList.get(joinSecondTableList.size()-1).getSelectedItem()))
                        .forEach(field -> {
                            newModel.addElement(joinSecondTableList.get(joinSecondTableList.size()-1).getSelectedItem() + "." + field);
                        });

                joinSecondFieldList.get(joinSecondFieldList.size()-1).setModel(newModel);

                // Refresh where comboboxes
                DefaultComboBoxModel<String> newCombo = new DefaultComboBoxModel<>();
                DefaultComboBoxModel<String> oldComboFirst = (DefaultComboBoxModel<String>) joinSecondFieldList.get(joinSecondFieldList.size()-1).getModel();
                DefaultComboBoxModel<String> oldComboSecond = (DefaultComboBoxModel<String>) joinFirstFieldList.get(joinFirstFieldList.size()-1).getModel();

                for (int i = 0; i < oldComboFirst.getSize(); i++) {
                    newCombo.addElement(oldComboFirst.getElementAt(i));
                }
                for (int i = 0; i < oldComboSecond.getSize(); i++) {
                    newCombo.addElement(oldComboSecond.getElementAt(i));
                }

                selectMenu.removeAll();
                groupByMenu.removeAll();
                aggrTableFieldComboBox.removeAll();
                DefaultComboBoxModel<String> aggrNewModel = new DefaultComboBoxModel<>();
                for (int i = 0; i < newCombo.getSize(); i++) {
                    selectMenu.add(new JCheckBoxMenuItem(newCombo.getElementAt(i)));
                    groupByMenu.add(new JCheckBoxMenuItem(newCombo.getElementAt(i)));
                    aggrNewModel.addElement(newCombo.getElementAt(i));
                }
                aggrTableFieldComboBox.setModel(aggrNewModel);

                whereFirstFieldList.forEach(comboBox -> {
                    comboBox.setModel(newCombo);
                });
                whereSecondFieldListCombo.forEach(comboBox -> {
                    comboBox.setModel(newCombo);
                });
                wherePanel.revalidate();
                wherePanel.repaint();
            });
            joinSecondTableList.get(joinSecondTableList.size()-1).setSelectedIndex(0); // trigger the AL above

            joinDeleteButtonList.get(joinDeleteButtonList.size()-1).addActionListener(e2 -> {
                JButton tempButton = (JButton) e2.getSource();
                int myIndex = joinDeleteButtonList.indexOf(tempButton);

                for (int i = 0; i < 4; i++) {
                    joinPanel.remove((myIndex + 1) * 4); // KEPLET
                }

                joinDeleteButtonList.remove(myIndex);
                joinFirstFieldList.remove(myIndex);
                joinSecondTableList.remove(myIndex);
                joinSecondFieldList.remove(myIndex);

                if (myIndex > 0) {
                    joinDeleteButtonList.get(myIndex-1).setEnabled(true);
                    joinFirstFieldList.get(myIndex-1).setEnabled(true);
                    joinSecondTableList.get(myIndex-1).setEnabled(true);
                    joinSecondFieldList.get(myIndex-1).setEnabled(true);
                } else if (myIndex == 0) {
                    fromComboBox.setEnabled(true);
                }

                if (joinSecondTableList.size() > 0) {
                    // Refresh where comboboxes
                    DefaultComboBoxModel<String> newCombo = new DefaultComboBoxModel<>();
                    DefaultComboBoxModel<String> oldComboFirst = (DefaultComboBoxModel<String>) joinSecondFieldList.get(joinSecondFieldList.size() - 1).getModel();
                    DefaultComboBoxModel<String> oldComboSecond = (DefaultComboBoxModel<String>) joinFirstFieldList.get(joinFirstFieldList.size() - 1).getModel();

                    for (int i = 0; i < oldComboFirst.getSize(); i++) {
                        newCombo.addElement(oldComboFirst.getElementAt(i));
                    }
                    for (int i = 0; i < oldComboSecond.getSize(); i++) {
                        newCombo.addElement(oldComboSecond.getElementAt(i));
                    }

                    selectMenu.removeAll();
                    groupByMenu.removeAll();
                    aggrTableFieldComboBox.removeAll();
                    DefaultComboBoxModel<String> aggrNewModel = new DefaultComboBoxModel<>();
                    for (int i = 0; i < newCombo.getSize(); i++) {
                        selectMenu.add(new JCheckBoxMenuItem(newCombo.getElementAt(i)));
                        groupByMenu.add(new JCheckBoxMenuItem(newCombo.getElementAt(i)));
                        aggrNewModel.addElement(newCombo.getElementAt(i));
                    }
                    aggrTableFieldComboBox.setModel(aggrNewModel);

                    whereFirstFieldList.forEach(comboBox -> {
                        comboBox.setModel(newCombo);
                    });
                    whereSecondFieldListCombo.forEach(comboBox -> {
                        comboBox.setModel(newCombo);
                    });
                    wherePanel.revalidate();
                    wherePanel.repaint();
                } else {
                    whereFirstFieldList.forEach(comboBox -> {
                        comboBox.setModel(new DefaultComboBoxModel<>(fromComboAllFieldNames.toArray(new String[0])));
                    });
                    whereSecondFieldListCombo.forEach(comboBox -> {
                        comboBox.setModel(new DefaultComboBoxModel<>(fromComboAllFieldNames.toArray(new String[0])));
                    });

                    selectMenu.removeAll();
                    groupByMenu.removeAll();
                    aggrTableFieldComboBox.removeAll();
                    DefaultComboBoxModel<String> aggrNewModel = new DefaultComboBoxModel<>();
                    fromComboAllFieldNames.forEach(field -> {
                        selectMenu.add(new JCheckBoxMenuItem(field));
                        groupByMenu.add(new JCheckBoxMenuItem(field));
                        aggrNewModel.addElement(field);
                    });
                    aggrTableFieldComboBox.setModel(aggrNewModel);
                }

                joinPanel.revalidate();
                joinPanel.repaint();
            });

            joinPanel.add(joinDeleteButtonList.get(joinDeleteButtonList.size()-1));
            joinPanel.add(joinSecondTableList.get(joinSecondTableList.size()-1));
            joinPanel.add(joinFirstFieldList.get(joinFirstFieldList.size()-1));
            joinPanel.add(joinSecondFieldList.get(joinSecondFieldList.size()-1));


            revalidate();
            repaint();
        });

        // WHERE
        addWhereButton.addActionListener(e -> {
            whereDeleteButtonList.add(new JButton("x"));
            whereOperationList.add(new JComboBox<>());
            whereFirstFieldList.add(new JComboBox<>());
            whereComparatorList.add(new JComboBox<>());
            whereSecondFieldListField.add(new JTextField());
            whereSecondFieldListCombo.add(new JComboBox<>());

            String[] op = {"Field-Value", "Field-Field"};
            String[] comp = {"=", "<", ">", "<=", ">="};
            whereOperationList.get(whereOperationList.size()-1).setModel(new DefaultComboBoxModel<>(op));
            whereFirstFieldList.get(whereFirstFieldList.size()-1).setModel(new DefaultComboBoxModel<>(selectMenuAllFieldNames.toArray(new String[selectMenuAllFieldNames.size()])));
            whereComparatorList.get(whereComparatorList.size()-1).setModel(new DefaultComboBoxModel<>(comp));
            whereSecondFieldListCombo.get(whereSecondFieldListCombo.size()-1).setModel(new DefaultComboBoxModel<>(selectMenuAllFieldNames.toArray(new String[selectMenuAllFieldNames.size()])));

            whereDeleteButtonList.get(whereDeleteButtonList.size()-1).addActionListener(e2 -> {
                JButton tempButton = (JButton) e2.getSource();

                int myIndex = whereDeleteButtonList.indexOf(tempButton);
                whereDeleteButtonList.remove(myIndex);
                whereOperationList.remove(myIndex);
                whereFirstFieldList.remove(myIndex);
                whereComparatorList.remove(myIndex);
                whereSecondFieldListField.remove(myIndex);
                whereSecondFieldListCombo.remove(myIndex);

                // Keplet
                for (int i = 0; i < 5; i++) {
                    wherePanel.remove((myIndex + 1) * 5);
                }
                wherePanel.revalidate();
                wherePanel.repaint();
            });

            whereOperationList.get(whereOperationList.size()-1).addActionListener(e2 -> {
                JComboBox combo = (JComboBox) e2.getSource();
                int myIndex = whereOperationList.indexOf(combo);
                switch (combo.getSelectedIndex()) {
                    case 0 -> {
                        wherePanel.remove(whereSecondFieldListCombo.get(myIndex));
                        wherePanel.add(whereSecondFieldListField.get(myIndex),  (myIndex+1) * 5 + 4); // KEPLET
                    }
                    case 1 -> {
                        wherePanel.remove(whereSecondFieldListField.get(myIndex));
                        wherePanel.add(whereSecondFieldListCombo.get(myIndex), (myIndex+1) * 5 + 4); // KEPLET
                    }
                }
                wherePanel.revalidate();
                wherePanel.repaint();
            });
            if (joinSecondFieldList.size() > 0) {
                // Refresh where comboboxes
                DefaultComboBoxModel<String> newCombo = new DefaultComboBoxModel<>();
                DefaultComboBoxModel<String> oldComboFirst = (DefaultComboBoxModel<String>) joinSecondFieldList.get(joinSecondFieldList.size() - 1).getModel();
                DefaultComboBoxModel<String> oldComboSecond = (DefaultComboBoxModel<String>) joinFirstFieldList.get(joinFirstFieldList.size() - 1).getModel();

                for (int i = 0; i < oldComboFirst.getSize(); i++) {
                    newCombo.addElement(oldComboFirst.getElementAt(i));
                }
                for (int i = 0; i < oldComboSecond.getSize(); i++) {
                    newCombo.addElement(oldComboSecond.getElementAt(i));
                }

                whereFirstFieldList.forEach(comboBox -> {
                    comboBox.setModel(newCombo);
                });
                whereSecondFieldListCombo.forEach(comboBox -> {
                    comboBox.setModel(newCombo);
                });
                wherePanel.revalidate();
                wherePanel.repaint();
            }

            wherePanel.add(whereDeleteButtonList.get(whereDeleteButtonList.size()-1));
            wherePanel.add(whereOperationList.get(whereOperationList.size()-1));
            wherePanel.add(whereFirstFieldList.get(whereFirstFieldList.size()-1));
            wherePanel.add(whereComparatorList.get(whereComparatorList.size()-1));
            wherePanel.add(whereSecondFieldListField.get(whereSecondFieldListField.size()-1));

            revalidate();
            repaint();
        });

        JLabel fromLabel = new JLabel("FROM: ");
        JPanel fromLabelPanel = new JPanel();
        fromLabelPanel.add(fromLabel);
        controlPanel.add(fromLabelPanel);
        controlPanel.add(fromComboBox);
        innerPane.add(controlPanel, BorderLayout.NORTH);

        JPanel tempPanel = new JPanel(new BorderLayout());
        // Aggr
        JPanel aggrPanel = new JPanel(new GridLayout(0, 3));
        JLabel aggrLabel = new JLabel("AGGR:");
        JPanel aggrLabelPanel = new JPanel();
        aggrLabelPanel.add(aggrLabel);
        aggrPanel.add(aggrLabelPanel);
        aggrPanel.add(new JPanel());
        aggrPanel.add(new JPanel());
        JTextField aggrNameTextField = new JTextField();
        aggrPanel.add(aggrNameTextField);
        JComboBox<String> aggrFunctionComboBox = new JComboBox<>();
        String[] aggrFunctionsArray = {"Select function", "min", "max", "avg", "sum", "count"};
        aggrFunctionComboBox.setModel(new DefaultComboBoxModel<>(aggrFunctionsArray));
        aggrPanel.add(aggrFunctionComboBox);
        aggrPanel.add(aggrTableFieldComboBox);

        tempPanel.add(aggrPanel, BorderLayout.NORTH);


        JLabel joinLabel = new JLabel("JOIN:");
        JPanel joinLabelPanel = new JPanel();
        joinLabelPanel.add(joinLabel);
        joinPanel.add(joinLabelPanel);
        for (int i = 0; i < 3; i++)
            joinPanel.add(new JPanel()); // dummy panels
        tempPanel.add(joinPanel, BorderLayout.CENTER);
        innerPane.add(tempPanel, BorderLayout.CENTER);

        JLabel whereLabel = new JLabel("WHERE:");
        JPanel whereLabelPanel = new JPanel();
        whereLabelPanel.add(whereLabel);
        wherePanel.add(whereLabelPanel);
        for (int i = 0; i < 4; i++)
            wherePanel.add(new JPanel()); // dummy panels
        innerPane.add(wherePanel, BorderLayout.SOUTH);

        add(innerPane, BorderLayout.PAGE_START);

        revalidate();
        repaint();

        submitButton.addActionListener(e -> {
            // 12. command
            StringBuilder tableNames = new StringBuilder();
            StringBuilder fieldNames = new StringBuilder();
            StringBuilder tableJoins = new StringBuilder();
            StringBuilder conditions = new StringBuilder();

            tableNames.append((String) fromComboBox.getSelectedItem());

            for (int i = 0; i < selectMenu.getComponentCount(); i++) {
                if (((JCheckBoxMenuItem) selectMenu.getComponent(i)).isSelected()) {
                    fieldNames.append(((JCheckBoxMenuItem) selectMenu.getComponent(i)).getText()).append("#");
                }
            }
            if (fieldNames.toString().equals("")) {
                fieldNames.append(" ");
            }

            for (int i = 0; i < joinDeleteButtonList.size(); i++) {
                tableJoins.append(joinSecondTableList.get(i).getSelectedItem()).append("/");
                tableJoins.append(joinFirstFieldList.get(i).getSelectedItem()).append("/");
                tableJoins.append(joinSecondFieldList.get(i).getSelectedItem()).append("/");
                tableJoins.append("#");
            }

            for (int i = 0; i < whereOperationList.size(); i++) {
                int selected_op = whereOperationList.get(i).getSelectedIndex();
                conditions.append(selected_op).append("/");
                conditions.append(whereFirstFieldList.get(i).getSelectedItem()).append("/");
                conditions.append(whereComparatorList.get(i).getSelectedItem()).append("/");
                if (selected_op == 0) {
                    conditions.append(whereSecondFieldListField.get(i).getText());
                } else if (selected_op == 1) {
                    conditions.append(whereSecondFieldListCombo.get(i).getSelectedItem());
                }
                conditions.append("#");
            }
            if (conditions.toString().equals("")) {
                conditions.append(" ");
            } else {
                // Validate the correctness of the fields
                List<String> fieldTypesList = new ArrayList<>();
                List<String> fieldNamesList = new ArrayList<>();
                Arrays.asList(tableNames.toString().split("#")).forEach(table -> {
                    fieldNamesList.addAll(Arrays.asList(serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), table)));
                    fieldTypesList.addAll(Arrays.asList(serverController.getFieldTypesFromTable(dbPanel.getSelectedDatabase(), table)));
                });

                String[] condsArray = conditions.toString().split("#");
                for (String str: condsArray) {
                    String[] partsOfCond = str.split("/");
                    if (partsOfCond[0].equals("0")) {
                        String currentType = fieldTypesList.get(fieldNamesList.indexOf(partsOfCond[1].split("\\.")[1]));
                        if (!checkRegex(partsOfCond[3], currentType)) {
                            consolePanel.displayText("[ERROR] Field types in where conditions don't match.");
                            return;
                        }
                    } else if (partsOfCond[0].equals("1")) {
                        int indexFirstField = fieldNamesList.indexOf(partsOfCond[1].split("\\.")[1]);
                        int indexSecondField = fieldNamesList.indexOf(partsOfCond[3].split("\\.")[1]);
                        if (!fieldTypesList.get(indexFirstField).equals(fieldTypesList.get(indexSecondField))) {
                            consolePanel.displayText("[ERROR] Field types in where conditions don't match.");
                            return;
                        }
                    }
                }
            }

            String result;
            if (aggrFunctionComboBox.getSelectedIndex() == 0) {
                result = serverController.selectFromWithJoin(dbPanel.getSelectedDatabase(), tableNames.toString(), fieldNames.toString(), tableJoins.toString(), conditions.toString());
            } else {
                StringBuilder groupByNames = new StringBuilder();

                for (int i = 0; i < groupByMenu.getComponentCount(); i++) {
                    if (((JCheckBoxMenuItem) groupByMenu.getComponent(i)).isSelected()) {
                        groupByNames.append(((JCheckBoxMenuItem) groupByMenu.getComponent(i)).getText()).append("#");
                    }
                }
                if (groupByNames.toString().equals("")) {
                    groupByNames.append(" ");
                }

                StringBuilder aggr = new StringBuilder(aggrNameTextField.getText())
                        .append("/")
                        .append(aggrFunctionComboBox.getSelectedItem())
                        .append("/")
                        .append(aggrTableFieldComboBox.getSelectedItem())
                        .append("/");

                result = serverController.selectFromWithGroupBy(dbPanel.getSelectedDatabase(), tableNames.toString(), fieldNames.toString(), tableJoins.toString(), conditions.toString(), groupByNames.toString(), aggr.toString());
            }

            // Refresh table list
            if (result != null) {
                opPanel.clearCommandSelection();
                String[] tmp = result.split("\n", 2);
                consolePanel.displayData(tmp[0], tmp[1]);
            }
        });
    }

    public void showInsertRow() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null || dbPanel.getSelectedTable() == null) {
            revalidate();
            repaint();
            return;
        }

        JPanel innerPane = new JPanel(new GridLayout(0, 1));
        innerPane.setBackground(this.getBackground());
        JButton submitButton = new JButton("Submit");

        String[] fieldsTable = serverController.getFieldsFromTable(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable());
        String[] fieldTypesTable = serverController.getFieldTypesFromTable(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable());
        List<String> keysTable = Arrays.asList(serverController.getKeysFromTable(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable(), "primaryKey"));

        ArrayList<TextFieldModel> fieldList = new ArrayList<>();
        for (String field : fieldsTable) {
            fieldList.add(new TextFieldModel(field));
            innerPane.add(fieldList.get(fieldList.size() - 1));
        }
        innerPane.add(submitButton);
        add(innerPane, BorderLayout.PAGE_START);

        submitButton.addActionListener(e -> {
            StringBuilder fieldsAsString = new StringBuilder();
            StringBuilder keysAsString = new StringBuilder();

            for (int i = 0; i < fieldList.size(); i++) {
                String fieldValue = fieldList.get(i).getText();
                if (!checkRegex(fieldValue, fieldTypesTable[i])) {
                    consolePanel.displayText("[ERROR] Incorrect text format at " + fieldsTable[i]);
                    return;
                } else {
                    fieldsAsString.append(fieldValue).append("#");

                    if (keysTable.contains(fieldsTable[i])) {
                        keysAsString.append(fieldValue).append("#");
                    }
                }
            }

            int status = serverController.insertRow(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable(), keysAsString.toString(), fieldsAsString.toString());
            sendStatusMessage(status);

            if(status == 0){
                opPanel.clearCommandSelection();
            }
        });

        revalidate();
        repaint();
    }

    public void showDeleteRow() {
        removeAll();

        if (dbPanel.getSelectedDatabase() == null || dbPanel.getSelectedTable() == null) {
            revalidate();
            repaint();
            return;
        }
        
        TextFieldModel keyName = new TextFieldModel("Key value");
        JButton submitButton = new JButton("Submit");

        submitButton.addActionListener(e -> {
            int status = serverController.deleteRow(dbPanel.getSelectedDatabase(), dbPanel.getSelectedTable(), keyName.getText() + "#");
            sendStatusMessage(status);
            if(status == 0){
                opPanel.clearCommandSelection();
            }
        });

        JPanel innerPane = new JPanel(new GridLayout(0, 2));
        innerPane.setBackground(this.getBackground());
        innerPane.add(keyName);
        innerPane.add(submitButton);
        add(innerPane, BorderLayout.PAGE_START);

        revalidate();
        repaint();
    }

    private boolean checkRegex(String checkText, String type) {
        switch (type) {
            case "int" -> pattern = Pattern.compile("^[0-9?]+$");
            case "float" -> pattern = Pattern.compile("^[0-9]+.[0-9]+$");
            case "string" -> pattern = Pattern.compile("^[A-Za-z0-9]+$");
            case "date" -> pattern = Pattern.compile("^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$");
            case "datetime" -> pattern = Pattern.compile("^(?=\\d)(?:(?:31(?!.(?:0?[2469]|11))|(?:30|29)(?!.0?2)|29(?=.0?2.(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00)))(?:\\x20|$))|(?:2[0-8]|1\\d|0?[1-9]))([-./])(?:1[012]|0?[1-9])\\1(?:1[6-9]|[2-9]\\d)?\\d\\d(?:(?=\\x20\\d)\\x20|$))?(((0?[1-9]|1[012])(:[0-5]\\d){0,2}(\\x20[AP]M))|([01]\\d|2[0-3])(:[0-5]\\d){1,2})?$");
        }

        matcher = pattern.matcher(checkText);
        return (matcher.matches());
    }
}
