// License: MIT. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.changeset.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Logging;

/**
 * Build the changeset dataset to show the user
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
     * @param dataString The json string
     * @return The dataset
     */
    public BoundedChangesetDataSet build(final String dataString) {
        dataSet = new DataSet();
        try (JsonReader reader = Json.createReader(new StringReader(dataString))) {
            JsonObject jsonObject = reader.readObject();
            return build(jsonObject.getJsonArray("elements"));
        }
    }

    private BoundedChangesetDataSet build(JsonArray array) {
        for (JsonObject obj : array.getValuesAs(JsonObject.class)) {
            String action = obj.getString("action");
            String type = obj.getString("type");
            JsonObject tags = obj.getJsonObject("tags");
            //DELETE
            if ("delete".equals(action) && "node".equals(type) && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                processPoint(tags, old, action);
            } else if ("delete".equals(action) && "way".equals(type) && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                processLineString(tags, old, action);
            } //CREATE
            else if ("create".equals(action) && "node".equals(type)) {
                processPoint(tags, obj, action);
            } else if ("create".equals(action) && "way".equals(type)) {
                processLineString(tags, obj, action);
            } //MODIFY
            else if ("modify".equals(action) && "way".equals(type)) {
                //NEW
                processLineString(tags, obj, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                processLineString(tags, old, "modify-old");
            } else if ("modify".equals(action) && "node".equals(type)) {
                //NEW
                processPoint(tags, obj, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                processPoint(tags, old, "modify-old");
                //RELATION
            } else if ("modify".equals(action) && "relation".equals(type)) {
                //OLD
                JsonObject old = obj.getJsonObject("old");
                Bounds boundsRelationOld = buildRelation(old);
                bounds2rectangle(tags, boundsRelationOld, "modify-old-rel");
                //NEW
                Bounds boundsRelationNew = buildRelation(obj);
                bounds2rectangle(tags, boundsRelationNew, "modify-new-rel");
            } else if ("create".equals(action) && "relation".equals(type)) {
                Bounds boundsRelationNew = buildRelation(obj);
                bounds2rectangle(tags, boundsRelationNew, "create-rel");
            } else if ("delete".equals(action) && "relation".equals(type)) {
                JsonObject old = obj.getJsonObject("old");
                Bounds boundsRelationNew = buildRelation(old);
                bounds2rectangle(tags, boundsRelationNew, "delete-rel");
            }
        }

        Bounds bounds = null;
        for (OsmPrimitive osmPrimitive : dataSet.allPrimitives()) {
            bounds = mergeBounds(bounds, osmPrimitive);
        }
        return new BoundedChangesetDataSet(dataSet, bounds);
    }

    private void processPoint(final JsonObject tags, final JsonObject nodeJson, final String action) {
        final Node node = createNode(newLatLon(nodeJson));
        fillTagsFromFeature(tags, node, action);
    }

    private void processLineString(final JsonObject tags, final JsonObject wayJson, final String action) {
        JsonArray arrayNodes = wayJson.getJsonArray("nodes");
        if (arrayNodes.isEmpty()) {
            return;
        }

        List<LatLon> coordinates = new LinkedList<>();
        for (int i = 0; i < arrayNodes.size(); i++) {
            coordinates.add(newLatLon(arrayNodes.getJsonObject(i)));
        }

        final Way way = createWay(coordinates);
        fillTagsFromFeature(tags, way, action);
    }

    private static void fillTagsFromFeature(final JsonObject tags, final OsmPrimitive primitive, final String action) {
        if (tags != null) {
            primitive.setKeys(getTags(tags, action));
        }
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

    private static Map<String, String> getTags(final JsonObject tags, final String action) {
        final Map<String, String> mapTags = new TreeMap<>();
        mapTags.put("action", action);
        for (Map.Entry<String, JsonValue> entry : tags.entrySet()) {
            mapTags.put(entry.getKey(), String.valueOf(entry.getValue().toString()));
        }
        return mapTags;
    }

    private static Bounds mergeBounds(final Bounds bounds, final OsmPrimitive osmPrimitive) {
        // ways and relations consist of nodes that are already in the dataset
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

    private void bounds2rectangle(final JsonObject tags, final Bounds bounds, final String action) {
        if (bounds == null) {
            return;
        }
        double minLat = bounds.getMinLat();
        double minLon = bounds.getMinLon();
        double maxLat = bounds.getMaxLat();
        double maxLon = bounds.getMaxLon();
        List<Node> nodes = new ArrayList<>(4);
        Node n1 = new Node(new LatLon(minLat, minLon));
        dataSet.addPrimitive(n1);
        nodes.add(n1);
        Node n2 = new Node(new LatLon(minLat, maxLon));
        dataSet.addPrimitive(n2);
        nodes.add(n2);
        Node n3 = new Node(new LatLon(maxLat, maxLon));
        dataSet.addPrimitive(n3);
        nodes.add(n3);
        Node n4 = new Node(new LatLon(maxLat, minLon));
        dataSet.addPrimitive(n4);
        nodes.add(n4);
        Node n5 = new Node(new LatLon(minLat, minLon));
        dataSet.addPrimitive(n5);
        nodes.add(n5);
        Way way = new Way();
        way.setNodes(nodes);
        fillTagsFromFeature(tags, way, action);
        dataSet.addPrimitive(way);
    }

    private static Bounds buildRelation(final JsonObject obj) {
        DataSet dataSetRel = new DataSet();
        JsonArray members = obj.getJsonArray("members");
        for (int j = 0; j < members.size(); j++) {
            JsonObject member = members.getJsonObject(j);
            String memberType = member.getString("type");
            if ("way".equals(memberType)) {
                dataSetRel.addPrimitive(processRelationLineString(member, dataSetRel));
            } else if ("node".equals(memberType)) {
                dataSetRel.addPrimitive(newNode(member));
            }
        }
        Bounds boundsRelationOld = null;
        for (OsmPrimitive osmPrimitive : dataSetRel.allPrimitives()) {
            boundsRelationOld = mergeBounds(boundsRelationOld, osmPrimitive);
        }
        return boundsRelationOld;
    }

    private static LatLon newLatLon(final JsonObject json) {
        JsonString latString = json.getJsonString("lat");
        JsonString lonString = json.getJsonString("lon");
        if (latString == null || lonString == null) {
            Logging.error("Invalid JSON: " + json);
            return null;
        }
        return new LatLon(
                Double.parseDouble(latString.getString()),
                Double.parseDouble(lonString.getString()));
    }

    private static Node newNode(final JsonObject nodeJson) {
        return new Node(newLatLon(nodeJson));
    }

    private static Way processRelationLineString(final JsonObject wayJson, DataSet dataSetOld) {
        JsonArray arrayNodes = wayJson.getJsonArray("nodes");
        Way way = new Way();
        if (arrayNodes.isEmpty()) {
            return way;
        }
        List<Node> nodes = new ArrayList<>(arrayNodes.size());
        for (int i = 0; i < arrayNodes.size(); i++) {
            Node node = newNode(arrayNodes.getJsonObject(i));
            dataSetOld.addPrimitive(node);
            nodes.add(node);
        }
        way.setNodes(nodes);
        return way;
    }
}
