// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;
import org.openstreetmap.josm.tools.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Get changesets to show the user and show specific changesets
 * @author ruben
 */
public final class ChangesetController {
    public static final int PAGE_SIZE = 20;

    private ChangesetController() {
        // Hide the constructor
    }

    /**
     * Get a changeset, trying adiffs.osmcha.org first, then Overpass API as fallback
     * @param changesetId The changeset to get
     * @return The dataset to show
     */
    public static BoundedChangesetDataSet getChangeset(String changesetId) throws IOException {
        DataSetChangesetBuilder builder = new DataSetChangesetBuilder();
        try {
            // Try adiffs host first (only available for OSM)
            String adiffsHost = Config.getAdiffsHost();
            if (!adiffsHost.isEmpty()) {
                String url = adiffsHost + changesetId + ".adiff";
                String adiff = Request.sendGET(url);
                if (adiff != null) {
                    return builder.build(adiff);
                }
            }
            // Fallback: get changeset metadata from API, then query Overpass
            Logging.info("Changeset " + changesetId + " not found on adiffs host, trying Overpass API...");
            String adiffOverpass = getAdiffFromOverpass(changesetId);
            if (adiffOverpass != null) {
                return builder.build(adiffOverpass);
            }
            return null;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            Logger.getLogger(ChangesetController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Get augmented diff from Overpass API using changeset metadata
     */
    private static String getAdiffFromOverpass(String changesetId) throws Exception {
        String csUrl = Config.getApiUrl() + "changeset/" + changesetId;
        String csXml = Request.sendGET(csUrl);
        if (csXml == null) {
            return null;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(new InputSource(new StringReader(csXml)));
        NodeList csList = doc.getElementsByTagName("changeset");
        if (csList.getLength() == 0) {
            return null;
        }
        Element csElem = (Element) csList.item(0);
        String createdAt = csElem.getAttribute("created_at");
        String closedAt = csElem.getAttribute("closed_at");
        String minLat = csElem.getAttribute("min_lat");
        String minLon = csElem.getAttribute("min_lon");
        String maxLat = csElem.getAttribute("max_lat");
        String maxLon = csElem.getAttribute("max_lon");

        if (createdAt.isEmpty() || closedAt.isEmpty()) {
            return null;
        }

        String beforeTime = adjustTime(createdAt, -1);
        String afterTime = adjustTime(closedAt, 1);

        String bboxFilter = "";
        if (!minLat.isEmpty() && !minLon.isEmpty() && !maxLat.isEmpty() && !maxLon.isEmpty()) {
            bboxFilter = "[bbox:" + minLat + "," + minLon + "," + maxLat + "," + maxLon + "]";
        }

        // Query all elements in bbox; [adiff] mode only outputs elements that
        // changed between the two timestamps (create/modify/delete).
        String query = "[adiff:\"" + beforeTime + "\",\"" + afterTime + "\"]" + bboxFilter + ";"
                + "node;out meta;"
                + "way;out meta geom;"
                + "relation;out meta geom;";

        Logging.info("Overpass query for changeset " + changesetId + ": " + query);
        return Request.sendPOST(Config.getOverpassUrl(), "data=" + java.net.URLEncoder.encode(query, "UTF-8"));
    }

    private static String adjustTime(String isoTime, int seconds) {
        Instant instant = Instant.parse(isoTime);
        return instant.plusSeconds(seconds).toString();
    }

    /**
     * Collect all unique changeset IDs from the currently loaded data (fast, no API calls)
     * @return The changeset list without stats
     */
    public static List<ChangesetBeen> collectChangesetIds() {
        Map<Integer, ChangesetBeen> changesetMap = new LinkedHashMap<>();
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null) {
            return new ArrayList<>();
        }
        for (OsmPrimitive primitive : dataSet.allPrimitives()) {
            int csId = primitive.getChangesetId();
            if (csId <= 0) {
                continue;
            }
            if (!changesetMap.containsKey(csId)) {
                ChangesetBeen cs = new ChangesetBeen();
                cs.setChangesetId(csId);
                cs.setUser(primitive.getUser() != null ? primitive.getUser().getName() : "");
                cs.setDate(primitive.getInstant() != null ? primitive.getInstant().toString() : "");
                changesetMap.put(csId, cs);
            }
        }
        return new ArrayList<>(changesetMap.values());
    }

    /**
     * Fetch changesets from the platform API for a given bbox
     * @param bbox The bounding box (minLon,minLat,maxLon,maxLat)
     * @return The changeset list with stats from the API
     */
    public static List<ChangesetBeen> fetchChangesetsFromApi(String bbox) {
        List<ChangesetBeen> result = new ArrayList<>();
        try {
            String url = Config.getApiUrl() + "changesets?bbox=" + bbox + "&closed=true&limit=100";
            Logging.info("Fetching changesets from API: " + url);
            String xml = Request.sendGET(url);
            if (xml == null) {
                return result;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
            NodeList csList = doc.getElementsByTagName("changeset");
            for (int i = 0; i < csList.getLength(); i++) {
                Element elem = (Element) csList.item(i);
                ChangesetBeen cs = new ChangesetBeen();
                cs.setChangesetId(parseIntAttr(elem, "id"));
                cs.setUser(elem.getAttribute("user"));
                cs.setDate(elem.getAttribute("closed_at"));
                cs.setCreate(parseIntAttr(elem, "changes_count"));
                result.add(cs);
            }
        } catch (Exception ex) {
            Logging.warn("Could not fetch changesets from API: " + ex.getMessage());
        }
        return result;
    }

    /**
     * Fetch stats from the API for a page of changesets
     * @param allChangesets The full list of changesets
     * @param page The page number (0-based)
     */
    public static void fetchStatsForPage(List<ChangesetBeen> allChangesets, int page) {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allChangesets.size());
        for (int i = from; i < to; i++) {
            fetchChangesetStats(allChangesets.get(i));
        }
    }

    /**
     * Fetch changeset stats (create/modify/delete counts) from the OSM API
     */
    private static void fetchChangesetStats(ChangesetBeen cs) {
        try {
            String csUrl = Config.getApiUrl() + "changeset/" + cs.getChangesetId();
            String xml = Request.sendGET(csUrl);
            if (xml == null) {
                return;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
            NodeList csList = doc.getElementsByTagName("changeset");
            if (csList.getLength() == 0) {
                return;
            }
            Element elem = (Element) csList.item(0);
            cs.setUser(elem.getAttribute("user"));
            cs.setDate(elem.getAttribute("closed_at"));
            cs.setCreate(parseIntAttr(elem, "created_count"));
            cs.setModify(parseIntAttr(elem, "modified_count"));
            cs.setDelete(parseIntAttr(elem, "deleted_count"));
        } catch (Exception ex) {
            Logging.warn("Could not fetch stats for changeset " + cs.getChangesetId() + ": " + ex.getMessage());
        }
    }

    private static int parseIntAttr(Element elem, String attr) {
        String val = elem.getAttribute(attr);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
