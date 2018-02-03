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
        System.out.println(url);
        Response response = HttpClient.create(new URL(url))
                .setAccept("application/json")
                .connect();
        String result = null;
        if (response.getResponseCode() == 200) {
            result = response.fetchContent();
        }
        response.disconnect();
        return result;
    }
}
