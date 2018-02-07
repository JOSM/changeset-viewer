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
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 *
 * @author ruben
 */
public class ChangesetLayer extends Layer implements ActionListener {

    BoundedChangesetDataSet dataSet;

    //DataSet dataset;
    float width;

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
        //Print the objetcs
        final float dash1[] = {10.0f};
        for (OsmPrimitive primitive : data.allPrimitives()) {
//            g.setColor(new Color(254, 30, 123));
            g.setStroke(new BasicStroke((float) 2f));
            if (primitive instanceof Way) {
                Way way = (Way) primitive;
                if (way.getInterestingTags().get("action").equals("create")) {
                    g.setColor(new Color(50, 214, 184));
                } else if (way.getInterestingTags().get("action").equals("delete")) {
                    g.setColor(new Color(197, 38, 63));
                } else if (way.getInterestingTags().get("action").equals("modify-old")) {
                    g.setColor(new Color(214, 138, 13));
                } else if (way.getInterestingTags().get("action").equals("modify-new")) {
                    g.setColor(new Color(229, 228, 61));
                } else if (way.getInterestingTags().get("action").equals("modify-new-rel")) {
                    g.setColor(new Color(229, 228, 61));
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.CAP_ROUND,
                            10.0f, dash1, 0.0f));
                } else if (way.getInterestingTags().get("action").equals("modify-old-rel")) {
                    g.setColor(new Color(214, 138, 13));
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.CAP_ROUND,
                            10.0f, dash1, 0.0f));
                } else if (way.getInterestingTags().get("action").equals("create-rel")) {
                    g.setColor(new Color(50, 214, 184));
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.CAP_ROUND,
                            10.0f, dash1, 0.0f));
                }else if (way.getInterestingTags().get("action").equals("delete-rel")) {
                    g.setColor(new Color(197, 38, 63));
                    g.setStroke(new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.CAP_ROUND,
                            10.0f, dash1, 0.0f));
                }
                List<Node> nodes = way.getNodes();
                if (nodes.size() < 2) {
                    return;
                }
                for (int i = 0; i <= way.getNodes().size() - 2; i++) {
                    Node node1 = way.getNode(i);
                    Node node2 = way.getNode(i + 1);
                    Point pnt1 = mv.getPoint(node1.getCoor());
                    Point pnt2 = mv.getPoint(node2.getCoor());
                    g.draw(new Line2D.Double(pnt1.x, pnt1.y, pnt2.x, pnt2.y));
                }
            } else if (primitive instanceof Node) {
                Node node = (Node) primitive;
                if (node.getParentWays().isEmpty()) {
                    if (node.getInterestingTags().get("action").equals("create")) {
                        g.setColor(new Color(50, 214, 184));
                    } else if (node.getInterestingTags().get("action").equals("delete")) {
                        g.setColor(new Color(197, 38, 63));
                    } else if (node.getInterestingTags().get("action").equals("modify-old")) {
                        g.setColor(new Color(214, 138, 13));
                    } else if (node.getInterestingTags().get("action").equals("modify-new")) {
                        g.setColor(new Color(229, 228, 61));
                    }

                    Point pnt = mv.getPoint(node.getCoor());
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

    }

}
