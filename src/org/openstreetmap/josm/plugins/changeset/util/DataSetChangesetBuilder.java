// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Build the changeset dataset from an augmented diff XML file
 * @author ruben
 */
public class DataSetChangesetBuilder {

    private static final DocumentBuilderFactory DOC_FACTORY = DocumentBuilderFactory.newInstance();

    /**
     * A bounded changset dataset to show the user
     */
    public static class BoundedChangesetDataSet {

        private final DataSet dataSet;
        private final Bounds bounds;

        /**
         * Create a new {@link BoundedChangesetDataSet}
         * @param dataSet The dataset with the changeset data
         * @param bounds The bounds of the changeset
         */
        public BoundedChangesetDataSet(final DataSet dataSet, final Bounds bounds) {
            this.dataSet = dataSet;
            this.bounds = bounds;
        }

        public Bounds getBounds() {
            return this.bounds;
        }

        public DataSet getDataSet() {
            return this.dataSet;
        }
    }

    private DataSet dataSet;
    private Bounds bounds;

    /**
     * Build the dataset to show the user
     * @param dataString The adiff XML string
     * @return The dataset
     */
    public BoundedChangesetDataSet build(final String dataString) {
        dataSet = new DataSet();
        bounds = null;
        try {
            DocumentBuilder docBuilder = DOC_FACTORY.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(dataString)));

