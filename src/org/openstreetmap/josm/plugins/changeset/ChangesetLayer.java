// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer to show what a changeset did
 * @author ruben
 */
public class ChangesetLayer extends Layer implements ActionListener {
    private static final CachingProperty<Color> DELETED_COLOR = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            "org.openstreetmap.josm.plugins.changeset.ChangesetLayer", tr("changeset-viewer: Deleted objects"),
            new Color(197, 38, 63)).cached();
    private static final CachingProperty<Color> CREATED_COLOR = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            "org.openstreetmap.josm.plugins.changeset.ChangesetLayer", tr("changeset-viewer: Created objects"),
            new Color(50, 214, 184)).cached();
    private static final CachingProperty<Color> MODIFIED_OLD = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            "org.openstreetmap.josm.plugins.changeset.ChangesetLayer", tr("changeset-viewer: Modified objects (old)"),
            new Color(214, 138, 13)).cached();
    private static final CachingProperty<Color> MODIFIED_NEW = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_GENERAL,
            "org.openstreetmap.josm.plugins.changeset.ChangesetLayer", tr("changeset-viewer: Modified objects (new)"),
            new Color(229, 228, 61)).cached();

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
        for (OsmPrimitive primitive : data.allPrimitives()) {
            g.setStroke(new BasicStroke(2f));
            Map<String, String> interestingTags = primitive.getInterestingTags();
            if (primitive instanceof Way) {
                Way way = (Way) primitive;
                if ("create".equals(interestingTags.get("action"))) {
                    g.setColor(CREATED_COLOR.get());
                } else if ("delete".equals(interestingTags.get("action"))) {
                    g.setColor(DELETED_COLOR.get());
                } else if ("modify-old".equals(interestingTags.get("action"))) {
                    g.setColor(MODIFIED_OLD.get());
                } else if ("modify-new".equals(interestingTags.get("action"))) {
                    g.setColor(MODIFIED_NEW.get());
                } else if ("modify-new-rel".equals(interestingTags.get("action"))) {
                    g.setColor(MODIFIED_NEW.get());
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND,
                            10.0f, dash1, 0.0f));
                } else if ("modify-old-rel".equals(interestingTags.get("action"))) {
                    g.setColor(MODIFIED_OLD.get());
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND,
                            10.0f, dash1, 0.0f));
                } else if ("create-rel".equals(interestingTags.get("action"))) {
                    g.setColor(CREATED_COLOR.get());
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND,
                            10.0f, dash1, 0.0f));
                }else if ("delete-rel".equals(interestingTags.get("action"))) {
                    g.setColor(DELETED_COLOR.get());
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND,
                            10.0f, dash1, 0.0f));
                }
                List<Node> nodes = way.getNodes();
                if (nodes.size() < 2) {
                    return;
                }
                for (int i = 0; i <= way.getNodes().size() - 2; i++) {
                    Node node1 = way.getNode(i);
                    Node node2 = way.getNode(i + 1);
                    Point pnt1 = mv.getPoint(node1);
                    Point pnt2 = mv.getPoint(node2);
                    g.draw(new Line2D.Double(pnt1.x, pnt1.y, pnt2.x, pnt2.y));
                }
            } else if (primitive instanceof Node) {
                Node node = (Node) primitive;
                if (node.getParentWays().isEmpty()) {
                    if ("create".equals(interestingTags.get("action"))) {
                        g.setColor(CREATED_COLOR.get());
                    } else if ("delete".equals(interestingTags.get("action"))) {
                        g.setColor(DELETED_COLOR.get());
                    } else if ("modify-old".equals(interestingTags.get("action"))) {
                        g.setColor(MODIFIED_OLD.get());
                    } else if ("modify-new".equals(interestingTags.get("action"))) {
                        g.setColor(MODIFIED_NEW.get());
                    }

                    Point pnt = mv.getPoint(node);
                    g.fillOval(pnt.x, pnt.y, 7, 7);
                }
            }
        }
        g.setStroke(stroke);
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
