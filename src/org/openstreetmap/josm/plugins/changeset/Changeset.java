package org.openstreetmap.josm.plugins.changeset;

import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.io.UploadDialog;
import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.plugins.changeset.util.DataSetBuilderChangesets.BoundedDataSetChangestes;
import org.openstreetmap.josm.tools.Logging;

/**
 *
 * @author ruben
 */
public class Changeset {

    MapView mv = null;
    ChangesetLayer tofixLayer = new ChangesetLayer(tr("Changeset:<id>"));

    public void work(BoundedDataSetChangestes data, String changesetId) {
        try {
            tofixLayer.setName(tr("Changeset: " + changesetId));
            ChangesetDraw.draw(tofixLayer, data);
        } catch (final Exception e) {
            Logging.error("Error while reading json file!");
            Logging.error(e);
        }
        UploadDialog.getUploadDialog().getChangeset().getCommentsCount();
    }

}
