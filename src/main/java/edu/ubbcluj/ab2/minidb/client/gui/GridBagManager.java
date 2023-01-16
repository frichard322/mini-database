package edu.ubbcluj.ab2.minidb.client.gui;

import javax.swing.*;
import java.awt.*;

public interface GridBagManager {
    default void addComp(JPanel where, JComponent comp, GridBagConstraints gbc
            , int x, int y, int gWidth
            , int gHeight, int fill
            , double weightx, double weighty) {

        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = gWidth;
        gbc.gridheight = gHeight;
        gbc.fill = fill;
        gbc.weightx = weightx;
        gbc.weighty = weighty;

        where.add(comp, gbc);
    }
}
