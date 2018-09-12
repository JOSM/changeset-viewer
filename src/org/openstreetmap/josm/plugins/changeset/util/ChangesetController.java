package org.openstreetmap.josm.plugins.changeset.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import org.openstreetmap.josm.plugins.changeset.util.DataSetChangesetBuilder.BoundedChangesetDataSet;

/**
 *
 * @author ruben
 */
public class ChangesetController {
    
    public BoundedChangesetDataSet getChangeset(String changesetId) {
        DataSetChangesetBuilder builder = new DataSetChangesetBuilder();
        try {
            String url = Config.HOST + changesetId + ".json";
            String stringChangeset = Request.sendGET(url);
            if (stringChangeset == null) {
                return null;
            } else {
                return builder.build(stringChangeset);
            }
        } catch (IOException ex) {
            Logger.getLogger(ChangesetController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public ChangesetBeen[] getListChangeset() {
        ChangesetBeen[] changesets = new ChangesetBeen[75];
        try {
            String stringChangesets = Request.sendGET(Config.getHost());
            if (stringChangesets == null) {
                return changesets;
            } else {
                try (JsonReader jsonReader = Json.createReader(new StringReader(stringChangesets))) {
                    JsonObject jsonObject = jsonReader.readObject();
                    JsonArray jsonArray = jsonObject.getJsonArray("features");
                    int i = 0;
                    for (Iterator<JsonValue> it = jsonArray.iterator(); it.hasNext();) {
                        JsonValue value = it.next();
                        try (JsonReader jsonReader2 = Json.createReader(new StringReader(value.toString()))) {
                            ChangesetBeen changesetBeen = new ChangesetBeen();
                            JsonObject jsonChangeset = jsonReader2.readObject();
                            changesetBeen.setChangesetId(jsonChangeset.getInt("id"));
                            JsonObject properties = jsonChangeset.getJsonObject("properties");
                            changesetBeen.setUser(properties.getString("user"));
                            changesetBeen.setDelete(properties.getInt("delete"));
                            changesetBeen.setCreate(properties.getInt("create"));
                            changesetBeen.setModify(properties.getInt("modify"));
                            changesetBeen.setDate(properties.getString("date"));
                            changesets[i] = changesetBeen;
                            i++;
                        }
                    }
                    return changesets;
                } catch (NullPointerException | JsonParsingException ex) {
                    return changesets;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ChangesetController.class.getName()).log(Level.SEVERE, null, ex);
            return changesets;
        }
    }
}
