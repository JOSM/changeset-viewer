// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import org.openstreetmap.josm.data.Bounds;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.plugins.changeset.util.CellRenderer;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetBeen;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetController;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetController.OsmchaResult;
import org.openstreetmap.josm.plugins.changeset.util.Config;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The dialog to choose what changeset to show
 * @author ruben
 */
public final class ChangesetDialog extends ToggleDialog {
    private Future<?> buttonUpdater;

    private final JosmTextField jTextFieldChangesetId;
    private final ListCellRenderer<ChangesetBeen> renderer = new CellRenderer();
    private final JProgressBar progressBar = new JProgressBar();
    private final JComboBox<ChangesetBeen> jComboBox = new JComboBox<>();
    private final JButton jButtonPrev = new JButton("<");
    private final JButton jButtonNext = new JButton(">");
    private final JLabel pageLabel = new JLabel("", SwingConstants.CENTER);

    private boolean updatingComboBox;
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalCount;
    private String currentBbox = "";

    /**
     * Create a new {@link ChangesetDialog} object
     */
    public ChangesetDialog() {
        super(tr("Changeset viewer"), "changeset", tr("Open changeset Viewer window."),
                Shortcut.registerShortcut("Tool:changeset-viewer", tr("Toggle: {0}", tr("Tool:changeset-Viewer")),
                        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), 120);

        JPanel jPanelProjects = new JPanel(new GridBagLayout());
        jPanelProjects.setBorder(BorderFactory.createTitledBorder(""));

        // Platform selector (OSM / OHM)
        JComboBox<Config.Platform> platformSelector = new JComboBox<>(Config.Platform.values());
        platformSelector.setSelectedItem(Config.getPlatform());
        platformSelector.addActionListener(e -> Config.setPlatform((Config.Platform) platformSelector.getSelectedItem()));
        jPanelProjects.add(platformSelector, GBC.eol().fill(GBC.HORIZONTAL));

        // "Get changesets from OSMCha" button
        JButton jButtonOsmcha = new JButton(tr("Get changesets in the area"));
        jPanelProjects.add(jButtonOsmcha, GBC.eol().fill(GBC.HORIZONTAL));

        // Progress bar
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        jPanelProjects.add(progressBar, GBC.eol().fill(GBC.HORIZONTAL));

        // Dropdown
        jComboBox.setRenderer(renderer);
        jPanelProjects.add(jComboBox, GBC.eol().fill(GBC.HORIZONTAL));

        // Pagination row: < Page x/y (total) >
        JPanel pageRow = new JPanel(new GridBagLayout());
        jButtonPrev.setEnabled(false);
        jButtonNext.setEnabled(false);
        pageRow.add(jButtonPrev, GBC.std());
        pageRow.add(pageLabel, GBC.std().fill(GBC.HORIZONTAL).weight(1, 0));
        pageRow.add(jButtonNext, GBC.eol());
        jPanelProjects.add(pageRow, GBC.eol().fill(GBC.HORIZONTAL));

        // Manual changeset ID field
        jTextFieldChangesetId = new JosmTextField();
        jTextFieldChangesetId.setHint("55006771");
        jPanelProjects.add(new JLabel(tr("Changeset number")), GBC.std());
        jPanelProjects.add(jTextFieldChangesetId, GBC.eol().fill(GBC.HORIZONTAL));

        // --- Listeners ---

        jButtonOsmcha.addActionListener(e -> {
            currentPage = 1;
            fetchOsmchaPage();
        });

        jButtonPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                fetchOsmchaPage();
            }
        });

        jButtonNext.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchOsmchaPage();
            }
        });

        jComboBox.addActionListener(e -> {
            if (!updatingComboBox) {
                ChangesetBeen ch = (ChangesetBeen) jComboBox.getSelectedItem();
                if (ch != null) {
                    jTextFieldChangesetId.setText(String.valueOf(ch.getChangesetId()));
                }
            }
        });

        // Side buttons
        SideButton displayChangesetButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Display changeset"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Display changeset"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jTextFieldChangesetId.getText().isEmpty()) {
                    loadAndShowChangeset(jTextFieldChangesetId.getText());
                } else {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fill a changeset id!"));
                }
            }
        });
        SideButton openChangesetweb = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Open in web"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Open in web"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jTextFieldChangesetId.getText().isEmpty()) {
                    OpenBrowser.displayUrl(Config.getChangesetWebUrl() + jTextFieldChangesetId.getText());
                } else {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fill a changeset id!"));
                }
            }
        });
        createLayout(jPanelProjects, false, Arrays.asList(displayChangesetButton, openChangesetweb));
    }

    private synchronized void fetchOsmchaPage() {
        if (this.buttonUpdater != null) {
            this.buttonUpdater.cancel(true);
        }
        if (currentPage == 1) {
            Bounds bounds = MainApplication.getMap().mapView.getRealBounds();
            currentBbox = bounds.getMinLon() + "," + bounds.getMinLat() + "," + bounds.getMaxLon() + "," + bounds.getMaxLat();
        }
        progressBar.setVisible(true);
        jComboBox.setVisible(false);
        updatingComboBox = true;
        jComboBox.removeAllItems();
        updatingComboBox = false;
        jComboBox.setEnabled(false);
        jButtonPrev.setEnabled(false);
        jButtonNext.setEnabled(false);
        this.buttonUpdater = MainApplication.worker.submit(() -> {
            try {
                OsmchaResult result = ChangesetController.fetchChangesetsFromOsmcha(currentBbox, currentPage);
                GuiHelper.runInEDT(() -> {
                    totalCount = result.getTotalCount();
                    totalPages = result.getTotalPages();
                    updatingComboBox = true;
                    jComboBox.removeAllItems();
                    for (ChangesetBeen cs : result.getChangesets()) {
                        jComboBox.addItem(cs);
                    }
                    updatingComboBox = false;
                    jComboBox.setEnabled(true);
                    progressBar.setVisible(false);
                    jComboBox.setVisible(true);
                    updatePageControls();
                });
            } finally {
                GuiHelper.runInEDT(() -> {
                    progressBar.setVisible(false);
                    jComboBox.setVisible(true);
                });
            }
        });
    }

    private void updatePageControls() {
        if (totalCount == 0) {
            pageLabel.setText(tr("No changesets found"));
            jButtonPrev.setEnabled(false);
            jButtonNext.setEnabled(false);
        } else if (totalPages <= 1) {
            pageLabel.setText(totalCount + " changesets");
            jButtonPrev.setEnabled(false);
            jButtonNext.setEnabled(false);
        } else {
            pageLabel.setText(tr("Page {0} / {1} ({2} total)", currentPage, totalPages, totalCount));
            jButtonPrev.setEnabled(currentPage > 1);
            jButtonNext.setEnabled(currentPage < totalPages);
        }
    }

    /**
     * Load and show a changeset with a progress indicator
     * @param changesetId The id to show
     */
    private void loadAndShowChangeset(String changesetId) {
        progressBar.setVisible(true);
        MainApplication.worker.submit(() -> {
            try {
                BoundedChangesetDataSet boundedDataSet = ChangesetController.getChangeset(changesetId);
                GuiHelper.runInEDT(() -> {
                    progressBar.setVisible(false);
                    if (boundedDataSet == null) {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                                tr("Check the right changeset Id, if it is ok, maybe the changeset was not processed yet, try again in few minutes!"));
                    } else {
                        Changeset.work(boundedDataSet, changesetId);
                    }
                });
            } catch (java.net.SocketTimeoutException ex) {
                Logging.warn("Timeout fetching changeset " + changesetId + ": " + ex.getMessage());
                GuiHelper.runInEDT(() -> {
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("The request timed out. The changeset may be too old or too large for the Overpass API. Try again later."));
                });
            } catch (IOException ex) {
                Logging.warn(ex.getMessage());
                GuiHelper.runInEDT(() -> {
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), ex.getMessage());
                });
            }
        });
    }
}
