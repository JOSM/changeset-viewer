
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.josm.plugins.changeset.util.DataSetBuilder.BoundedDataSet;

/**
 *
 * @author ruben
 */
public class ChangesetController {
    public BoundedDataSet getChangeset(String changesetId) {
        DataSetBuilder builder = new DataSetBuilder();
        try {
            String url=Config.HOST + changesetId + ".json";
            Util.print(url);
            String stringChangeset = Request.sendGET(url);
            Util.print(stringChangeset);
            return builder.build(stringChangeset);
        } catch (IOException ex) {
            Logger.getLogger(ChangesetController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
