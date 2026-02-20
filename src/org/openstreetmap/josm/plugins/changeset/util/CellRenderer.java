// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/**
 * The cell renderer for {@link ChangesetBeen}
 * @author ruben
 */
public class CellRenderer implements ListCellRenderer<ChangesetBeen> {

    private static final Color COLOR_CREATE = new Color(50, 214, 184);
    private static final Color COLOR_MODIFY = new Color(229, 228, 61);
    private static final Color COLOR_DELETE = new Color(197, 38, 63);
    private static final Color COLOR_DATE = new Color(0, 174, 255);
    private static final Color COLOR_SELECTED_BG = new Color(51, 102, 153);
    private static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 11);

    @Override
    public Component getListCellRendererComponent(JList<? extends ChangesetBeen> list, ChangesetBeen changesetBeen, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        if (changesetBeen != null) {
            // Left: ID + user
            JLabel idLabel = new JLabel("#" + changesetBeen.getChangesetId() + "  ");
            idLabel.setFont(FONT_BOLD);

            JLabel userLabel = new JLabel(changesetBeen.getUser());
            userLabel.setFont(FONT_NORMAL);

            JPanel leftPanel = new JPanel(new BorderLayout(4, 0));
            leftPanel.setOpaque(false);
            leftPanel.add(idLabel, BorderLayout.WEST);
            leftPanel.add(userLabel, BorderLayout.CENTER);

            // Center: date
            String dateStr = formatDate(changesetBeen.getDate());
            JLabel dateLabel = new JLabel(dateStr, SwingConstants.CENTER);
            dateLabel.setForeground(COLOR_DATE);
            dateLabel.setFont(FONT_NORMAL);

            // Right: C / M / D stats
            JPanel statsPanel = new JPanel(new GridLayout(1, 3, 8, 0));
            statsPanel.setOpaque(false);

            JLabel createLabel = new JLabel(String.valueOf(changesetBeen.getCreate()), SwingConstants.CENTER);
            createLabel.setForeground(COLOR_CREATE);
            createLabel.setFont(FONT_BOLD);

            JLabel modifyLabel = new JLabel(String.valueOf(changesetBeen.getModify()), SwingConstants.CENTER);
            modifyLabel.setForeground(COLOR_MODIFY);
            modifyLabel.setFont(FONT_BOLD);

            JLabel deleteLabel = new JLabel(String.valueOf(changesetBeen.getDelete()), SwingConstants.CENTER);
            deleteLabel.setForeground(COLOR_DELETE);
            deleteLabel.setFont(FONT_BOLD);

            statsPanel.add(createLabel);
            statsPanel.add(modifyLabel);
            statsPanel.add(deleteLabel);

            row.add(leftPanel, BorderLayout.WEST);
            row.add(dateLabel, BorderLayout.CENTER);
            row.add(statsPanel, BorderLayout.EAST);
        }

        if (isSelected) {
            row.setBackground(COLOR_SELECTED_BG);
            row.setOpaque(true);
        } else {
            row.setOpaque(true);
            row.setBackground(list.getBackground());
        }

        return row;
    }

    private static String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        // "2023-04-21T10:52:43Z" -> "2023-04-21 10:52"
        return date.replace("T", " ").replaceAll(":\\d{2}Z$", "").replaceAll(":\\d{2}$", "");
    }
}
