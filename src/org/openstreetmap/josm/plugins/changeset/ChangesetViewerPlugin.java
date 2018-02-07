package org.openstreetmap.josm.plugins.changeset;

import java.awt.GraphicsEnvironment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 *
 * @author ruben
 */
public class ChangesetViewerPlugin extends Plugin {

    public ChangesetViewerPlugin(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null && !GraphicsEnvironment.isHeadless()) {
            newFrame.addToggleDialog(new ChangesetDialog());
        }
    }
}
