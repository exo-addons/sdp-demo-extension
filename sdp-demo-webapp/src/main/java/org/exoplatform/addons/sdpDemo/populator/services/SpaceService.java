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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The Class SpaceService.
 */
@Named("spaceService")
@SessionScoped
public class SpaceService {

  /** The space service. */
  org.exoplatform.social.core.space.spi.SpaceService spaceService_;

  /** The identity manager. */
  IdentityManager                                    identityManager_;

  /** The log. */
  private final Log                                  LOG = ExoLogger.getLogger(SpaceService.class);

  /**
   * Instantiates a new space service.
   *
   * @param spaceService the space service
   * @param identityManager the identity manager
   */
  @Inject
  public SpaceService(org.exoplatform.social.core.space.spi.SpaceService spaceService, IdentityManager identityManager) {
    spaceService_ = spaceService;
    identityManager_ = identityManager;
  }

  /**
   * Creates the spaces.
   *
   * @param spaces the spaces
   * @param populatorService_ the populator service
   */
  public void createSpaces(JSONArray spaces, PopulatorService populatorService_) {
    for (int i = 0; i < spaces.length(); i++) {

      try {
        JSONObject space = spaces.getJSONObject(i);
        createSpace(space.getString("displayName"), space.getString("creator"));
        if (space.has("members")) {
          JSONArray members = space.getJSONArray("members");
          for (int j = 0; j < members.length(); j++) {
            Space spacet = spaceService_.getSpaceByDisplayName(space.getString("displayName"));
            if (spacet != null) {
              spaceService_.addMember(spacet, members.getString(j));
            }

          }
        }
        createSpaceAvatar(space.getString("displayName"), space.getString("creator"), space.getString("avatar"));
        populatorService_.setCompletion(populatorService_.SPACES, ((i + 1) * 100) / spaces.length());

      } catch (JSONException e) {
        LOG.error("Syntax error on space n°" + i, e);
      }
    }
  }

  /**
   * Creates the space avatar.
   *
   * @param name the name
   * @param editor the editor
   * @param avatarFile the avatar file
   */
  private void createSpaceAvatar(String name, String editor, String avatarFile) {
    Space space = spaceService_.getSpaceByDisplayName(name);
    if (space != null) {
      try {
        AvatarAttachment avatarAttachment = Utils.getAvatarAttachment(avatarFile);
        space.setAvatarAttachment(avatarAttachment);
        spaceService_.updateSpace(space);
        space.setEditor(editor);
        spaceService_.updateSpaceAvatar(space);
      } catch (Exception e) {
        LOG.error("Unable to set avatar for space " + space.getDisplayName(), e.getMessage());
      }
    }
  }

  /**
   * Creates the space.
   *
   * @param name the name
   * @param creator the creator
   */
  private void createSpace(String name, String creator) {
    Space target = spaceService_.getSpaceByDisplayName(name);
    if (target != null) {
      return;
    }

    Space space = new Space();
    // space.setId(name);
    space.setDisplayName(name);
    space.setPrettyName(name);
    space.setDescription(StringUtils.EMPTY);
    space.setGroupId("/spaces/" + space.getPrettyName());
    space.setRegistration(Space.OPEN);
    space.setVisibility(Space.PRIVATE);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);

    Identity identity = identityManager_.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), true);
    if (identity != null) {
      space.setPrettyName(SpaceUtils.buildPrettyName(space));
    }
    space.setType(DefaultSpaceApplicationHandler.NAME);

    spaceService_.createSpace(space, creator);

  }

}
