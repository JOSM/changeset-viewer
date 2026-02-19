// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
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

    /**
     * Build the dataset to show the user
     * @param dataString The adiff XML string
     * @return The dataset
     */
    public BoundedChangesetDataSet build(final String dataString) {
        dataSet = new DataSet();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(dataString)));
            doc.getDocumentElement().normalize();

            NodeList actionNodes = doc.getElementsByTagName("action");
            for (int i = 0; i < actionNodes.getLength(); i++) {
                Element actionElem = (Element) actionNodes.item(i);
                String actionType = actionElem.getAttribute("type");
                processAction(actionElem, actionType);
            }
        } catch (Exception e) {
            Logging.error("Error parsing adiff XML: " + e.getMessage());
            Logging.error(e);
        }

        Bounds bounds = null;
        for (OsmPrimitive osmPrimitive : dataSet.allPrimitives()) {
            bounds = mergeBounds(bounds, osmPrimitive);
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
        Element osmElement = getFirstOsmElement(actionElem);
        if (osmElement == null) return;
        processOsmElement(osmElement, "create");
    }

    private void processModifyAction(final Element actionElem) {
        Element oldContainer = getChildElement(actionElem, "old");
        Element newContainer = getChildElement(actionElem, "new");
        if (oldContainer != null) {
            Element oldElement = getFirstOsmElement(oldContainer);
            if (oldElement != null) {
                processOsmElement(oldElement, "modify-old");
            }
        }
        if (newContainer != null) {
            Element newElement = getFirstOsmElement(newContainer);
            if (newElement != null) {
                processOsmElement(newElement, "modify-new");
            }
        }
    }

    private void processDeleteAction(final Element actionElem) {
        Element oldContainer = getChildElement(actionElem, "old");
        if (oldContainer != null) {
            Element oldElement = getFirstOsmElement(oldContainer);
            if (oldElement != null) {
                processOsmElement(oldElement, "delete");
            }
        }
    }

    private void processOsmElement(final Element elem, final String action) {
        String tagName = elem.getTagName();
        Map<String, String> tags = extractTags(elem);
        switch (tagName) {
            case "node":
                processPoint(tags, elem, action);
                break;
            case "way":
                processLineString(tags, elem, action);
                break;
            case "relation":
                String relAction = action + "-rel";
                Bounds boundsRelation = buildRelation(elem);
                bounds2rectangle(tags, boundsRelation, relAction);
                break;
            default:
                Logging.warn("Unknown OSM element type: " + tagName);
        }
    }

    private void processPoint(final Map<String, String> tags, final Element nodeElem, final String action) {
        LatLon latLon = extractLatLon(nodeElem);
        if (latLon == null) return;
        final Node node = createNode(latLon);
        fillTags(tags, node, action);
    }

    private void processLineString(final Map<String, String> tags, final Element wayElem, final String action) {
        List<LatLon> coordinates = extractWayCoordinates(wayElem);
        if (coordinates.isEmpty()) {
            return;
        }
        final Way way = createWay(coordinates);
        fillTags(tags, way, action);
    }

    private static void fillTags(final Map<String, String> tags, final OsmPrimitive primitive, final String action) {
        Map<String, String> allTags = new TreeMap<>(tags);
        allTags.put("action", action);
        primitive.setKeys(allTags);
    }

    private Node createNode(final LatLon latLon) {
        final Node node = new Node(latLon);
        dataSet.addPrimitive(node);
        return node;
    }

    private Way createWay(final List<LatLon> coordinates) {
        if (coordinates.isEmpty()) {
            return null;
        }
        final Way way = new Way();
        way.setNodes(coordinates.stream().map(this::createNode).collect(Collectors.toList()));
        dataSet.addPrimitive(way);
        return way;
    }

    private static Bounds mergeBounds(final Bounds bounds, final OsmPrimitive osmPrimitive) {
        if (osmPrimitive instanceof Node && ((Node) osmPrimitive).isLatLonKnown()) {
            return mergeBounds(bounds, ((ILatLon) osmPrimitive));
        }
        return bounds;
    }

    private static Bounds mergeBounds(final Bounds bounds, final ILatLon coords) {
        if (bounds == null) {
            return new Bounds(coords.lat(), coords.lon(), coords.lat(), coords.lon());
        } else {
            bounds.extend(coords.lat(), coords.lon());
            return bounds;
        }
    }

    private void bounds2rectangle(final Map<String, String> tags, final Bounds bounds, final String action) {
        if (bounds == null) {
            return;
        }
        double minLat = bounds.getMinLat();
        double minLon = bounds.getMinLon();
        double maxLat = bounds.getMaxLat();
        double maxLon = bounds.getMaxLon();
        List<Node> nodes = Arrays.asList(
                new Node(new LatLon(minLat, minLon)),
                new Node(new LatLon(minLat, maxLon)),
                new Node(new LatLon(maxLat, maxLon)),
                new Node(new LatLon(maxLat, minLon)),
                new Node(new LatLon(minLat, minLon))
        );
        Way way = new Way();
        way.setNodes(nodes);
        fillTags(tags, way, action);
        dataSet.addPrimitiveRecursive(way);
    }

    private static Bounds buildRelation(final Element relationElem) {
        DataSet dataSetRel = new DataSet();
        NodeList members = relationElem.getElementsByTagName("member");
        for (int j = 0; j < members.getLength(); j++) {
            Element member = (Element) members.item(j);
            String memberType = member.getAttribute("type");
            if ("way".equals(memberType)) {
                Way way = processRelationWayMember(member, dataSetRel);
                dataSetRel.addPrimitive(way);
            } else if ("node".equals(memberType)) {
                LatLon latLon = extractLatLon(member);
                if (latLon != null) {
                    Node node = new Node(latLon);
                    dataSetRel.addPrimitive(node);
                }
            }
        }
        Bounds boundsRel = null;
        for (OsmPrimitive osmPrimitive : dataSetRel.allPrimitives()) {
            boundsRel = mergeBounds(boundsRel, osmPrimitive);
        }
        return boundsRel;
    }

    private static Way processRelationWayMember(final Element memberElem, DataSet dataSetRel) {
        Way way = new Way();
        NodeList ndNodes = memberElem.getElementsByTagName("nd");
        if (ndNodes.getLength() == 0) {
            return way;
        }
        List<Node> nodes = new ArrayList<>(ndNodes.getLength());
        for (int i = 0; i < ndNodes.getLength(); i++) {
            Element nd = (Element) ndNodes.item(i);
            LatLon latLon = extractLatLon(nd);
            if (latLon != null) {
                Node node = new Node(latLon);
                dataSetRel.addPrimitive(node);
                nodes.add(node);
            }
        }
        way.setNodes(nodes);
        return way;
    }

    // --- XML helper methods ---

    private static LatLon extractLatLon(final Element elem) {
        String latStr = elem.getAttribute("lat");
        String lonStr = elem.getAttribute("lon");
        if (latStr == null || latStr.isEmpty() || lonStr == null || lonStr.isEmpty()) {
            return null;
        }
        try {
            return new LatLon(Double.parseDouble(latStr), Double.parseDouble(lonStr));
        } catch (NumberFormatException e) {
            Logging.error("Invalid lat/lon: " + latStr + ", " + lonStr);
            return null;
        }
    }

    private static List<LatLon> extractWayCoordinates(final Element wayElem) {
        List<LatLon> coordinates = new LinkedList<>();
        NodeList ndNodes = wayElem.getElementsByTagName("nd");
        for (int i = 0; i < ndNodes.getLength(); i++) {
            Element nd = (Element) ndNodes.item(i);
            LatLon latLon = extractLatLon(nd);
            if (latLon != null) {
                coordinates.add(latLon);
            }
        }
        return coordinates;
    }

    private static Map<String, String> extractTags(final Element elem) {
        Map<String, String> tags = new TreeMap<>();
        NodeList tagNodes = elem.getElementsByTagName("tag");
        for (int i = 0; i < tagNodes.getLength(); i++) {
            Element tag = (Element) tagNodes.item(i);
            String key = tag.getAttribute("k");
            String value = tag.getAttribute("v");
            if (key != null && !key.isEmpty()) {
                tags.put(key, value);
            }
        }
        return tags;
    }

    private static Element getFirstOsmElement(final Element parent) {
        for (String tagName : new String[]{"node", "way", "relation"}) {
            NodeList list = parent.getElementsByTagName(tagName);
            if (list.getLength() > 0) {
                return (Element) list.item(0);
            }
        }
        return null;
    }

    private static Element getChildElement(final Element parent, final String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        if (children.getLength() > 0) {
            return (Element) children.item(0);
        }
        return null;
    }
}
