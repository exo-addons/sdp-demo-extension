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
package org.exoplatform.addons.sdpDemo.populator.portlet.populator;

import java.util.Collection;

import javax.inject.Inject;

import org.json.JSONObject;

import org.exoplatform.addons.sdpDemo.populator.services.PopulatorService;

import juzu.*;
import juzu.plugin.ajax.Ajax;
import juzu.template.Template;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 04/02/16.
 */
@SessionScoped
public class Populator {

  /** . */
  @Inject
  @Path("index.gtmpl")
  Template         index;

  /** The populator service. */
  @Inject
  PopulatorService populatorService_;

  /**
   * Index.
   *
   * @return the response. content
   */
  @View
  public Response.Content index() {
    Collection<JSONObject> scenarios = populatorService_.getScenarios().values();
    return index.with().set("scenarios", scenarios).ok();
  }

  /**
   * Populate.
   *
   * @param scenarioName the scenario name
   * @return the response. content
   */
  @Ajax
  @Resource
  public Response.Content populate(String scenarioName) {
    String downloadUrl = populatorService_.populate(scenarioName);
    StringBuilder sb = new StringBuilder();
    sb.append("{\"status\": \"OK\",\"downloadUrl\": \"" + downloadUrl + "\"}");

    return Response.ok(sb.toString()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  /**
   * Elements.
   *
   * @return the response. content
   */
  @Ajax
  @Resource
  public Response.Content elements() {
    return Response.ok(populatorService_.getCompletionAsJson())
                   .withMimeType("application/json; charset=UTF-8")
                   .withHeader("Cache-Control", "no-cache");
  }

}
