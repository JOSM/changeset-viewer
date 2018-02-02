package org.openstreetmap.josm.plugins.changeset;

import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.plugins.changeset.util.ChangesetController;
import org.openstreetmap.josm.plugins.changeset.util.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author ruben
 */
public final class ChangesetDialog extends ToggleDialog implements ActionListener {

    MapView mv = MainApplication.getMap().mapView;
    JTabbedPane TabbedPanel = new javax.swing.JTabbedPane();
    JPanel jContentPanelProjects = new JPanel(new GridLayout(2, 1));
    JPanel jContenActivation = new JPanel(new GridLayout(6, 1));
    JPanel jPanelProjects = new JPanel(new GridLayout(1, 1));
    JPanel jPanelQuery = new JPanel(new GridLayout(2, 1));
    private final JLabel JlabelTitleProject;
    private final SideButton getChangesetButton;
    private final JTextField jTextFieldChangesetId;
    ChangesetController changesetController = new ChangesetController();
    Changeset tofixProject = new Changeset();

    public ChangesetDialog() {
        super(tr("Changeset-map"), "changeset", tr("Open changeset-map window."),
                Shortcut.registerShortcut("Tool:Changeset-map", tr("Toggle: {0}", tr("Tool:Changeset-map")),
                        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), 40);

        JlabelTitleProject = new javax.swing.JLabel();
        JlabelTitleProject.setText(tr("<html><a href=\"\">Changeset Id</a></html>"));
        JlabelTitleProject.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JlabelTitleProject.addMouseListener(
                new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e
            ) {
                OpenBrowser.displayUrl(Config.CHANGESET_MAP);
            }
        });

        jContentPanelProjects.add(JlabelTitleProject);
        jTextFieldChangesetId = new JTextField("55982280");
        jPanelProjects.add(jTextFieldChangesetId);
        jContentPanelProjects.add(jPanelProjects);

        getChangesetButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Get Changeset"));
                new ImageProvider("mapmode", "getchangeset").getResource().attachImageIcon(this, true);
                putValue(SHORT_DESCRIPTION, tr("Get Changeset"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                tofixProject.work(changesetController.getChangeset(jTextFieldChangesetId.getText()), jTextFieldChangesetId.getText());;
            }
        });
        createLayout(jContentPanelProjects, false, Arrays.asList(new SideButton[]{getChangesetButton}));

    }

    public void actionPerformed(ActionEvent e) {
    }

}
