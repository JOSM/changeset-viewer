package org.openstreetmap.josm.plugins.changeset.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 *
 * @author ruben
 */
public class DataSetChangesetBuilder {

    public static final int MAX_LINK_LENGTH = 102400;

    public static class BoundedChangesetDataSet {

        private final DataSet dataSet;
        private final Bounds bounds;

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

    public BoundedChangesetDataSet build(final String dataString) {
        dataSet = new DataSet();
        JsonReader reader = Json.createReader(new StringReader(dataString));
        JsonObject jsonObject = reader.readObject();
        JsonArray array = jsonObject.getJsonArray("elements");
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).asJsonObject();
            String action = obj.getString("action");
            String type = obj.getString("type");
            JsonObject tags = obj.getJsonObject("tags");
            //DELETE
            if (action.equals("delete") && type.equals("node") && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                processPoint(tags, old, action);
            } else if (action.equals("delete") && type.equals("way") && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                processLineString(tags, old, action);
            } //CREATE
            else if (action.equals("create") && type.equals("node")) {
                processPoint(tags, obj, action);
            } else if (action.equals("create") && type.equals("way")) {
                processLineString(tags, obj, action);
            } //MODIFY
            else if (action.equals("modify") && type.equals("way")) {
                //NEW
                processLineString(tags, obj, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                processLineString(tags, old, "modify-old");
            } else if (action.equals("modify") && type.equals("node")) {
                //NEW
                processPoint(tags, obj, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                processPoint(tags, old, "modify-old");
                //RELATION
            } else if (action.equals("modify") && type.equals("relation")) {
                //OLD
                JsonObject old = obj.getJsonObject("old");
                Bounds boundsRelationOld = buildRelation(tags, old, "modify-old-rel");
                bounds2rectangle(tags, boundsRelationOld, "modify-old-rel");
                //NEW
                Bounds boundsRelationNew = buildRelation(tags, obj, "modify-new-rel");
                bounds2rectangle(tags, boundsRelationNew, "modify-new-rel");
            } else if (action.equals("create") && type.equals("relation")) {
                Bounds boundsRelationNew = buildRelation(tags, obj, "create-rel");
                bounds2rectangle(tags, boundsRelationNew, "create-rel");
            } else if (action.equals("delete") && type.equals("relation")) {
                JsonObject old = obj.getJsonObject("old");
                Bounds boundsRelationNew = buildRelation(tags, old, "delete-rel");
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
        Double lat = Double.parseDouble(nodeJson.getString("lat"));
        Double lon = Double.parseDouble(nodeJson.getString("lon"));
        LatLon latLon = new LatLon(lat, lon);
        final Node node = createNode(latLon);
        fillTagsFromFeature(tags, node, action);
    }

    private void processLineString(final JsonObject tags, final JsonObject wayJson, final String action) {
        JsonArray arrayNodes = wayJson.getJsonArray("nodes");
        if (arrayNodes.isEmpty()) {
            return;
        }

        List<LatLon> coordinates = new LinkedList<>();
        for (int i = 0; i < arrayNodes.size(); i++) {
            Double lat = Double.parseDouble(arrayNodes.getJsonObject(i).getString("lat"));
            Double lon = Double.parseDouble(arrayNodes.getJsonObject(i).getString("lon"));
            LatLon latLon = new LatLon(lat, lon);
            coordinates.add(latLon);
        }

        final Way way = createWay(coordinates);
        fillTagsFromFeature(tags, way, action);
    }

    private void fillTagsFromFeature(final JsonObject tags, final OsmPrimitive primitive, final String action) {
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
        final List<Node> nodes = new ArrayList<>(coordinates.size());
        for (final LatLon point : coordinates) {
            final Node node = createNode(point);
            nodes.add(node);
        }
        way.setNodes(nodes);
        dataSet.addPrimitive(way);
        return way;
    }

    private Map<String, String> getTags(final JsonObject tags, final String action) {
        final Map<String, String> mapTags = new TreeMap<>();
        mapTags.put("action", action);
        for (Map.Entry<String, JsonValue> entry : tags.entrySet()) {
            mapTags.put(entry.getKey(), String.valueOf(entry.getValue().toString()));
        }
        return mapTags;
    }

    private Bounds mergeBounds(final Bounds bounds, final OsmPrimitive osmPrimitive) {
        if (osmPrimitive instanceof Node) { // ways and relations consist of nodes that are already in the dataset
            return mergeBounds(bounds, ((Node) osmPrimitive).getCoor());
        }
        return bounds;
    }

    private Bounds mergeBounds(final Bounds bounds, final LatLon coords) {
        if (bounds == null) {
            return new Bounds(coords);
        } else {
            bounds.extend(coords);
            return bounds;
        }
    }

    private void bounds2rectangle(final JsonObject tags, final Bounds bounds, final String action) {
        Double minLat = bounds.getMinLat();
        Double minLon = bounds.getMinLon();
        Double maxLat = bounds.getMaxLat();
        Double maxLon = bounds.getMaxLon();
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

    private Bounds buildRelation(final JsonObject tags, final JsonObject obj, final String action) {
        DataSet dataSetRel = new DataSet();
        //OLD
        JsonArray members = obj.getJsonArray("members");
        for (int j = 0; j < members.size(); j++) {
            JsonObject member = members.getJsonObject(j);
            if (member.getString("type").equals("way")) {
//                processLineString(tags, member, action + "-rel");
                dataSetRel.addPrimitive(processRelationLineString(tags, member, dataSetRel));
            } else if (member.getString("type").equals("node")) {
//                processPoint(tags, member, action + "-rel");
                dataSetRel.addPrimitive(processRelationPoint(tags, member));
            }
        }
        Bounds boundsRelationOld = null;
        for (OsmPrimitive osmPrimitive : dataSetRel.allPrimitives()) {
            boundsRelationOld = mergeBounds(boundsRelationOld, osmPrimitive);
        }
        return boundsRelationOld;
    }

    private Node processRelationPoint(final JsonObject tags, final JsonObject nodeJson) {
        Double lat = Double.parseDouble(nodeJson.getString("lat"));
        Double lon = Double.parseDouble(nodeJson.getString("lon"));
        LatLon latLon = new LatLon(lat, lon);
        Node node = new Node(latLon);
        return node;
    }

    private Way processRelationLineString(final JsonObject tags, final JsonObject wayJson, DataSet dataSetOld) {
        JsonArray arrayNodes = wayJson.getJsonArray("nodes");
        Way way = new Way();
        if (arrayNodes.isEmpty()) {
            return way;
        }
        List<Node> nodes = new ArrayList<>(arrayNodes.size());
        for (int i = 0; i < arrayNodes.size(); i++) {
            Double lat = Double.parseDouble(arrayNodes.getJsonObject(i).getString("lat"));
            Double lon = Double.parseDouble(arrayNodes.getJsonObject(i).getString("lon"));
            LatLon latLon = new LatLon(lat, lon);
            Node node = new Node(latLon);
            dataSetOld.addPrimitive(node);
            nodes.add(node);
        }
        way.setNodes(nodes);
        return way;
    }
}
