package org.openstreetmap.josm.plugins.changeset;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetBeen;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetController;
import org.openstreetmap.josm.plugins.changeset.util.CellRenderer;
import org.openstreetmap.josm.plugins.changeset.util.Config;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author ruben
 */
public final class ChangesetDialog extends ToggleDialog implements ActionListener {

    private MapView mv = MainApplication.getMap().mapView;
    private final JPanel jContentPanel = new JPanel(new GridLayout(1, 1));
    private final JPanel jPanelProjects = new JPanel(new GridLayout(4, 1));
//    private final JPanel jPanelButtons = new JPanel(new GridLayout(3, 1));
    private final JPanel jPanelOptions = new JPanel(new GridLayout(1, 2));
    private final JButton jButtonGetChangesets = new JButton(tr("Get changeset in the area"));
    private final JButton jButtonNext = new JButton(tr("Next ->"));
    private final JButton jButtonprevious = new JButton(tr("<- Previous"));
    private final SideButton displayChangesetButton;
    private final SideButton OpenChangesetweb;
    private final JTextField jTextFieldChangesetId;
    private final ChangesetController changesetController = new ChangesetController();
    Changeset Changeset = new Changeset();
    private final ListCellRenderer renderer = new CellRenderer();
    private final JComboBox jComboBox = new JComboBox();
    private boolean flag = true;

    public ChangesetDialog() {
        super(tr("Changeset viewer"), "changeset", tr("Open changeset Viewer window."),
                Shortcut.registerShortcut("Tool:changeset-viewer", tr("Toggle: {0}", tr("Tool:changeset-Viewer")),
                        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), 120);

        jPanelProjects.setBorder(BorderFactory.createTitledBorder(""));
        jPanelProjects.add(jButtonGetChangesets);
        jButtonprevious.setEnabled(false);
        jButtonNext.setEnabled(false);

        jButtonGetChangesets.addActionListener((ActionEvent e) -> {
            flag = false;
            Config.setPAGE(1);
            //Get area
            Bounds bounds = mv.getRealBounds();
            String bbox = String.valueOf(bounds.getMinLon()) + "," + String.valueOf(bounds.getMinLat()) + "," + String.valueOf(bounds.getMaxLon()) + "," + String.valueOf(bounds.getMaxLat());
            Config.setBBOX(bbox);
            getChangesets();
            jButtonNext.setEnabled(true);
        });

        jButtonprevious.addActionListener((ActionEvent e) -> {
            flag = false;
            if (Config.getPAGE() > 1) {
                Config.setPAGE(Config.getPAGE() - 1);
                getChangesets();
            }
        });

        jButtonNext.addActionListener((ActionEvent e) -> {
            jButtonprevious.setEnabled(true);
            flag = false;
            Config.setPAGE(Config.getPAGE() + 1);
            getChangesets();
        });

        jComboBox.addActionListener(this);
        jPanelProjects.add(jComboBox);
        jPanelOptions.add(jButtonprevious);
        jPanelOptions.add(jButtonNext);
        jPanelProjects.add(jPanelOptions);
        jTextFieldChangesetId = new JTextField("55006771");
        jPanelProjects.add(jTextFieldChangesetId);
        jContentPanel.add(jPanelProjects);
        displayChangesetButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Display changeset"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Display changeset"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jTextFieldChangesetId.getText().isEmpty()) {
                    printMap(jTextFieldChangesetId.getText());
                } else {
                    JOptionPane.showMessageDialog(Main.parent, tr("Fill a changeset id!"));
                }
            }
        });
        OpenChangesetweb = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Open in OSM"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Open in OSM"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jTextFieldChangesetId.getText().isEmpty()) {
                    OpenBrowser.displayUrl(Config.OSMCHANGESET + jTextFieldChangesetId.getText());
                } else {
                    JOptionPane.showMessageDialog(Main.parent, tr("Fill a changeset id!"));
                }
            }
        });
        createLayout(jContentPanel, false, Arrays.asList(new SideButton[]{displayChangesetButton, OpenChangesetweb}));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChangesetBeen ch = (ChangesetBeen) jComboBox.getSelectedItem();
        if (ch != null && flag) {
            jTextFieldChangesetId.setText(String.valueOf(ch.getChangesetId()));
            printMap(String.valueOf(ch.getChangesetId()));
        }
        flag = true;
    }

    private void getChangesets() {
        jComboBox.removeAllItems();
        Object[] elements = changesetController.getListChangeset();
        for (Object element : elements) {
            jComboBox.addItem(element);
        }
        jComboBox.setRenderer(renderer);
    }

    public void printMap(String ChangesetId) {
        BoundedChangesetDataSet boundedDataSet = changesetController.getChangeset(ChangesetId);
        if (boundedDataSet == null) {
            JOptionPane.showMessageDialog(Main.parent, tr("Check the right changeset Id, if it is ok, maybe the changeset was not processed yet, try again in few minutes!"));
        } else {
            Changeset.work(boundedDataSet, ChangesetId);;
        }
    }
}
