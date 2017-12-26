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
import org.exoplatform.injection.core.module.*;
import org.exoplatform.injection.services.DataInjector;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

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

/**
  @Inject
  DocumentService                 documentService_;
*/
  ActivityModule activityModule;

  CalendarModule calendarModule;

  DocumentModule documentModule;

  ForumModule forumModule;

  SpaceModule spaceModule;

  UserModule userModule;

  WikiModule wikiModule;

  DataInjector dataInjector;


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
  @Inject
  public PopulatorService(DataInjector dataInjector, ActivityModule activityModule, CalendarModule calendarModule, DocumentModule documentModule, ForumModule forumModule, SpaceModule spaceModule, UserModule userModule, WikiModule wikiModule) {
    this.activityModule = activityModule;
    this.calendarModule = calendarModule;
    this.documentModule = documentModule;
    this.forumModule = forumModule;
    this.spaceModule = spaceModule;
    this.userModule = userModule;
    this.wikiModule = wikiModule;
    this.dataInjector = dataInjector;
    init();
  }
  /**
   * Inits the.
   */
  public void init() {
    scenarios = dataInjector.setup("injector-dataset");
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
        userModule.createUsers(scenarioData.getJSONArray("users"),"injector-dataset");
        this.setCompletion(this.USERS, 100);

      }
      if (scenarioData.has("relations")) {
        LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
        userModule.createRelations(scenarioData.getJSONArray("relations"));
      }
      if (scenarioData.has("spaces")) {
        LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
        spaceModule.createSpaces(scenarioData.getJSONArray("spaces"),"injector-dataset");
        this.setCompletion(this.SPACES, 100);
      }
      if (scenarioData.has("calendars")) {
        LOG.info("Create " + scenarioData.getJSONArray("calendars").length() + " calendars.");
        calendarModule.setCalendarColors(scenarioData.getJSONArray("calendars"));
        this.setCompletion(this.CALENDAR,100);
        calendarModule.createEvents(scenarioData.getJSONArray("calendars"));
        this.setCompletion(this.CALENDAR,100);
      }
      if (scenarioData.has("wikis")) {
        LOG.info("Create " + scenarioData.getJSONArray("wikis").length() + " wikis.");
        wikiModule.createUserWiki(scenarioData.getJSONArray("wikis"),"injector-dataset");
        this.setCompletion(this.WIKI, 100);
      }
      if (scenarioData.has("activities")) {

        LOG.info("Create " + scenarioData.getJSONArray("activities").length() + " activities.");
        activityModule.pushActivities(scenarioData.getJSONArray("activities"));
        this.setCompletion(this.ACTIVITIES,100);
      }
      if (scenarioData.has("documents")) {
        LOG.info("Create " + scenarioData.getJSONArray("documents").length() + " documents.");
        documentModule.uploadDocuments(scenarioData.getJSONArray("documents"),"injector-dataset");
        this.setCompletion(this.DOCUMENTS, 100);
      }
      if (scenarioData.has("forums")) {
        forumModule.createForumContents(scenarioData.getJSONArray("forums"));
        this.setCompletion(this.FORUM, 100);
      }

      if (scenarios.get(scenarioName).has("scriptData")) {
        downloadUrl = documentModule.storeScript(scenarios.get(scenarioName).getString("scriptData"),"injector-dataset");
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
