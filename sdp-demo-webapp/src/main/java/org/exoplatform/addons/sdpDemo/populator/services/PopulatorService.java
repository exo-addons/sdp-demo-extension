/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.addons.sdpDemo.populator.services;

import org.apache.commons.io.IOUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 04/02/16.
 */
public class PopulatorService {

  /** The log. */
  private final Log               LOG                            = ExoLogger.getLogger(PopulatorService.class);

  /** The scenario folder. */
  public String                   SCENARIO_FOLDER                = "/scenarios";

  /** The scenario name attribute. */
  public String                   SCENARIO_NAME_ATTRIBUTE        = "scenarioName";

  /** The scenario description attribute. */
  public String                   SCENARIO_DESCRIPTION_ATTRIBUTE = "scenarioName";

  /** The user service. */
  @Inject
  UserService                     userService_;

  /** The space service. */
  @Inject
  SpaceService                    spaceService_;

  /** The calendar service. */
  @Inject
  CalendarService                 calendarService_;

  /** The wiki service. */
  @Inject
  WikiService                     wikiService_;

  /** The forum service. */
  @Inject
  ForumService                    forumService_;

  /** The document service. */
  @Inject
  DocumentService                 documentService_;

  /** The activity service. */
  @Inject
  ActivityService                 activityService_;

  /** The scenarios. */
  private Map<String, JSONObject> scenarios;

  /** The completion. */
  Map<String, Integer>            completion                     = new HashMap<String, Integer>();

  /** The users. */
  public final String             USERS                          = "Users";

  /** The spaces. */
  public final String             SPACES                         = "Spaces";

  /** The calendar. */
  public final String             CALENDAR                       = "Calendar";

  /** The wiki. */
  public final String             WIKI                           = "Wiki";

  /** The documents. */
  public final String             DOCUMENTS                      = "Documents";

  /** The forum. */
  public final String             FORUM                          = "Forum";

  /** The activities. */
  public final String             ACTIVITIES                     = "Activities";

  /**
   * Instantiates a new populator service.
   */
  public PopulatorService() {
    init();
  }

  /**
   * Inits the.
   */
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

  /**
   * Populate.
   *
   * @param scenarioName the scenario name
   * @return the string
   */
  public String populate(String scenarioName) {
    completion.put(USERS, 0);
    completion.put(SPACES, 0);
    completion.put(CALENDAR, 0);
    completion.put(WIKI, 0);
    completion.put(DOCUMENTS, 0);
    completion.put(FORUM, 0);
    completion.put(ACTIVITIES, 0);

    String downloadUrl = "";
    try {
      JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
      if (scenarioData.has("users")) {
        LOG.info("Create " + scenarioData.getJSONArray("users").length() + " users.");
        userService_.createUsers(scenarioData.getJSONArray("users"), this);

      }
      if (scenarioData.has("relations")) {
        LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
        userService_.createRelations(scenarioData.getJSONArray("relations"));
      }
      if (scenarioData.has("spaces")) {
        LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
        spaceService_.createSpaces(scenarioData.getJSONArray("spaces"), this);
      }
      if (scenarioData.has("calendars")) {
        LOG.info("Create " + scenarioData.getJSONArray("calendars").length() + " calendars.");
        calendarService_.setCalendarColors(scenarioData.getJSONArray("calendars"), this);
        calendarService_.createEvents(scenarioData.getJSONArray("calendars"), this);
      }
      if (scenarioData.has("wikis")) {
        LOG.info("Create " + scenarioData.getJSONArray("wikis").length() + " wikis.");
        wikiService_.createUserWiki(scenarioData.getJSONArray("wikis"), this);
      }
      if (scenarioData.has("activities")) {

        LOG.info("Create " + scenarioData.getJSONArray("activities").length() + " activities.");
        activityService_.pushActivities(scenarioData.getJSONArray("activities"), this);
      }
      if (scenarioData.has("documents")) {
        LOG.info("Create " + scenarioData.getJSONArray("documents").length() + " documents.");
        documentService_.uploadDocuments(scenarioData.getJSONArray("documents"), this);
      }
      if (scenarioData.has("forums")) {
        forumService_.createForumContents(scenarioData.getJSONArray("forums"), this);
      }

      if (scenarios.get(scenarioName).has("scriptData")) {
        downloadUrl = documentService_.storeScript(scenarios.get(scenarioName).getString("scriptData"));
      }

    } catch (JSONException e) {
      LOG.error("Syntax error when reading scenario " + scenarioName, e);
    }

    return downloadUrl;
  }

  /**
   * Gets the data.
   *
   * @param inputStream the input stream
   * @return the data
   */
  public String getData(InputStream inputStream) {
    String out = "";
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(inputStream, writer);
      out = writer.toString();

    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }

    return out;
  }

  /**
   * Gets the scenarios.
   *
   * @return the scenarios
   */
  public Map<String, JSONObject> getScenarios() {
    return scenarios;
  }

  /**
   * Sets the completion.
   *
   * @param category the category
   * @param value the value
   */
  public void setCompletion(String category, int value) {
    completion.put(category, value);
  }

  /**
   * Gets the completion as json.
   *
   * @return the completion as json
   */
  public String getCompletionAsJson() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (String key : completion.keySet()) {
      if (!first)
        sb.append(",");
      sb.append("{\"name\": \"" + key + "\",");
      sb.append("\"percentage\": \"" + completion.get(key) + "%\"}");
      first = false;
    }
    sb.append("]");

    return sb.toString();
  }

}
