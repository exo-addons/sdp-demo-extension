package org.exoplatform.addons.sdpDemo.populator.services;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.ajax.JSON;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 04/02/16.
 */
public class PopulatorService {

    private final Log LOG = ExoLogger.getLogger(PopulatorService.class);
    public String SCENARIO_FOLDER = "/scenarios";
    public String SCENARIO_NAME_ATTRIBUTE = "scenarioName";
    public String SCENARIO_DESCRIPTION_ATTRIBUTE = "scenarioName";

    @Inject
    UserService userService_;

    @Inject
    SpaceService spaceService_;

    @Inject
    CalendarService calendarService_;

    /*@Inject
    WikiService wikiService_;

    @Inject
    ForumService forumService_;

    @Inject
    DocumentService documentService_;

    @Inject
    ActivityService activityService_;*/

    private Map<String, JSONObject> scenarios;


    public PopulatorService() {
        init();
    }

    public void init() {
        scenarios = new HashMap<String, JSONObject>();
        try {
            File folder = new File(PopulatorService.class.getClassLoader().getResource(SCENARIO_FOLDER).toURI());

            for (String fileName : folder.list()) {
                InputStream stream = PopulatorService.class.getClassLoader().getResourceAsStream(SCENARIO_FOLDER + "/" + fileName);
                String fileContent = getData(stream);
                try {
                    JSONObject json = new JSONObject(fileContent);
                    String name = json.getString(SCENARIO_NAME_ATTRIBUTE);
                    scenarios.put(name, json);
                } catch (JSONException e) {
                    LOG.error("Syntax error in scenario " + fileName, e);
                }
            }
        } catch (URISyntaxException e) {
            LOG.error("Unable to read scenario file", e);
        }
    }


    public void populate(String scenarioName) {

        try {
            JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
            if (scenarioData.has("users")) {
                userService_.createUsers(scenarioData.getJSONArray("users"));
            }
            if (scenarioData.has("relations")) {
                userService_.createRelations(scenarioData.getJSONArray("relations"));
            }
            if (scenarioData.has("spaces")) {
                spaceService_.createSpaces(scenarioData.getJSONArray("spaces"));
            }
            if (scenarioData.has("calendars")) {
                calendarService_.setCalendarColors(scenarioData.getJSONArray("calendars"));
                calendarService_.createEvents(scenarioData.getJSONArray("calendars"));
            }
        } catch (JSONException e) {
            LOG.error("Syntax error when reading scenario "+scenarioName,e);
        }

    }

    public String getData(InputStream inputStream) {
        String out = "";
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer);
            out = writer.toString();

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return out;
    }

    public Map<String, JSONObject> getScenarios() {
        return scenarios;
    }
}
