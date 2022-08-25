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

    public static final String OSMCHA_HOST = "https://osmcha.org/";
    public static final String OSMCHA_HOST_API = OSMCHA_HOST + "api/v1/";
    public static final String OSMCHA_HOST_CHANGESETS = OSMCHA_HOST_API + "changesets/?";
    public static final String HOST = "https://s3.amazonaws.com/mapbox/real-changesets/production/";
    public static final String CHANGESET_MAP = "https://osmlab.github.io/changeset-map/";
    public static final String OSMCHANGESET = "https://www.openstreetmap.org/changeset/";
    private static int page = 1;
    private static String pageSize = "page_size=75";
    private static String bbox = "none";
    private static final String areaLt = "area_lt=1";

    public static int getPAGE() {
        return page;
    }

    public static void setPAGE(int page) {
        Config.page = page;
    }

    public static String getPAGE_SIZE() {
        return pageSize;
    }

    public static void setPAGE_SIZE(String pageSize) {
        Config.pageSize = pageSize;
    }

    public static String getBBOX() {
        return bbox;
    }

    public static void setBBOX(String bbox) {
        Config.bbox = "in_bbox=" + bbox;
    }

    public static String getHost() {
        if ("none".equals(bbox)) {
            return OSMCHA_HOST_CHANGESETS + "page=" + page + "&" + pageSize + "&" + areaLt;
        } else {
            return OSMCHA_HOST_CHANGESETS + "page=" + page + "&" + pageSize + "&" + bbox + "&" + areaLt;
        }
    }

}
