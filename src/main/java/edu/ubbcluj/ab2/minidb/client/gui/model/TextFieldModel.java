package edu.ubbcluj.ab2.minidb.client.gui.model;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class TextFieldModel extends JTextField implements FocusListener {
    private final String defaultText;

    public TextFieldModel(String text) {
        super(text);
        defaultText = text;

        addFocusListener(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (getText().equals(defaultText)) {
            setText("");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (getText().equals("")) {
            setText(defaultText);
        }
    }
}
