package org.openstreetmap.josm.plugins.changeset.util;

/**
 *
 * @author ruben
 */
public class Config {

    public static final String OSMCHA_HOST = "https://osmcha.mapbox.com/api/v1/changesets/?";
    public static final String HOST = "https://s3.amazonaws.com/mapbox/real-changesets/production/";
    public static final String CHANGESET_MAP = "https://osmlab.github.io/changeset-map/";
    private static String PAGE = "page=1";
    private static String PAGE_SIZE = "page_size=75";
    private static String BBOX = "none";

    public String getPAGE() {
        return PAGE;
    }

    public void setPAGE(String PAGE) {
        Config.PAGE = PAGE;
    }

    public String getPAGE_SIZE() {
        return PAGE_SIZE;
    }

    public void setPAGE_SIZE(String PAGE_SIZE) {
        Config.PAGE_SIZE = PAGE_SIZE;
    }

    public String getBBOX() {
        return BBOX;
    }

    public static void setBBOX(String BBOX) {
        Config.BBOX = "in_bbox="+BBOX;
    }

    public static String getHost() {
        if (BBOX.equals("none")) {
            return OSMCHA_HOST + PAGE + "&" + PAGE_SIZE;
        } else {
            return OSMCHA_HOST + PAGE + "&" + PAGE_SIZE + "&" + BBOX;
        }
    }

}
