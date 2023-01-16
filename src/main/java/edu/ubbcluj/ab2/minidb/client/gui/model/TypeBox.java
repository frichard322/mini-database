package edu.ubbcluj.ab2.minidb.client.gui.model;

import javax.swing.*;

public class TypeBox extends JComboBox<String> {
    public TypeBox() {
        addItem("int");
        addItem("float");
        addItem("string");
        addItem("date");
        addItem("datetime");
    }
}
