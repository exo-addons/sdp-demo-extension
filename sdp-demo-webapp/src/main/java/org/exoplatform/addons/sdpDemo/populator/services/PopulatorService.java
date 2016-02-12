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

    @Inject
    WikiService wikiService_;

    @Inject
    ForumService forumService_;

    @Inject
    DocumentService documentService_;

    @Inject
    ActivityService activityService_;

    private Map<String, JSONObject> scenarios;

    Map<String, Integer> completion = new HashMap<String, Integer>();
    public final String USERS = "Users";
    public final String SPACES = "Spaces";
    public final String CALENDAR = "Calendar";
    public final String WIKI = "Wiki";
    public final String DOCUMENTS = "Documents";
    public final String FORUM = "Forum";
    public final String ACTIVITIES = "Activities";


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

        completion.put(USERS, 0);
        completion.put(SPACES, 0);
        completion.put(CALENDAR, 0);
        completion.put(WIKI, 0);
        completion.put(DOCUMENTS, 0);
        completion.put(FORUM, 0);
        completion.put(ACTIVITIES, 0);
    }


    public String populate(String scenarioName) {
        completion.put(USERS, 0);
        completion.put(SPACES, 0);
        completion.put(CALENDAR, 0);
        completion.put(WIKI, 0);
        completion.put(DOCUMENTS, 0);
        completion.put(FORUM, 0);
        completion.put(ACTIVITIES, 0);

        String downloadUrl ="";
        try {
            JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
            if (scenarioData.has("users")) {
                LOG.info("Create "+scenarioData.getJSONArray("users").length()+" users.");
                userService_.createUsers(scenarioData.getJSONArray("users"),this);

            }
            if (scenarioData.has("relations")) {
                LOG.info("Create "+scenarioData.getJSONArray("relations").length()+" relations.");
                userService_.createRelations(scenarioData.getJSONArray("relations"));
            }
            if (scenarioData.has("spaces")) {
                LOG.info("Create "+scenarioData.getJSONArray("spaces").length()+" spaces.");
                spaceService_.createSpaces(scenarioData.getJSONArray("spaces"), this);
            }
            if (scenarioData.has("calendars")) {
                LOG.info("Create "+scenarioData.getJSONArray("calendars").length()+" calendars.");
                calendarService_.setCalendarColors(scenarioData.getJSONArray("calendars"),this);
                calendarService_.createEvents(scenarioData.getJSONArray("calendars"),this);
            }
            if (scenarioData.has("wikis")) {
                LOG.info("Create "+scenarioData.getJSONArray("wikis").length()+" wikis.");
                wikiService_.createUserWiki(scenarioData.getJSONArray("wikis"), this);
            }
            if (scenarioData.has("activities")) {

                LOG.info("Create "+scenarioData.getJSONArray("activities").length()+" activities.");
                activityService_.pushActivities(scenarioData.getJSONArray("activities"), this);
            }
            if (scenarioData.has("documents")) {
                LOG.info("Create "+scenarioData.getJSONArray("documents").length()+" documents.");
                documentService_.uploadDocuments(scenarioData.getJSONArray("documents"),this);
            }
            if (scenarioData.has("forums")) {
                forumService_.createForumContents(scenarioData.getJSONArray("forums"), this);
            }

            if (scenarios.get(scenarioName).has("scriptData")) {
                downloadUrl = documentService_.storeScript(scenarios.get(scenarioName).getString("scriptData"));
            }


        } catch (JSONException e) {
            LOG.error("Syntax error when reading scenario "+scenarioName,e);
        }

        return downloadUrl;
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

    public void setCompletion(String category, int value) {
        completion.put(category,value);
    }

    public String getCompletionAsJson()
    {
        StringBuilder sb = new StringBuilder() ;
        sb.append("[");
        boolean first = true;
        for (String key:completion.keySet())
        {
            if (!first) sb.append(",");
            sb.append("{\"name\": \""+key+"\",");
            sb.append("\"percentage\": \""+completion.get(key)+"%\"}");
            first = false;
        }
        sb.append("]");

        return sb.toString();
    }

}
