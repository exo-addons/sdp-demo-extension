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


import javax.inject.Inject;
import javax.inject.Named;

import juzu.SessionScoped;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class ActivityService.
 */
@Named("activityService")
@SessionScoped
public class ActivityService {

  /** The log. */
  private final Log LOG = ExoLogger.getLogger(ActivityService.class);


  /** The activity manager. */
  ActivityManager activityManager_;
  
  /** The identity manager. */
  IdentityManager identityManager_;

  /**
   * Instantiates a new activity service.
   *
   * @param activityManager the activity manager
   * @param identityManager the identity manager
   */
  @Inject
  public ActivityService(ActivityManager activityManager, IdentityManager identityManager)
  {
    activityManager_ = activityManager;
    identityManager_ = identityManager;
  }

  /**
   * Push activities.
   *
   * @param activities the activities
   * @param populatorService_ the populator service
   */
  public void pushActivities(JSONArray activities, PopulatorService populatorService_)
  {

    for (int i =0;i<activities.length();i++) {
      try {
        JSONObject activity = activities.getJSONObject(i);
        pushActivity(activity);
        populatorService_.setCompletion(populatorService_.ACTIVITIES,((i+1)*100)/activities.length());
      } catch (JSONException e) {
        LOG.error("Syntax error on activity n°" + i, e);

      } catch (Exception e) {
        LOG.error("Error when creating activity n°"+i,e);
      }
    }

    //likeRandomActivities(Utils.MARY);
    //likeRandomActivities(Utils.JAMES);
  }

  /**
   * Push activity.
   *
   * @param activityJSON the activity JSON
   * @throws Exception the exception
   */
  private void pushActivity(JSONObject activityJSON) throws Exception
  {

    String from = activityJSON.getString("from");
    Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, from, false);
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setBody(activityJSON.getString("body"));
    activity.setTitle(activityJSON.getString("body"));
    activity.setUserId(identity.getId());
    activity.setType("DEFAULT_ACTIVITY");
    // TODO cleanup
    //activity = activityManager_.saveActivity(identity, activity);
    activityManager_.saveActivityNoReturn(identity, activity);

    Thread.sleep(1000);
    JSONArray likes = activityJSON.getJSONArray("likes");

    for (int i=0;i<likes.length();i++)
    {
      String like = likes.getString(i);
      Identity identityLike = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, like, false);
      try {
        activityManager_.saveLike(activity, identityLike);
      } catch (Exception e) {
        LOG.error("Error when liking an activity with "+like,e);
      }
    }

    JSONArray comments = activityJSON.getJSONArray("comments");
    for (int i =0;i<comments.length();i++) {
      JSONObject commentJSON = comments.getJSONObject(i);

      Thread.sleep(1000);
      Identity identityComment = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, commentJSON.getString("from"), false);
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle(commentJSON.getString("body"));
      comment.setUserId(identityComment.getId());
      activityManager_.saveComment(activity, comment);
    }

  }

  /*
  private void likeRandomActivities (String username)
  {
    Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false);
    RealtimeListAccess rtla = activityManager_.getActivitiesWithListAccess(identity);
    ExoSocialActivity[] la = (ExoSocialActivity[])rtla.load(0, rtla.getSize());
    for (int iam = 0; iam<la.length ; iam++)
    {
      ExoSocialActivity activityMary = la[iam];
      boolean like = random.nextBoolean();
      if (like)
      {
        activityManager_.saveLike(activityMary, identity);
      }
    }
    rtla = activityManager_.getActivitiesOfUserSpacesWithListAccess(identity);
    la = (ExoSocialActivity[])rtla.load(0, rtla.getSize());
    for (int iam = 0; iam<la.length ; iam++)
    {
      ExoSocialActivity activityMary = la[iam];
      boolean like = random.nextBoolean();
      if (like)
      {
        activityManager_.saveLike(activityMary, identity);
      }
    }

  }*/

}
