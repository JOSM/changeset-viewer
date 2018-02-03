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
 * @author ruben modified from :
 * https://github.com/JOSM/geojson/blob/master/src/main/java/org/openstreetmap/josm/plugins/geojson/DataSetBuilder.java
 */
public class DataSetBuilderChangesets {

    public static final int MAX_LINK_LENGTH = 102400;

    public static class BoundedDataSetChangestes {

        private final DataSet dataSet;
        private final Bounds bounds;

        public BoundedDataSetChangestes(final DataSet dataSet, final Bounds bounds) {
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

    public BoundedDataSetChangestes build(final String dataString) {
        dataSet = new DataSet();

        JsonReader reader = Json.createReader(new StringReader(dataString));
        JsonObject jsonObject = reader.readObject();
        JsonArray array = jsonObject.getJsonArray("elements");

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).asJsonObject();
            String action = obj.getString("action");
            String type = obj.getString("type");
            JsonObject tags = obj.getJsonObject("tags");
            if (action.equals("delete") && type.equals("node") && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                Double lat = Double.parseDouble(old.getString("lat"));
                Double lon = Double.parseDouble(old.getString("lon"));
                LatLon latLon = new LatLon(lat, lon);
                processPoint(tags, latLon, action);
            } else if (action.equals("delete") && type.equals("way") && !obj.isNull("old")) {
                JsonObject old = obj.getJsonObject("old");
                JsonArray arrayNodes = old.getJsonArray("nodes");
                processLineString(tags, arrayNodes, action);
            } else if (action.equals("create") && type.equals("node")) {
                Double lat = Double.parseDouble(obj.getString("lat"));
                Double lon = Double.parseDouble(obj.getString("lon"));
                LatLon latLon = new LatLon(lat, lon);
                processPoint(tags, latLon, action);
            } else if (action.equals("create") && type.equals("way")) {
                JsonArray arrayNodes = obj.getJsonArray("nodes");
                processLineString(tags, arrayNodes, action);
            } else if (action.equals("modify") && type.equals("way")) {
                //NEW
                JsonArray arrayNodesNew = obj.getJsonArray("nodes");
                processLineString(tags, arrayNodesNew, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                JsonArray arrayNodesOld = obj.getJsonArray("nodes");
                processLineString(tags, arrayNodesOld, "modify-old");
            } else if (action.equals("modify") && type.equals("node")) {
                //NEW
                Double latNew = Double.parseDouble(obj.getString("lat"));
                Double lonNew = Double.parseDouble(obj.getString("lon"));
                LatLon latLonNew = new LatLon(latNew, lonNew);
                processPoint(tags, latLonNew, "modify-new");
                //OLD
                JsonObject old = obj.getJsonObject("old");
                Double latOld = Double.parseDouble(old.getString("lat"));
                Double lonOld = Double.parseDouble(old.getString("lon"));
                LatLon latLonOld = new LatLon(latOld, lonOld);
                processPoint(tags, latLonOld, "modify-old");
            }
        }

        Bounds bounds = null;
        for (OsmPrimitive osmPrimitive : dataSet.allPrimitives()) {
            bounds = mergeBounds(bounds, osmPrimitive);
        }
        return new BoundedDataSetChangestes(dataSet, bounds);
    }

    private void processPoint(final JsonObject tags, final LatLon latLon, final String action) {
        final Node node = createNode(latLon);
        fillTagsFromFeature(tags, node, action);
    }

    private void processLineString(final JsonObject tags, final JsonArray arrayNodes, final String action) {
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

}
