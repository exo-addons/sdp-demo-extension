package org.exoplatform.addons.sdpDemo.populator.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import juzu.SessionScoped;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Named("userService")
@SessionScoped
public class UserService {

  OrganizationService organizationService_;
  UserHandler userHandler_;
  IdentityManager identityManager_;
  RelationshipManager relationshipManager_;

  private final Log LOG = ExoLogger.getLogger(UserService.class);

  
  private final static String PLATFORM_USERS_GROUP = "/platform/administrators";
  private final static String MEMBERSHIP_TYPE_MANAGER = "*";
  private final static int WIDTH = 200;

  @Inject
  public UserService(OrganizationService organizationService, IdentityManager identityManager, RelationshipManager relationshipManager)
  {
    organizationService_ = organizationService;
    userHandler_ = organizationService_.getUserHandler();
    identityManager_ = identityManager;
    relationshipManager_ = relationshipManager;
  }

  public void createUsers(JSONArray users, PopulatorService populatorService_) {

    for (int i = 0; i < users.length(); i++) {
      try {
        JSONObject user = users.getJSONObject(i);
        createUser(user.getString("username"),user.getString("position"),
                user.getString("firstname"), user.getString("lastname"),
                user.getString("email"), user.getString("password"), user.getString("isadmin"));
        saveUserAvatar(user.getString("username"),user.getString("avatar"));
        populatorService_.setCompletion(populatorService_.USERS,((i+1)*100)/users.length());

      } catch (JSONException e) {
        LOG.error("Syntax error on user n°" + i, e);
      }
    }

  }

  private boolean createUser(String username, String position, String firstname, String lastname, String email, String password, String isAdmin)
  {
    Boolean ok = true;

    User user = null;
    try {
      user = userHandler_.findUserByName(username);
    } catch (Exception e) {
      LOG.info(e.getMessage());
    }

    if (user!=null)
    {
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
      // Assign the membership "*:/platform/administrators"  to the created user
      try {
        Group group = organizationService_.getGroupHandler().findGroupById(PLATFORM_USERS_GROUP);
        MembershipType membershipType = organizationService_.getMembershipTypeHandler().findMembershipType(MEMBERSHIP_TYPE_MANAGER);
        organizationService_.getMembershipHandler().linkMembership(user, group, membershipType, true);
      } catch (Exception e) {
        LOG.warn("Can not assign *:/platform/administrators membership to the created user");
        ok = false;
      }


    }

    if (!"".equals(position)) {
      Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true);
      if (identity!=null) {
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


  private void saveUserAvatar(String username, String fileName)
  {
    try
    {

      AvatarAttachment avatarAttachment = Utils.getAvatarAttachment(fileName);
      Profile p = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true).getProfile();
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

    }
    catch (Exception e)
    {
      LOG.info(e.getMessage());
    }
  }

  public void createRelations(JSONArray relations)
  {
    for (int i = 0; i < relations.length(); i++) {

      try {
        JSONObject relation = relations.getJSONObject(i);
        Identity idInviting = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, relation.getString("inviting"), false);
        Identity idInvited = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, relation.getString("invited"), false);
        relationshipManager_.inviteToConnect(idInviting, idInvited);
        if (relation.has("confirmed") && relation.getBoolean("confirmed")) {
          relationshipManager_.confirm(idInvited, idInviting);
        }
      }catch (JSONException e) {
        LOG.error("Syntax error on relation n°" + i, e);
      }
    }
  }
}
