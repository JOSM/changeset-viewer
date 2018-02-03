package org.openstreetmap.josm.plugins.changeset.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 *
 * @author ruben
 */
public class CellRenderer implements ListCellRenderer {

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel jPanelRow = new JPanel(new GridLayout(1, 3));
        JPanel jPanelNumChanges = new JPanel(new GridLayout(1, 3, 5, 5));
        jPanelNumChanges.setBackground(Color.BLACK);
        if (value instanceof ChangesetBeen) {
            ChangesetBeen changesetBeen = (ChangesetBeen) value;
//            JLabel jLabelData = new JLabel(changesetBeen.getDate());
            JLabel jLabelChangesetId = new JLabel(String.valueOf(changesetBeen.getChangesetId()));
            JLabel jLabelUser = new JLabel(changesetBeen.getUser());
            JLabel jLabelCreate = new JLabel(String.valueOf(changesetBeen.getCreate()));
            jLabelCreate.setForeground(new Color(50, 214, 184));
            jLabelCreate.setFont(new Font("Serif", Font.BOLD, 12));
            JLabel jLabelModify = new JLabel(String.valueOf(changesetBeen.getModify()));
            jLabelModify.setForeground(new Color(229, 228, 61));
            jLabelModify.setFont(new Font("Serif", Font.BOLD, 12));
            JLabel jLabelDelete = new JLabel(String.valueOf(changesetBeen.getDelete()));
            jLabelDelete.setForeground(new Color(197, 38, 63));
            jLabelDelete.setFont(new Font("Serif", Font.BOLD, 12));
//            jPanelRow.add(jLabelData);
            jPanelRow.add(jLabelChangesetId);
            jPanelRow.add(jLabelUser);
            jPanelNumChanges.add(jLabelCreate);
            jPanelNumChanges.add(jLabelModify);
            jPanelNumChanges.add(jLabelDelete);
            jPanelRow.add(jPanelNumChanges);
        }
        return jPanelRow;
    }
}
