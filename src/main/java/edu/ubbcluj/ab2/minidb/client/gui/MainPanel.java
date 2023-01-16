package edu.ubbcluj.ab2.minidb.client.gui;

import javax.swing.*;
import java.awt.*;

public class MainPanel extends JPanel implements GridBagManager {
    private final DbPanel dbPanel;
    private final OpPanel opPanel;
    private final FormPanel formPanel;
    private final ConsolePanel consolePanel;

    private final GridBagConstraints gbc;

    public MainPanel(DbPanel dbPanel, OpPanel opPanel, FormPanel formPanel, ConsolePanel consolePanel) {
        this.dbPanel = dbPanel;
        this.opPanel = opPanel;
        this.formPanel = formPanel;
        JScrollPane formPane = new JScrollPane(formPanel); // formPanel to be scrollable
        this.consolePanel = consolePanel;

        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        addComp(this, dbPanel, gbc, 0, 0, 1, 1, GridBagConstraints.BOTH, 0.2, 0.7);
        addComp(this, opPanel, gbc, 1, 0, 1, 1, GridBagConstraints.BOTH, 0.2, 0.7);
        addComp(this, formPane, gbc, 2, 0, 1, 1, GridBagConstraints.BOTH, 0.6, 0.7);
        addComp(this, consolePanel, gbc, 0, 1, 3, 2, GridBagConstraints.BOTH, 1.0, 0.3);
    }
}
