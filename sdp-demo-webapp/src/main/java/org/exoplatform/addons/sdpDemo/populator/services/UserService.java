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

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.webui.exception.MessageException;

import juzu.SessionScoped;

/**
 * The Class UserService.
 */
@Named("userService")
@SessionScoped
public class UserService {

  /** The Constant PLATFORM_USERS_GROUP. */
  private final static String PLATFORM_USERS_GROUP    = "/platform/administrators";

  /** The Constant MEMBERSHIP_TYPE_MANAGER. */
  private final static String MEMBERSHIP_TYPE_MANAGER = "*";

  /** The Constant WIDTH. */
  private final static int    WIDTH                   = 200;

  /** The log. */
  private final Log           LOG                     = ExoLogger.getLogger(UserService.class);

  /** The organization service. */
  OrganizationService         organizationService_;

  /** The user handler. */
  UserHandler                 userHandler_;

  /** The identity manager. */
  IdentityManager             identityManager_;

  /** The relationship manager. */
  RelationshipManager         relationshipManager_;

  /**
   * Instantiates a new user service.
   *
   * @param organizationService the organization service
   * @param identityManager the identity manager
   * @param relationshipManager the relationship manager
   */
  @Inject
  public UserService(OrganizationService organizationService,
                     IdentityManager identityManager,
                     RelationshipManager relationshipManager) {
    organizationService_ = organizationService;
    userHandler_ = organizationService_.getUserHandler();
    identityManager_ = identityManager;
    relationshipManager_ = relationshipManager;
  }

  /**
   * Creates the users.
   *
   * @param users the users
   * @param populatorService_ the populator service
   */
  public void createUsers(JSONArray users, String scenario, PopulatorService populatorService_) {

    for (int i = 0; i < users.length(); i++) {
      try {
        JSONObject user = users.getJSONObject(i);
        createUser(user.getString("username"),
                   user.getString("position"),
                   user.getString("firstname"),
                   user.getString("lastname"),
                   user.getString("email"),
                   user.getString("password"),
                   user.getString("isadmin"));
        saveUserAvatar(user.getString("username"), user.getString("avatar"), scenario);
        populatorService_.setCompletion(populatorService_.USERS, ((i + 1) * 100) / users.length());

      } catch (JSONException e) {
        LOG.error("Syntax error on user n°" + i, e);
      }
    }

  }

  /**
   * Creates the user.
   *
   * @param username the username
   * @param position the position
   * @param firstname the firstname
   * @param lastname the lastname
   * @param email the email
   * @param password the password
   * @param isAdmin the is admin
   * @return true, if successful
   */
  private boolean createUser(String username,
                             String position,
                             String firstname,
                             String lastname,
                             String email,
                             String password,
                             String isAdmin) {
    Boolean ok = true;

    User user = null;
    try {
      user = userHandler_.findUserByName(username);
    } catch (Exception e) {
      LOG.info(e.getMessage());
    }

    if (user != null) {
      return true;
    }

    user = userHandler_.createUserInstance(username);
    user.setDisplayName(firstname + " " + lastname);
    user.setEmail(email);
    user.setFirstName(firstname);
    user.setLastName(lastname);
    user.setPassword(password);

    try {
      userHandler_.createUser(user, true);
    } catch (Exception e) {
      LOG.info(e.getMessage());
      ok = false;
    }

    if (isAdmin != null && isAdmin.equals("true")) {
      // Assign the membership "*:/platform/administrators" to the created user
      try {
        Group group = organizationService_.getGroupHandler().findGroupById(PLATFORM_USERS_GROUP);
        MembershipType membershipType =
                                      organizationService_.getMembershipTypeHandler().findMembershipType(MEMBERSHIP_TYPE_MANAGER);
        organizationService_.getMembershipHandler().linkMembership(user, group, membershipType, true);
      } catch (Exception e) {
        LOG.warn("Can not assign *:/platform/administrators membership to the created user");
        ok = false;
      }

    }

    if (!"".equals(position)) {
      Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true);
      if (identity != null) {
        Profile profile = identity.getProfile();
        profile.setProperty(Profile.POSITION, position);
        profile.setListUpdateTypes(Arrays.asList(Profile.UpdateType.CONTACT));
        try {
          identityManager_.updateProfile(profile);
        } catch (MessageException e) {
          e.printStackTrace();
        }
      }
    }

    return ok;
  }

  /**
   * Save user avatar.
   *
   * @param username the username
   * @param fileName the file name
   */
  private void saveUserAvatar(String username, String fileName, String scenario) {
    try {

      AvatarAttachment avatarAttachment = Utils.getAvatarAttachment(fileName, scenario);
      Profile p = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true).getProfile();
      if (avatarAttachment != null)
        p.setProperty(Profile.AVATAR, avatarAttachment);
      p.setListUpdateTypes(Arrays.asList(Profile.UpdateType.AVATAR));

      Map<String, Object> props = p.getProperties();

      // Removes avatar url and resized avatar
      for (String key : props.keySet()) {
        if (key.startsWith(Profile.AVATAR + ImageUtils.KEY_SEPARATOR)) {
          p.removeProperty(key);
        }
      }

      identityManager_.updateProfile(p);

    } catch (Exception e) {
      LOG.info(e.getMessage());
    }
  }

  /**
   * Creates the relations.
   *
   * @param relations the relations
   */
  public void createRelations(JSONArray relations) {
    for (int i = 0; i < relations.length(); i++) {

      try {
        JSONObject relation = relations.getJSONObject(i);
        Identity idInviting = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                   relation.getString("inviting"),
                                                                   false);
        Identity idInvited = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                  relation.getString("invited"),
                                                                  false);
        relationshipManager_.inviteToConnect(idInviting, idInvited);
        if (relation.has("confirmed") && relation.getBoolean("confirmed")) {
          relationshipManager_.confirm(idInvited, idInviting);
        }
      } catch (JSONException e) {
        LOG.error("Syntax error on relation n°" + i, e);
      }
    }
  }
}
