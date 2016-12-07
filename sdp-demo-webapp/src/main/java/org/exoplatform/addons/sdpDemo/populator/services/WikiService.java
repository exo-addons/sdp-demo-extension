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

import juzu.SessionScoped;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwiki.rendering.syntax.Syntax;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The Class WikiService.
 */
@Named("wikiService")
@SessionScoped
public class WikiService {

  /** The wiki service. */
  org.exoplatform.wiki.service.WikiService wikiService_;

  /** The log. */
  private final Log                        LOG = ExoLogger.getLogger(WikiService.class);

  /**
   * Instantiates a new wiki service.
   *
   * @param wikiService the wiki service
   */
  @Inject
  public WikiService(org.exoplatform.wiki.service.WikiService wikiService) {
    wikiService_ = wikiService;
  }

  /**
   * Creates the user wiki.
   *
   * @param wikis the wikis
   * @param populatorService_ the populator service
   */
  public void createUserWiki(JSONArray wikis, PopulatorService populatorService_) {
    for (int i = 0; i < wikis.length(); i++) {
      try {
        JSONObject wiki = wikis.getJSONObject(i);
        createOrEditPage(wiki, wiki.has("parent") ? wiki.getString("parent") : "");
        populatorService_.setCompletion(populatorService_.WIKI, ((i + 1) * 100) / wikis.length());
      } catch (JSONException e) {
        LOG.error("Syntax error on wiki n°" + i, e);

      }
    }
  }

  /**
   * Creates the or edit page.
   *
   * @param wiki the wiki
   * @param parentTitle the parent title
   * @throws JSONException the JSON exception
   */
  private void createOrEditPage(JSONObject wiki, String parentTitle) throws JSONException {
    boolean forceNew = wiki.has("new") && wiki.getBoolean("new");
    String title = wiki.getString("title");
    String filename = wiki.has("filename") ? wiki.getString("filename") : "";
    String parent = parentTitle;
    String type = wiki.has("type") ? wiki.getString("type") : "";
    if ("group".equals(type)) {
      type = PortalConfig.GROUP_TYPE;
    } else if ("portal".equals(type)) {
      type = PortalConfig.PORTAL_TYPE;
    } else {
      type = PortalConfig.USER_TYPE;
    }
    String owner = wiki.has("owner") ? wiki.getString("owner") : "";

    try {
      // does wiki exists ?
      if (wikiService_.getWikiByTypeAndOwner(type, owner) == null) {
        wikiService_.createWiki(type, owner);
      }

      if (forceNew && !title.equals("WikiHome")) {
        if (wikiService_.isExisting(type, owner, TitleResolver.getId(title, false))) {
          wikiService_.deletePage(type, owner, TitleResolver.getId(title, false));
        }
      }

      Page page;
      if (wikiService_.isExisting(type, owner, TitleResolver.getId(title, false))) {
        page = wikiService_.getPageOfWikiByName(type, owner, TitleResolver.getId(title, false));
      } else {
        page = wikiService_.createPage(new Wiki(type, owner), TitleResolver.getId(parent, false), new Page(title, title));
      }

      String content = "= " + title + " =";
      if (filename != null && !filename.equals(""))
        content = Utils.getWikiPage(filename);
      page.setContent(content);
      page.setSyntax(Syntax.XWIKI_2_1.toIdString());
      wikiService_.updatePage(page, null);
      // wikiService_.createVersionOfPage(page);

      if (wiki.has("wikis") && wiki.getJSONArray("wikis").length() > 0) {
        for (int j = 0; j < wiki.getJSONArray("wikis").length(); j++) {
          JSONObject childWiki = wiki.getJSONArray("wikis").getJSONObject(j);
          createOrEditPage(childWiki, wiki.getString("title"));
        }
      }

    } catch (WikiException e) {
      LOG.error("Error when creating wiki page", e); // To change body of catch statement use File | Settings
                                                     // | File Templates.
    } catch (IOException e) {
      LOG.error("Error when reading wiki content", e);
    }

  }

}
