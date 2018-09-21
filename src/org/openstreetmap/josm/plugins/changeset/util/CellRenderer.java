package org.openstreetmap.josm.plugins.changeset.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/**
 *
 * @author ruben
 */
public class CellRenderer implements ListCellRenderer<ChangesetBeen> {

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList<? extends ChangesetBeen> list, ChangesetBeen changesetBeen, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel jPanelChangeset = new JPanel(new GridLayout(2, 1, 8, 8));
        if (changesetBeen != null) {
            JPanel jPanelRow = new JPanel(new GridLayout(1, 3, 10, 10));
            JPanel jPanelNumChanges = new JPanel(new GridLayout(1, 2, 10, 10));
            jPanelNumChanges.setBackground(Color.BLACK);
            JLabel jLabelDate = new JLabel(changesetBeen.getDate(), SwingConstants.CENTER);
            jLabelDate.setForeground(new Color(0, 174, 255));
    //      JLabel jLabelChangesetId = new JLabel(String.valueOf(changesetBeen.getChangesetId()));
            jPanelChangeset.setBorder(BorderFactory.createTitledBorder("Changeset :" + String.valueOf(changesetBeen.getChangesetId())));
            JLabel jLabelUser = new JLabel(changesetBeen.getUser(), SwingConstants.CENTER);
            JLabel jLabelCreate = new JLabel(String.valueOf(changesetBeen.getCreate()), SwingConstants.CENTER);
            jLabelCreate.setForeground(new Color(50, 214, 184));
            jLabelCreate.setFont(new Font("Serif", Font.BOLD, 12));
            JLabel jLabelModify = new JLabel(String.valueOf(changesetBeen.getModify()), SwingConstants.CENTER);
            jLabelModify.setForeground(new Color(229, 228, 61));
            jLabelModify.setFont(new Font("Serif", Font.BOLD, 12));
            JLabel jLabelDelete = new JLabel(String.valueOf(changesetBeen.getDelete()), SwingConstants.CENTER);
            jLabelDelete.setForeground(new Color(197, 38, 63));
            jLabelDelete.setFont(new Font("Serif", Font.BOLD, 12));
    //      jPanelRow.add(jLabelChangesetId);
            jPanelRow.add(jLabelUser);
            jPanelNumChanges.add(jLabelCreate);
            jPanelNumChanges.add(jLabelModify);
            jPanelNumChanges.add(jLabelDelete);
            jPanelRow.add(jPanelNumChanges);
            //last
            jPanelChangeset.add(jLabelDate);
            jPanelChangeset.add(jPanelRow);
        }
        return jPanelChangeset;
    }
}
