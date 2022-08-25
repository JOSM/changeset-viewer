// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        Stroke stroke = g.getStroke();
        if (data == null) {
            return;
        }
        //Print the objects
        final float[] dash1 = {10.0f};
        final BasicStroke defaultStroke = new BasicStroke(2f);
        for (Way way : data.searchWays(bounds.toBBox())) {
            g.setStroke(defaultStroke);
            final String action = way.get("action");
            paintWay(g, mv, dash1, way, action);
        }
        for (Node node : data.searchNodes(bounds.toBBox())) {
            g.setStroke(defaultStroke);
            final String action = node.get("action");
            paintNode(g, mv, node, action);
        }
        g.setStroke(stroke);
    }

    private static void paintWay(Graphics2D g, MapView mv, float[] dash1, Way way, String action) {
        switch (action) {
            case "create":
                g.setColor(CREATED_COLOR.get());
                break;
            case "delete":
                g.setColor(DELETED_COLOR.get());
                break;
            case "modify-old":
                g.setColor(MODIFIED_OLD.get());
                break;
            case "modify-new":
                g.setColor(MODIFIED_NEW.get());
                break;
            case "modify-new-rel":
            case "modify-old-rel":
            case "create-rel":
            case "delete-rel":
                setRelationColor(g, action);
                g.setStroke(new BasicStroke(1.0f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND,
                        10.0f, dash1, 0.0f));
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
        List<Node> nodes = way.getNodes();
        if (nodes.size() < 2) {
            return;
        }
        // We cannot use MapViewPath
        Point previous = null;
        for (Node node : way.getNodes()) {
            final boolean latLonKnown = node.isLatLonKnown();
            if (previous == null && latLonKnown) {
                previous = mv.getPoint(node);
                continue;
            } else if (previous != null && !latLonKnown) {
                previous = null;
                continue;
            }
            if (latLonKnown) {
                Point point = mv.getPoint(node);
                g.drawLine(previous.x, previous.y, point.x, point.y);
                previous = point;
            }
        }
    }

    private static void setRelationColor(Graphics2D g, String action) {
        switch (action) {
            case "modify-new-rel":
                g.setColor(MODIFIED_NEW.get());
                break;
            case "modify-old-rel":
                g.setColor(MODIFIED_OLD.get());
                break;
            case "create-rel":
                g.setColor(CREATED_COLOR.get());
                break;
            case "delete-rel":
                g.setColor(DELETED_COLOR.get());
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private static void paintNode(Graphics2D g, MapView mv, Node node, String action) {
        if (!node.referrers(Way.class).findAny().isPresent()) {
            switch (action) {
                case "create":
                    g.setColor(CREATED_COLOR.get());
                    break;
                case "delete":
                    g.setColor(DELETED_COLOR.get());
                    break;
                case "modify-old":
                    g.setColor(MODIFIED_OLD.get());
                    break;
                case "modify-new":
                    g.setColor(MODIFIED_NEW.get());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }

            Point pnt = mv.getPoint(node);
            g.fillOval(pnt.x, pnt.y, 7, 7);
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
