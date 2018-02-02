package org.openstreetmap.josm.plugins.changeset.util;

import java.io.IOException;
import java.net.URL;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;

/**
 *
 * @author ruben
 */
public class Request {
   
    public static String sendGET(String url) throws IOException {
        Util.print("sendGET => :" + url);
        Response response = HttpClient.create(new URL(url))
                .setAccept("application/json")
                .connect();
        String result = response.fetchContent();
        response.disconnect();
        return result;
    }
}
