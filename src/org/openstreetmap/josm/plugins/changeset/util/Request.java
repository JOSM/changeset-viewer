// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;

/**
 * A helper class to fetch content
 * @author ruben
 */
public final class Request {
    private static final StringProperty OSMCHA_AUTHORIZATION = new StringProperty("osmcha.authorization.key", null);
    private Request() {
        // Hide constructor
    }

    /**
     * Get the OSMCha authorization key
     * @return {@code true} if the user entered an authorization key
     */
    private static boolean getOsmChaAuthorization() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        String message = tr("Please enter your OSMCha API Key (see link)");
        String userUrl = Config.OSMCHA_HOST + "user";
        HtmlPanel htmlPanel = new HtmlPanel("<html><body style=\"width: 375px;\">" + message
                + "<br><a href=\"" + userUrl + "\">" + userUrl + "</a></body></html>");
        htmlPanel.enableClickableHyperlinks();
        jPanel.add(htmlPanel);
        JosmTextField textField = new JosmTextField();
        textField.setHint("Token <Token Value>");
        jPanel.add(textField);
        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(),
                tr("Missing OSMCha API Key"), tr("OK"), tr("Cancel"))
                .setContent(jPanel);
        int ret = ed.showDialog().getValue();
        if (ret == ExtendedDialog.DialogClosedOtherwise || ret == 2) {
            return false;
        }
        OSMCHA_AUTHORIZATION.put(textField.getText());
        return OSMCHA_AUTHORIZATION.isSet();
    }

    /**
     * Get a URL
     * @param url The url to GET
     * @return The result (or null)
     * @throws IOException if we couldn't connect
     */
    public static String sendGET(String url) throws IOException {
        Logging.trace(url);
        HttpClient client = HttpClient.create(new URL(url))
                .setAccept("application/json");
        if (url.contains(Config.OSMCHA_HOST_API)) {
            if (!OSMCHA_AUTHORIZATION.isSet() && !getOsmChaAuthorization()) {
                // They haven't authorized osmcha before, and they declined authorization now
                return null;
            }
            client.setHeader("Authorization", OSMCHA_AUTHORIZATION.get());
        }
        Response response = client.connect();
        String result = null;
        if (response.getResponseCode() == 200) {
            result = response.fetchContent();
        } else if (response.getResponseCode() == 401 && url.contains(Config.OSMCHA_HOST_API)) {
            OSMCHA_AUTHORIZATION.remove();
        }
        response.disconnect();
        return result;
    }
}
