package org.openstreetmap.josm.plugins.changeset;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
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
import org.openstreetmap.josm.plugins.changeset.util.DataSetBuilderChangesets.BoundedDataSetChangestes;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author ruben
 */
public final class ChangesetDialog extends ToggleDialog implements ActionListener {

    MapView mv = MainApplication.getMap().mapView;
    JPanel jContentPanel = new JPanel(new GridLayout(1, 1));
    JPanel jPanelProjects = new JPanel(new GridLayout(3, 1));
    JButton jButtonGetChangesets = new JButton(tr("Get changeset in the area"));
    private final SideButton getChangesetButton;
    private final JTextField jTextFieldChangesetId;
    ChangesetController changesetController = new ChangesetController();
    Changeset Changeset = new Changeset();
    ListCellRenderer renderer = new CellRenderer();
    JComboBox jComboBox = new JComboBox();

    public ChangesetDialog() {
        super(tr("Changeset-map"), "changeset", tr("Open changeset-map window."),
                Shortcut.registerShortcut("Tool:Changeset-map", tr("Toggle: {0}", tr("Tool:Changeset-map")),
                        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), 100);
        jPanelProjects.add(jButtonGetChangesets);
        jButtonGetChangesets.addActionListener((ActionEvent e) -> {
            //Get area
            Bounds bounds = mv.getRealBounds();
            String bbox = String.valueOf(bounds.getMinLon()) + "," + String.valueOf(bounds.getMinLat()) + "," + String.valueOf(bounds.getMaxLon()) + "," + String.valueOf(bounds.getMaxLat());
            Config.setBBOX(bbox);
            //remove item from combox
            jComboBox.removeAllItems();
            Object[] elements = changesetController.getListChangeset();
            for (Object element : elements) {
                jComboBox.addItem(element);
            }
            jComboBox.setRenderer(renderer);
        });
        jComboBox.addActionListener(this);
        jPanelProjects.add(jComboBox);
        jTextFieldChangesetId = new JTextField("55982280");
        jPanelProjects.add(jTextFieldChangesetId);
        jContentPanel.add(jPanelProjects);
        getChangesetButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Get Changeset"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Get Changeset"));
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
        createLayout(jContentPanel, false, Arrays.asList(new SideButton[]{getChangesetButton}));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChangesetBeen ch = (ChangesetBeen) jComboBox.getSelectedItem();
        if (ch != null) {
            jTextFieldChangesetId.setText(String.valueOf(ch.getChangesetId()));
            printMap(String.valueOf(ch.getChangesetId()));
        }
    }

    public void printMap(String ChangesetId) {
        BoundedDataSetChangestes boundedDataSet = changesetController.getChangeset(ChangesetId);
        if (boundedDataSet == null) {
            JOptionPane.showMessageDialog(Main.parent, tr("Check the right changeset Id, if it is ok, maybe the changeset was not processed yet, try again in few minutes!"));
        } else {
            Changeset.work(boundedDataSet, ChangesetId);;
        }
    }
}
