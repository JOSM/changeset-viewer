// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer to show what a changeset did
 * @author ruben
 */
public class ChangesetLayer extends Layer implements ActionListener {
    private static final CachingProperty<Color> DELETED_COLOR = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            marktr("changeset-viewer"), marktr("Deleted objects"), new Color(197, 38, 63)).cached();
    private static final CachingProperty<Color> CREATED_COLOR = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            marktr("changeset-viewer"), marktr("Created objects"), new Color(50, 214, 184)).cached();
    private static final CachingProperty<Color> MODIFIED_OLD = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            marktr("changeset-viewer"), marktr("Modified objects (old)"), new Color(214, 138, 13)).cached();
    private static final CachingProperty<Color> MODIFIED_NEW = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            marktr("changeset-viewer"), marktr("Modified objects (new)"), new Color(229, 228, 61)).cached();

    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(2f);
    private static final BasicStroke DASHED_STROKE = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{10.0f}, 0.0f);

    BoundedChangesetDataSet dataSet;

    /**
     * Create a new {@link ChangesetLayer}
     * @param name The name of the layer
     */
    public ChangesetLayer(String name) {
        super(name);
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("changeset_layer");
    }

    @Override
    public String getToolTipText() {
        return tr("Layer to draw OSM error");
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    /**
     * Set the dataset for this layer
     * @param dataSet The dataset to show the user
     */
    public void setDataSet(BoundedChangesetDataSet dataSet) {
        this.dataSet = dataSet;
        invalidate();
    }

    @Override
    public void paint(Graphics2D g, final MapView mv, Bounds bounds) {
        DataSet data = dataSet.getDataSet();
        if (data == null) {
            return;
        }
        Stroke originalStroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Resolve colors once per paint call
        Color createColor = CREATED_COLOR.get();
        Color deleteColor = DELETED_COLOR.get();
        Color modOldColor = MODIFIED_OLD.get();
        Color modNewColor = MODIFIED_NEW.get();

        // Batch ways by action into GeneralPaths
        GeneralPath createPath = new GeneralPath();
        GeneralPath deletePath = new GeneralPath();
        GeneralPath modOldPath = new GeneralPath();
        GeneralPath modNewPath = new GeneralPath();
        GeneralPath createRelPath = new GeneralPath();
        GeneralPath deleteRelPath = new GeneralPath();
        GeneralPath modOldRelPath = new GeneralPath();
        GeneralPath modNewRelPath = new GeneralPath();

        Collection<Way> ways = data.searchWays(bounds.toBBox());
        for (Way way : ways) {
            String action = way.get("action");
            if (action == null) continue;
            List<Node> nodes = way.getNodes();
            if (nodes.size() < 2) continue;

            GeneralPath target;
            switch (action) {
                case "create": target = createPath; break;
                case "delete": target = deletePath; break;
                case "modify-old": target = modOldPath; break;
                case "modify-new": target = modNewPath; break;
                case "create-rel": target = createRelPath; break;
                case "delete-rel": target = deleteRelPath; break;
                case "modify-old-rel": target = modOldRelPath; break;
                case "modify-new-rel": target = modNewRelPath; break;
                default: continue;
            }
            appendWayToPath(target, mv, nodes);
        }

        // Draw all solid ways
        g.setStroke(DEFAULT_STROKE);
        drawPath(g, createPath, createColor);
        drawPath(g, deletePath, deleteColor);
        drawPath(g, modOldPath, modOldColor);
        drawPath(g, modNewPath, modNewColor);

        // Draw all relation (dashed) ways
        g.setStroke(DASHED_STROKE);
        drawPath(g, createRelPath, createColor);
        drawPath(g, deleteRelPath, deleteColor);
        drawPath(g, modOldRelPath, modOldColor);
        drawPath(g, modNewRelPath, modNewColor);

        // Draw standalone nodes
        g.setStroke(DEFAULT_STROKE);
        for (Node node : data.searchNodes(bounds.toBBox())) {
            if (node.referrers(Way.class).findAny().isPresent()) {
                continue;
            }
            String action = node.get("action");
            if (action == null) continue;
            Color c;
            switch (action) {
                case "create": c = createColor; break;
                case "delete": c = deleteColor; break;
                case "modify-old": c = modOldColor; break;
                case "modify-new": c = modNewColor; break;
                default: continue;
            }
            g.setColor(c);
            Point pnt = mv.getPoint(node);
            g.fillOval(pnt.x - 3, pnt.y - 3, 7, 7);
        }

        g.setStroke(originalStroke);
    }

    private static void appendWayToPath(GeneralPath path, MapView mv, List<Node> nodes) {
        boolean started = false;
        for (Node node : nodes) {
            if (!node.isLatLonKnown()) {
                started = false;
                continue;
            }
            Point p = mv.getPoint(node);
            if (!started) {
                path.moveTo(p.x, p.y);
                started = true;
            } else {
                path.lineTo(p.x, p.y);
            }
        }
    }

    private static void drawPath(Graphics2D g, GeneralPath path, Color color) {
        if (path.getCurrentPoint() != null) {
            g.setColor(color);
            g.draw(path);
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        // nothing to do here
    }

    @Override
    public Object getInfoComponent() {
        return getToolTipText();
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[]{
            LayerListDialog.getInstance().createShowHideLayerAction(),
            SeparatorLayerAction.INSTANCE,
            SeparatorLayerAction.INSTANCE,
            new LayerListPopup.InfoAction(this)};
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showConfirmDialog(null, e.getSource());
    }

    @Override
    public void mergeFrom(Layer layer) {
        throw new UnsupportedOperationException("Layer merge is not supported");
    }
}
