package org.openstreetmap.josm.plugins.changeset;

import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;

/**
 *
 * @author ruben
 */
public class ChangesetDraw {

    public static void draw(final ChangesetLayer tofixNewLayer, BoundedChangesetDataSet data) {
        if (data == null) {
            return;
        }
        BoundingXYVisitor v = new BoundingXYVisitor();
        v.visit(data.getBounds());
        MainApplication.getMap().mapView.zoomTo(v);
        if (!MainApplication.getLayerManager().containsLayer(tofixNewLayer)) {
            MainApplication.getLayerManager().addLayer(tofixNewLayer);
        }
        tofixNewLayer.setDataSet(data);
    }
}
