package edu.ubbcluj.ab2.minidb.client;
import edu.ubbcluj.ab2.minidb.client.controller.ServerController;
import edu.ubbcluj.ab2.minidb.client.gui.GuiFrame;

import javax.swing.*;
import javax.swing.UIManager.*;

public class Main {
    public static void main(String[] args) {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
        ServerController serverController = new ServerController();

        GuiFrame guiFrame = new GuiFrame(serverController);
    }
}
