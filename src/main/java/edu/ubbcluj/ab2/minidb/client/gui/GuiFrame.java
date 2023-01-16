package edu.ubbcluj.ab2.minidb.client.gui;

import edu.ubbcluj.ab2.minidb.client.controller.ServerController;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GuiFrame extends JFrame {
    private final ServerController serverController;
    private final MainPanel mainPanel;
    private final DbPanel dbPanel;
    private final OpPanel opPanel;
    private final FormPanel formPanel;
    private final ConsolePanel consolePanel;

    public GuiFrame(ServerController serverController) {
        this.serverController = serverController;

        opPanel = new OpPanel();
        dbPanel = new DbPanel(serverController, opPanel);
        consolePanel = new ConsolePanel();
        formPanel = new FormPanel(serverController, consolePanel, dbPanel, opPanel);
        opPanel.setFormPanel(formPanel);

        mainPanel = new MainPanel(dbPanel, opPanel, formPanel, consolePanel);
        setContentPane(mainPanel);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                serverController.closeConnection();
                e.getWindow().dispose();
            }
        });

        setTitle("Team LGTM");
        setBounds(0, 0, 1024, 768);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }
}
