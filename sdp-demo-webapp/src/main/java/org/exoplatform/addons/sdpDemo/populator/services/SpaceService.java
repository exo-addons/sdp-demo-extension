package org.exoplatform.addons.sdpDemo.populator.services;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

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

@Named("spaceService")
@SessionScoped
public class SpaceService {

  org.exoplatform.social.core.space.spi.SpaceService spaceService_;
  IdentityManager identityManager_;

  private final Log LOG = ExoLogger.getLogger(SpaceService.class);


  @Inject
  public SpaceService(org.exoplatform.social.core.space.spi.SpaceService spaceService, IdentityManager identityManager)
  {
    spaceService_ = spaceService;
    identityManager_ = identityManager;
  }

  public void createSpaces(JSONArray spaces, PopulatorService populatorService_)
  {
    for (int i = 0; i < spaces.length(); i++) {

      try {
        JSONObject space = spaces.getJSONObject(i);
        createSpace(space.getString("displayName"), space.getString("prettyName"), space.getString("creator"));
        if (space.has("members")) {
          JSONArray  members = space.getJSONArray("members");
          for (int j = 0; j < members.length(); j++) {
            Space spacet = spaceService_.getSpaceByDisplayName(space.getString("displayName"));
            if (spacet != null) {
              spaceService_.addMember(spacet, members.getString(j));
            }

          }
        }
        createSpaceAvatar(space.getString("displayName"), space.getString("creator"), space.getString("avatar"));
        populatorService_.setCompletion(populatorService_.SPACES,((i+1)*100)/spaces.length());

      }catch (JSONException e) {
        LOG.error("Syntax error on space n°" + i, e);
      }
    }
  }

  private void createSpaceAvatar(String name, String editor, String avatarFile)
  {
    Space space = spaceService_.getSpaceByDisplayName(name);
    if (space!=null)
    {
      try {
        AvatarAttachment avatarAttachment = Utils.getAvatarAttachment(avatarFile);
        space.setAvatarAttachment(avatarAttachment);
        spaceService_.updateSpace(space);
        space.setEditor(editor);
        spaceService_.updateSpaceAvatar(space);
      } catch (Exception e) {
        LOG.error("Unable to set avatar for space "+space.getDisplayName(),e.getMessage());
      }
    }
  }

  private void createSpace(String name, String prettyName, String creator)
  {
    Space target = spaceService_.getSpaceByDisplayName(name);
    if (target!=null)
    {
      return;
    }

    Space space = new Space();
//    space.setId(name);
    space.setDisplayName(name);
    space.setPrettyName(prettyName);
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
