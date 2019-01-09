package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.Logging;

/**
 *
 * @author ruben
 */
public class Changeset {

    public void work(BoundedChangesetDataSet data, String changesetId) {
        try {
            ChangesetLayer tofixLayer = new ChangesetLayer(tr("Changeset: " + changesetId));
            ChangesetDraw.draw(tofixLayer, data);
        } catch (final Exception e) {
            Logging.error("Error while reading json file!");
            Logging.error(e);
        }
    }

}
