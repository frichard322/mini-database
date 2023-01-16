package edu.ubbcluj.ab2.minidb.client.gui;

import edu.ubbcluj.ab2.minidb.client.controller.ServerController;

import javax.swing.*;
import java.awt.*;

public class DbPanel extends JPanel implements GridBagManager{
    private final ServerController serverController;
    private final JLabel dbSelectorLabel;
    private final JComboBox<String> dbSelector;
    private final JLabel tableSelectorLabel;
    private final JList<String> tableSelector;
    private final OpPanel opPanel;
    private int dbCount;

    public DbPanel(ServerController serverController, OpPanel opPanel) {
        this.serverController = serverController;
        this.opPanel = opPanel;

        setLayout(new GridBagLayout());
        dbCount = 0;

        // Do not change order
        tableSelectorLabel = new JLabel("Select Table:");
        tableSelector = new JList<>();
        tableSelector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        dbSelectorLabel = new JLabel("Select DB:");
        dbSelector = new JComboBox<>();
        loadDatabases(); // into dbSelector
        dbSelector.addItemListener(e -> loadTables());
        tableSelector.addListSelectionListener(e -> this.opPanel.clearCommandSelection());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        addComp(this, dbSelectorLabel, gbc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, dbSelector, gbc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, tableSelectorLabel, gbc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, tableSelector, gbc,0, 3, 1, 1, GridBagConstraints.BOTH, 1.0, 0.97);

        setBackground(new Color(214, 217, 223));
//        setBackground(Color.LIGHT_GRAY);
    }

    public String getSelectedDatabase() {
        return (String) dbSelector.getSelectedItem();
    }

    public String getSelectedTable() {
        return tableSelector.getSelectedValue();
    }

    public DefaultComboBoxModel<String> getTableModel() {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        ListModel<String> listModel = tableSelector.getModel();
        for (int i = 0; i < listModel.getSize(); i++) {
            comboBoxModel.addElement(listModel.getElementAt(i));
        }

        return comboBoxModel;
    }

    public void loadDatabases() {
        dbCount = 0;
        dbSelector.removeAllItems();
        String[] dbList = serverController.getDatabases();

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        if (dbList != null && dbList.length > 0 ) {
            if(!dbList[0].equalsIgnoreCase("")) {
                dbCount = dbList.length;
                for (String db : dbList) {
                    model.addElement(db);
                }
                dbSelector.setModel(model);
                loadTables();
            }
        }
    }

    public void loadTables() {
        DefaultListModel<String> model = new DefaultListModel<>();
        if (dbCount > 0) {
            String selectedDb = dbSelector.getSelectedItem().toString();
            String[] tableList = serverController.getTables(selectedDb);

            if (tableList != null && tableList.length > 0) {
                if(!tableList[0].equalsIgnoreCase("")) {
                    for (String s : tableList) {
                        model.addElement(s);
                    }
                }
            }
        }
        tableSelector.setModel(model);
    }

    public void clearTableSelection() {
        tableSelector.clearSelection();
    }

    public int getDbCount() {
        return dbCount;
    }
}
