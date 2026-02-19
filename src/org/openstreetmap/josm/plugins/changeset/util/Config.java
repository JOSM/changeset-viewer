// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

/**
 * A class storing config values
 * @author ruben
 */
public final class Config {
    private Config() {
        // Hide constructor
    }

    /**
     * Supported mapping platforms
     */
    public enum Platform {
        OSM("OpenStreetMap",
            "https://api.openstreetmap.org/api/0.6/",
            "https://adiffs.osmcha.org/changesets/",
            "https://www.openstreetmap.org/changeset/",
            "https://overpass-api.de/api/interpreter"),
        OHM("OpenHistoricalMap",
            "https://www.openhistoricalmap.org/api/0.6/",
            "https://s3.us-east-1.amazonaws.com/planet.openhistoricalmap.org/ohm-augmented-diffs/changesets/",
            "https://www.openhistoricalmap.org/changeset/",
            "https://overpass-api.openhistoricalmap.org/api/interpreter");

        private final String label;
        private final String apiUrl;
        private final String adiffsHost;
        private final String changesetUrl;
        private final String overpassUrl;

        Platform(String label, String apiUrl, String adiffsHost, String changesetUrl, String overpassUrl) {
            this.label = label;
            this.apiUrl = apiUrl;
            this.adiffsHost = adiffsHost;
            this.changesetUrl = changesetUrl;
            this.overpassUrl = overpassUrl;
        }

        public String getLabel() {
            return label;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getAdiffsHost() {
            return adiffsHost;
        }

        public String getChangesetUrl() {
            return changesetUrl;
        }

        public String getOverpassUrl() {
            return overpassUrl;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static Platform platform = Platform.OSM;
    private static String bbox = "";

    public static void setPlatform(Platform platform) {
        Config.platform = platform;
    }

    public static Platform getPlatform() {
        return platform;
    }

    public static String getApiUrl() {
        return platform.getApiUrl();
    }

    public static String getAdiffsHost() {
        return platform.getAdiffsHost();
    }

    public static String getChangesetWebUrl() {
        return platform.getChangesetUrl();
    }

    public static String getOverpassUrl() {
        return platform.getOverpassUrl();
    }

    public static void setBBOX(String bbox) {
        Config.bbox = bbox;
    }

    public static String getBBOX() {
        return bbox;
    }
}