            // Iterate direct children of root instead of getElementsByTagName
            org.w3c.dom.Node child = doc.getDocumentElement().getFirstChild();
            while (child != null) {
                if (child instanceof Element) {
                    Element elem = (Element) child;
                    if ("action".equals(elem.getTagName())) {
                        processAction(elem, elem.getAttribute("type"));
                    }
                }
                child = child.getNextSibling();
            }
        } catch (Exception e) {
            Logging.error("Error parsing adiff XML: " + e.getMessage());
            Logging.error(e);
        }

        return new BoundedChangesetDataSet(dataSet, bounds);
    }

    private void processAction(final Element actionElem, final String actionType) {
        switch (actionType) {
            case "create":
                processCreateAction(actionElem);
                break;
            case "modify":
                processModifyAction(actionElem);
                break;
            case "delete":
                processDeleteAction(actionElem);
                break;
            default:
                Logging.warn("Unknown action type: " + actionType);
        }
    }

    private void processCreateAction(final Element actionElem) {
        Element osmElement = getFirstChildOsmElement(actionElem);
        if (osmElement == null) return;
        processOsmElement(osmElement, "create");
    }

    private void processModifyAction(final Element actionElem) {
        Element oldContainer = getDirectChild(actionElem, "old");
        Element newContainer = getDirectChild(actionElem, "new");
        if (oldContainer != null) {
            Element oldElement = getFirstChildOsmElement(oldContainer);
            if (oldElement != null) {
                processOsmElement(oldElement, "modify-old");
            }
        }
        if (newContainer != null) {
            Element newElement = getFirstChildOsmElement(newContainer);
            if (newElement != null) {
                processOsmElement(newElement, "modify-new");
            }
        }
    }

    private void processDeleteAction(final Element actionElem) {
        Element oldContainer = getDirectChild(actionElem, "old");
        if (oldContainer != null) {
            Element oldElement = getFirstChildOsmElement(oldContainer);
            if (oldElement != null) {
                processOsmElement(oldElement, "delete");
            }
        }
    }

    private void processOsmElement(final Element elem, final String action) {
        String tagName = elem.getTagName();
        Map<String, String> tags = extractTags(elem);
        tags.put("action", action);
        switch (tagName) {
            case "node":
                processPoint(tags, elem);
                break;
            case "way":
                processLineString(tags, elem);
                break;
            case "relation":
                Bounds boundsRelation = buildRelation(elem);
                bounds2rectangle(tags, boundsRelation, action + "-rel");
                break;
            default:
                Logging.warn("Unknown OSM element type: " + tagName);
        }
    }

    private void processPoint(final Map<String, String> tags, final Element nodeElem) {
        LatLon latLon = extractLatLon(nodeElem);
        if (latLon == null) return;
        final Node node = createNode(latLon);
        node.setKeys(tags);
    }

    private void processLineString(final Map<String, String> tags, final Element wayElem) {
        List<LatLon> coordinates = extractWayCoordinates(wayElem);
        if (coordinates.isEmpty()) {
            return;
        }
        final Way way = createWay(coordinates);
        way.setKeys(tags);
    }

    private Node createNode(final LatLon latLon) {
        final Node node = new Node(latLon);
        dataSet.addPrimitive(node);
        extendBounds(latLon);
        return node;
    }

    private Way createWay(final List<LatLon> coordinates) {
        final Way way = new Way();
        List<Node> nodes = new ArrayList<>(coordinates.size());
        for (LatLon ll : coordinates) {
            nodes.add(createNode(ll));
        }
        way.setNodes(nodes);
        dataSet.addPrimitive(way);
        return way;
    }

    private void extendBounds(final LatLon latLon) {
        if (bounds == null) {
            bounds = new Bounds(latLon.lat(), latLon.lon(), latLon.lat(), latLon.lon());
        } else {
            bounds.extend(latLon.lat(), latLon.lon());
        }
    }

    private void bounds2rectangle(final Map<String, String> tags, final Bounds relBounds, final String relAction) {
        if (relBounds == null) {
            return;
        }
        double minLat = relBounds.getMinLat();
        double minLon = relBounds.getMinLon();
        double maxLat = relBounds.getMaxLat();
        double maxLon = relBounds.getMaxLon();
        List<Node> nodes = Arrays.asList(
                new Node(new LatLon(minLat, minLon)),
                new Node(new LatLon(minLat, maxLon)),
                new Node(new LatLon(maxLat, maxLon)),
                new Node(new LatLon(maxLat, minLon)),
                new Node(new LatLon(minLat, minLon))
        );
        Way way = new Way();
        way.setNodes(nodes);
        Map<String, String> relTags = new HashMap<>(tags);
        relTags.put("action", relAction);
        way.setKeys(relTags);
        dataSet.addPrimitiveRecursive(way);
    }

    private static Bounds buildRelation(final Element relationElem) {
        Bounds boundsRel = null;
        // Iterate direct children for member elements
        org.w3c.dom.Node child = relationElem.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element member = (Element) child;
                if ("member".equals(member.getTagName())) {
                    String memberType = member.getAttribute("type");
                    if ("way".equals(memberType)) {
                        boundsRel = extendBoundsFromWayNds(member, boundsRel);
                    } else if ("node".equals(memberType)) {
                        LatLon latLon = extractLatLon(member);
                        if (latLon != null) {
                            boundsRel = extendStaticBounds(boundsRel, latLon);
                        }
                    }
                }
            }
            child = child.getNextSibling();
        }
        return boundsRel;
    }

    private static Bounds extendBoundsFromWayNds(final Element memberElem, Bounds boundsRel) {
        org.w3c.dom.Node child = memberElem.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element nd = (Element) child;
                if ("nd".equals(nd.getTagName())) {
                    LatLon latLon = extractLatLon(nd);
                    if (latLon != null) {
                        boundsRel = extendStaticBounds(boundsRel, latLon);
                    }
                }
            }
            child = child.getNextSibling();
        }
        return boundsRel;
    }

    private static Bounds extendStaticBounds(Bounds b, LatLon ll) {
        if (b == null) {
            return new Bounds(ll.lat(), ll.lon(), ll.lat(), ll.lon());
        }
        b.extend(ll.lat(), ll.lon());
        return b;
    }

    // --- XML helper methods using direct child iteration ---

    private static LatLon extractLatLon(final Element elem) {
        String latStr = elem.getAttribute("lat");
        String lonStr = elem.getAttribute("lon");
        if (latStr == null || latStr.isEmpty() || lonStr == null || lonStr.isEmpty()) {
            return null;
        }
        try {
            return new LatLon(Double.parseDouble(latStr), Double.parseDouble(lonStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<LatLon> extractWayCoordinates(final Element wayElem) {
        List<LatLon> coordinates = new ArrayList<>();
        org.w3c.dom.Node child = wayElem.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element elem = (Element) child;
                if ("nd".equals(elem.getTagName())) {
                    LatLon latLon = extractLatLon(elem);
                    if (latLon != null) {
                        coordinates.add(latLon);
                    }
                }
            }
            child = child.getNextSibling();
        }
        return coordinates;
    }

    private static Map<String, String> extractTags(final Element elem) {
        Map<String, String> tags = new HashMap<>();
        org.w3c.dom.Node child = elem.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element tag = (Element) child;
                if ("tag".equals(tag.getTagName())) {
                    String key = tag.getAttribute("k");
                    String value = tag.getAttribute("v");
                    if (key != null && !key.isEmpty()) {
                        tags.put(key, value);
                    }
                }
            }
            child = child.getNextSibling();
        }
        return tags;
    }

    private static Element getFirstChildOsmElement(final Element parent) {
        org.w3c.dom.Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                String name = ((Element) child).getTagName();
                if ("node".equals(name) || "way".equals(name) || "relation".equals(name)) {
                    return (Element) child;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static Element getDirectChild(final Element parent, final String childName) {
        org.w3c.dom.Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && childName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }
}
