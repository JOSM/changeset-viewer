// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.IOException;
import java.net.URL;

import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;

/**
 * A helper class to fetch content
 * @author ruben
 */
public final class Request {
    /** Maximum response size for downloads (50 MB) */
    public static final long MAX_DOWNLOAD_SIZE = 50L * 1024 * 1024;

    private Request() {
        // Hide constructor
    }

    /**
     * Get a URL
     * @param url The url to GET
     * @return The result (or null)
     * @throws IOException if we couldn't connect or the response is too large
     */
    public static String sendGET(String url) throws IOException {
        Logging.trace(url);
        HttpClient client = HttpClient.create(new URL(url));
        Response response = client.connect();
        String result = null;
        if (response.getResponseCode() == 200) {
            long contentLength = response.getContentLength();
            if (contentLength > MAX_DOWNLOAD_SIZE) {
                response.disconnect();
                throw new IOException(String.format(
                        "Changeset file is too large to load: %.1f MB (maximum allowed: %d MB).",
                        contentLength / (1024.0 * 1024.0),
                        MAX_DOWNLOAD_SIZE / (1024 * 1024)));
            }
            result = response.fetchContent();
        }
        response.disconnect();
        return result;
    }

    /**
     * Send a POST request
     * @param url The url to POST to
     * @param body The request body
     * @return The result (or null)
     * @throws IOException if we couldn't connect
     */
    public static String sendPOST(String url, String body) throws IOException {
        Logging.trace(url);
        HttpClient client = HttpClient.create(new URL(url), "POST")
                .setRequestBody(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Response response = client.connect();
        String result = null;
        if (response.getResponseCode() == 200) {
            result = response.fetchContent();
        }
        response.disconnect();
        return result;
    }
}
