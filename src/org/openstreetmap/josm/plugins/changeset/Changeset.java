// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;

/**
 * A class to draw a changeset layer
 * @author ruben
 */
public final class Changeset {
    private Changeset() {
        // Hide constructor
    }

    /**
     * Do the work to show the data
     * @param data The data to show
     * @param changesetId The changeset id to show
     */
    public static void work(BoundedChangesetDataSet data, String changesetId) {
        ChangesetLayer tofixLayer = new ChangesetLayer(tr("Changeset: {0}", changesetId));
        ChangesetDraw.draw(tofixLayer, data);
    }

}
