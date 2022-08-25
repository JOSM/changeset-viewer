// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.changeset.util.CellRenderer;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetBeen;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetController;
import org.openstreetmap.josm.plugins.changeset.util.Config;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The dialog to choose what changeset to show
 * @author ruben
 */
public final class ChangesetDialog extends ToggleDialog implements ActionListener {
    private Future<?> buttonUpdater;

    private final MapView mv = MainApplication.getMap().mapView;
    private final JButton jButtonNext = new JButton(tr("Next ->"));
    private final JButton jButtonprevious = new JButton(tr("<- Previous"));
    private final JTextField jTextFieldChangesetId;
    private final ListCellRenderer<ChangesetBeen> renderer = new CellRenderer();
    private final JComboBox<ChangesetBeen> jComboBox = new JComboBox<>();
    private boolean flag = true;

    /**
     * Create a new {@link ChangesetDialog} object
     */
    public ChangesetDialog() {
        super(tr("Changeset viewer"), "changeset", tr("Open changeset Viewer window."),
                Shortcut.registerShortcut("Tool:changeset-viewer", tr("Toggle: {0}", tr("Tool:changeset-Viewer")),
                        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), 120);

        JPanel jPanelProjects = new JPanel(new GridLayout(4, 1));
        jPanelProjects.setBorder(BorderFactory.createTitledBorder(""));
        JButton jButtonGetChangesets = new JButton(tr("Get changeset in the area"));
        jPanelProjects.add(jButtonGetChangesets);
        jButtonprevious.setEnabled(false);
        jButtonNext.setEnabled(false);

        jButtonGetChangesets.addActionListener((ActionEvent e) -> {
            flag = false;
            Config.setPAGE(1);
            //Get area
            Bounds bounds = mv.getRealBounds();
            String bbox = bounds.getMinLon() + "," + bounds.getMinLat() + "," + bounds.getMaxLon() + "," + bounds.getMaxLat();
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
        JPanel jPanelOptions = new JPanel(new GridLayout(1, 2));
        jPanelOptions.add(jButtonprevious);
        jPanelOptions.add(jButtonNext);
        jPanelProjects.add(jPanelOptions);
        jTextFieldChangesetId = new JTextField("55006771");
        jPanelProjects.add(jTextFieldChangesetId);
        JPanel jContentPanel = new JPanel(new GridLayout(1, 1));
        jContentPanel.add(jPanelProjects);
        SideButton displayChangesetButton = new SideButton(new AbstractAction() {
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
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fill a changeset id!"));
                }
            }
        });
        SideButton openChangesetweb = new SideButton(new AbstractAction() {
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
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fill a changeset id!"));
                }
            }
        });
        createLayout(jContentPanel, false, Arrays.asList(displayChangesetButton, openChangesetweb));
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

    private synchronized void getChangesets() {
        if (this.buttonUpdater != null) {
            this.buttonUpdater.cancel(true);
        }
        jComboBox.removeAllItems();
        jComboBox.setEnabled(false);
        CompletableFuture<ChangesetBeen[]> future = CompletableFuture.supplyAsync(ChangesetController::getListChangeset, MainApplication.worker);
        future.thenAccept(changesetBeens -> GuiHelper.runInEDT(() -> {
            jComboBox.setEnabled(true);
            for (ChangesetBeen changesetBeen : changesetBeens) {
                if (changesetBeen != null) {
                    jComboBox.addItem(changesetBeen);
                }
            }
            jComboBox.setRenderer(renderer);
        }));
        this.buttonUpdater = future;
    }

    /**
     * Show how a changeset modified OSM
     * @param changesetId The id to show
     */
    public static void printMap(String changesetId) {
        BoundedChangesetDataSet boundedDataSet = ChangesetController.getChangeset(changesetId);
        if (boundedDataSet == null) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), 
                    tr("Check the right changeset Id, if it is ok, maybe the changeset was not processed yet, try again in few minutes!"));
        } else {
            Changeset.work(boundedDataSet, changesetId);
        }
    }
}
