package edu.ubbcluj.ab2.minidb.client.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class ConsolePanel extends JPanel {
    private final JTextArea consoleTextArea;
    private final JTabbedPane tabbedPane;
    private final JScrollPane textScroll;
    private final JScrollPane tableScroll;
    private final JPanel panel1;
    private final JPanel panel2;

    public ConsolePanel() {
        setLayout(new GridLayout(1, 1));
        tabbedPane = new JTabbedPane();

        panel1 = new JPanel();
        consoleTextArea = new JTextArea(1, 30);
        consoleTextArea.setEditable(false);
        consoleTextArea.setWrapStyleWord(true);
        panel1.setLayout(new GridLayout(1, 1));
        panel1.add(consoleTextArea);
        textScroll = new JScrollPane(panel1);
        textScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Text", null, textScroll,"View text");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        tableScroll = new JScrollPane(panel2);
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Table", null, tableScroll,"View table");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        add(tabbedPane);
    }

    public void displayText(String message) {
        consoleTextArea.setText(message);
        consoleTextArea.append("\n");
    }

    public void displayData(String fieldNames, String data) {
        String[] fieldNamesArray = fieldNames.split("\t");

        ArrayList<ArrayList<String>> dataArrayList = new ArrayList<>();

        Arrays.asList(data.split("\n")).forEach(row -> {
            dataArrayList.add(new ArrayList<>());

            Arrays.asList(row.split("\t")).forEach(column -> {
                dataArrayList.get(dataArrayList.size()-1).add(column);
            });
        });

        String[][] dataStringArray = dataArrayList.stream().map(u -> u.toArray(new String[0])).toArray(String[][]::new);
        JTable table = new JTable(dataStringArray, fieldNamesArray);

        panel2.removeAll();
        panel2.add(table.getTableHeader(), BorderLayout.PAGE_START);
        panel2.add(table, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
