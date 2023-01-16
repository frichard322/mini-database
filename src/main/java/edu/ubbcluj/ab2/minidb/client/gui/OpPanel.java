package edu.ubbcluj.ab2.minidb.client.gui;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public class OpPanel extends JPanel implements GridBagManager, ActionListener, ListSelectionListener {
    private FormPanel formPanel;
    private final JLabel typeSelectorLabel;
    private final JComboBox<String> typeSelector;
    private final JLabel cmdSelectorLabel;
    private final JList<String> cmdSelector;

    public OpPanel() {
        setLayout(new GridBagLayout());

        // Do not change order
        cmdSelectorLabel = new JLabel("Select Command:");
        cmdSelector = new JList<>();
//        cmdSelector.setBackground(new Color(214, 217, 223));
        cmdSelector.setDragEnabled(false);
        cmdSelector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cmdSelector.addListSelectionListener(this);

        typeSelectorLabel = new JLabel("Select Type:");
        typeSelector = new JComboBox<>();
        typeSelector.addItem("DDL");
        typeSelector.addItem("DML");
        typeSelector.addActionListener(this);
        typeSelector.setSelectedIndex(0); // refresh appropriate command list at start

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        addComp(this, typeSelectorLabel, gbc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, typeSelector, gbc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, cmdSelectorLabel, gbc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.01);
        addComp(this, cmdSelector, gbc,0, 3, 1, 1, GridBagConstraints.BOTH, 1.0, 0.97);

        setBackground(new Color(214, 217, 223));
//        setBackground(Color.LIGHT_GRAY);
    }

    public void clearCommandSelection() {
        cmdSelector.clearSelection();
        if(formPanel != null) {
            formPanel.removeAll();
            formPanel.revalidate();
            formPanel.repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(typeSelector)) {
            String selectedItem = Objects.requireNonNull(typeSelector.getSelectedItem()).toString();

            if ("DDL".equals(selectedItem)) {
                DefaultListModel<String> model = new DefaultListModel<>();
                model.addElement("CREATE DATABASE");
                model.addElement("DROP DATABASE");
                model.addElement("CREATE TABLE");
                model.addElement("DROP TABLE");
                model.addElement("CREATE INDEX");

                cmdSelector.setModel(model);
            } else if ("DML".equals(selectedItem)) {
                DefaultListModel<String> model = new DefaultListModel<>();
                model.addElement("SELECT");
                model.addElement("SELECT WITH JOIN");
                model.addElement("INSERT");
                model.addElement("DELETE");

                cmdSelector.setModel(model);
            }
            // Remove content of formPanel when switch between command types like DDL, DML
            if(formPanel != null) {
                formPanel.removeAll();
                formPanel.revalidate();
                formPanel.repaint();
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (cmdSelector.getSelectedValue() != null && !e.getValueIsAdjusting()) {
            switch (cmdSelector.getSelectedValue()) {
                case "CREATE DATABASE" -> formPanel.showDatabase(0);
                case "DROP DATABASE" -> formPanel.showDatabase(1);
                case "CREATE TABLE" -> formPanel.showCreateTable();
                case "DROP TABLE" -> formPanel.showDropTable();
                case "CREATE INDEX" -> formPanel.showCreateIndex();
                case "SELECT" -> formPanel.showSelect();
                case "SELECT WITH JOIN" -> formPanel.showSelectWithJoin();
                case "INSERT" -> formPanel.showInsertRow();
                case "DELETE" -> formPanel.showDeleteRow();
                default -> System.out.println("This command in cmdSelector is not implemented yet.");
            }
        }
    }

    public void setFormPanel(FormPanel formPanel) {
        this.formPanel = formPanel;
    }
}
