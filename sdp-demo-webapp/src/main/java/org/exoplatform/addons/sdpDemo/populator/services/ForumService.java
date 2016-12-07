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

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.common.jcr.PropertyReader;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * The Class ForumService.
 */
@Named("forumService")
@SessionScoped
public class ForumService {

  /** The log. */
  private final Log                          LOG = ExoLogger.getLogger(ForumService.class);

  /** The forum service. */
  org.exoplatform.forum.service.ForumService forumService_;

  /** The poll service. */
  PollService                                pollService_;

  /** The locator. */
  KSDataLocation                             locator_;

  /**
   * Instantiates a new forum service.
   *
   * @param forumService the forum service
   * @param pollService the poll service
   * @param locator the locator
   */
  @Inject
  public ForumService(org.exoplatform.forum.service.ForumService forumService,
                      PollService pollService,
                      KSDataLocation locator) {
    forumService_ = forumService;
    pollService_ = pollService;
    locator_ = locator;
  }

  /**
   * Creates the forum contents.
   *
   * @param forumContent the forum content
   * @param populatorService_ the populator service
   */
  public void createForumContents(JSONArray forumContent, PopulatorService populatorService_) {

    for (int i = 0; i < forumContent.length(); i++) {

      try {
        JSONObject category = forumContent.getJSONObject(i);
        createCategory(category);

        populatorService_.setCompletion(populatorService_.FORUM, ((i + 1) * 100) / forumContent.length());
      } catch (JSONException e) {
        LOG.error("Syntax error on space n°" + i, e);
      }
    }

  }

  /**
   * Creates the category.
   *
   * @param categoryJSON the category JSON
   * @throws JSONException the JSON exception
   */
  public void createCategory(JSONObject categoryJSON) throws JSONException {
    String title = categoryJSON.getString("categoryTitle");
    String description = categoryJSON.getString("description");

    String owner = categoryJSON.getString("owner");

    List<Category> categories = forumService_.getCategories();
    String categoryId = "";
    for (Category category : categories) {
      if (category.getCategoryName().equals(title)) {
        categoryId = category.getId();
        break;
      }
    }
    if (categoryId.equals("")) {
      Category category = new Category();
      category.setCategoryName(title);
      category.setDescription(description);
      category.setOwner(owner);
      try {
        forumService_.saveCategory(category, true);
        categoryId = category.getId();

      } catch (Exception e) {
        LOG.error("Exception when creating category " + title + " in forum", e);
      }
    }

    if (categoryJSON.has("forums")) {
      JSONArray forums = categoryJSON.getJSONArray("forums");
      for (int i = 0; i < forums.length(); i++) {

        try {
          JSONObject forum = forums.getJSONObject(i);
          createForum(forum, categoryId);
        } catch (JSONException e) {
          LOG.error("Syntax error on forum n°" + i, e);
        }
      }
    }

  }

  /**
   * Creates the forum.
   *
   * @param forumJSON the forum JSON
   * @param categoryId the category id
   * @throws JSONException the JSON exception
   */
  private void createForum(JSONObject forumJSON, String categoryId) throws JSONException {
    String title = forumJSON.getString("forumTitle");
    String description = forumJSON.getString("description");

    String owner = forumJSON.getString("owner");

    ForumFilter filter = new ForumFilter(categoryId, true);
    List<Forum> forums = forumService_.getForums(filter);

    String forumId = "";
    for (Forum forum : forums) {
      if (forum.getForumName().equals(title)) {
        forumId = forum.getId();
        break;
      }
    }
    if (forumId.equals("")) {
      Forum forum = new Forum();
      forum.setForumName(title);
      forum.setDescription(description);
      forum.setOwner(owner);

      try {
        forumService_.saveForum(categoryId, forum, true);
        forumId = forum.getId();

      } catch (Exception e) {
        LOG.error("Exception when creating forum " + title, e);
      }

    }
    if (forumJSON.has("topics")) {

      JSONArray topics = forumJSON.getJSONArray("topics");
      for (int i = 0; i < topics.length(); i++) {

        try {
          JSONObject topic = topics.getJSONObject(i);
          createTopic(topic, forumId, categoryId);
        } catch (JSONException e) {
          LOG.error("Syntax error on topic n°" + i, e);
        }
      }
    }

  }

  /**
   * Creates the topic.
   *
   * @param topicJSON the topic JSON
   * @param forumId the forum id
   * @param categoryId the category id
   * @throws JSONException the JSON exception
   */
  private void createTopic(JSONObject topicJSON, String forumId, String categoryId) throws JSONException {
    String title = topicJSON.getString("topicTitle");
    String content = topicJSON.getString("content");
    String owner = topicJSON.getString("owner");

    try {
      List<Topic> topics = forumService_.getTopics(categoryId, forumId);
      String topicId = "";
      for (Topic topic : topics) {
        if (topic.getTopicName().equals(title)) {
          topicId = topic.getId();
          break;

        }
      }
      if (topicId.equals("")) {
        Topic topic = new Topic();
        topic.setTopicName(title);
        topic.setDescription(content);
        topic.setOwner(owner);

        topic.setIcon("classNameIcon");

        try {
          forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
          topicId = topic.getId();

        } catch (Exception e) {
          LOG.error("Exception when creating topic " + title, e);
        }
      }
      if (topicJSON.has("posts")) {

        JSONArray posts = topicJSON.getJSONArray("posts");
        for (int i = 0; i < posts.length(); i++) {

          try {
            JSONObject post = posts.getJSONObject(i);
            createPost(post, topicId, forumId, categoryId, title);
          } catch (JSONException e) {
            LOG.error("Syntax error on forum n°" + i, e);
          }
        }
      }

    } catch (Exception e) {
      LOG.error("Error when reading topics lists");
    }

  }

  /**
   * Creates the post.
   *
   * @param postJSON the post JSON
   * @param topicId the topic id
   * @param forumId the forum id
   * @param categoryId the category id
   * @param topicTitle the topic title
   * @throws JSONException the JSON exception
   */
  private void createPost(JSONObject postJSON,
                          String topicId,
                          String forumId,
                          String categoryId,
                          String topicTitle) throws JSONException {
    String content = postJSON.getString("content");
    String owner = postJSON.getString("owner");

    try {

      PostFilter postFilter = new PostFilter(categoryId, forumId, topicId, null, null, null, null);
      ListAccess<Post> posts = forumService_.getPosts(postFilter);
      Post[] postArray = posts.load(0, posts.getSize());
      String postId = "";
      for (Post post : postArray) {
        if (post.getMessage().equals(content)) {
          postId = post.getId();
        }
      }

      if (postId.equals("")) {
        // post not founded, add it.
        Post newPost = new Post();
        newPost.setOwner(owner);
        newPost.setMessage(content);
        newPost.setName(topicTitle);

        forumService_.savePost(categoryId, forumId, topicId, newPost, true, new MessageBuilder());
      }

    } catch (Exception e) {
      LOG.error("Error when reading posts lists");
    }

  }

  /**
   * Creates the posts.
   *
   * @param username the username
   */
  public void createPosts(String username) {
    String forumName = "Public Discussions";
    try {
      Forum forum = getForumByName(forumName);
      Category cat = getCategoryByForumName(forumName);

      List<Topic> topics = forumService_.getTopics(cat.getId(), forum.getId());
      if (topics.size() > 0)
        return;

      Topic topicNew = new Topic();
      topicNew.setOwner(username);
      topicNew.setTopicName("General");
      topicNew.setCreatedDate(new Date());
      topicNew.setModifiedBy(username);
      topicNew.setModifiedDate(new Date());
      topicNew.setLastPostBy(username);
      topicNew.setLastPostDate(new Date());
      topicNew.setDescription("General Topic");
      topicNew.setPostCount(0);
      topicNew.setViewCount(0);
      topicNew.setIsNotifyWhenAddPost("");
      topicNew.setIsModeratePost(false);
      topicNew.setIsClosed(false);
      topicNew.setIsLock(false);
      topicNew.setIsWaiting(false);
      topicNew.setIsActive(true);
      topicNew.setIcon("classNameIcon");
      topicNew.setIsApproved(true);
      topicNew.setCanView(new String[] {});
      topicNew.setCanPost(new String[] {});

      forumService_.saveTopic(cat.getId(), forum.getId(), topicNew, true, false, new MessageBuilder());
    } catch (Exception e) {
    }
  }

  /**
   * Gets the forum by name.
   *
   * @param forumName the forum name
   * @return the forum by name
   * @throws Exception the exception
   */
  /*
   * public void createPollAndVote()
   * {
   * String forumName = "Public Discussions";
   * try {
   * List<Poll> polls = pollService_.getPagePoll();
   * for (Poll poll:polls)
   * {
   * pollService_.removePoll(poll.getId());
   * }
   * Forum forum = getForumByName(forumName);
   * Category cat = getCategoryByForumName(forumName);
   * List<Topic> topics = forumService_.getTopics(cat.getId(), forum.getId());
   * if (topics.size()>0) {
   * Topic topic = topics.get(0);
   * String[] options = {"It's amazing", "I love it", "I like it", "No opinion"};
   * String[] votes = {"50.0", "33.333336", "16.666668", "0.0"};
   * String[] userVotes = {org.exoplatform.addons.populator.services.Utils.JAMES+":2:0",
   * org.exoplatform.addons.populator.services.Utils.JOHN+":1:0",
   * org.exoplatform.addons.populator.services.Utils.MARY+":1:0"};
   * Poll poll = new Poll();
   * String pollPath = forum.getPath() + CommonUtils.SLASH + topic.getId();
   * String pollId = topic.getId().replace(Utils.TOPIC, Utils.POLL);
   * poll.setId(pollId);
   * poll.setParentPath(pollPath);
   * poll.setInTopic(true);
   * poll.setQuestion("Do you like our new Intranet?");
   * poll.setOption(options);
   * poll.setOwner(org.exoplatform.addons.populator.services.Utils.MARY);
   * poll.setIsMultiCheck(true);
   * poll.setShowVote(true);
   * poll.setIsAgainVote(true);
   * poll.setIsClosed(false);
   * poll.setTimeOut(0);
   * pollService_.savePoll(poll, true, false);
   * poll.setVote(votes);
   * poll.setUserVote(userVotes);
   * poll.setModifiedBy(org.exoplatform.addons.populator.services.Utils.MARY);
   * pollService_.savePoll(poll, true, true);
   * }
   * } catch (Exception e) {}
   * }
   */
  private Forum getForumByName(String forumName) throws Exception {
    StringBuffer sb = new StringBuffer(Utils.JCR_ROOT);
    sb.append("/").append(locator_.getForumCategoriesLocation()).append("//element(*,");
    sb.append(Utils.EXO_FORUM).append(")[jcr:like(exo:name, '%").append(forumName).append("%')]");

    NodeIterator iter = forumService_.search(sb.toString());
    if (iter.hasNext()) {
      Node forumNode = (Node) iter.next();

      Forum forum = new Forum();
      PropertyReader reader = new PropertyReader(forumNode);
      forum.setId(forumNode.getName());
      forum.setPath(forumNode.getPath());
      forum.setOwner(reader.string(Utils.EXO_OWNER));
      forum.setForumName(reader.string(Utils.EXO_NAME));
      forum.setViewer(reader.strings(Utils.EXO_VIEWER));

      return forum;
    }

    return null;
  }

  /**
   * Gets the category by forum name.
   *
   * @param forumName the forum name
   * @return the category by forum name
   * @throws Exception the exception
   */
  private Category getCategoryByForumName(String forumName) throws Exception {
    StringBuffer sb = new StringBuffer(Utils.JCR_ROOT);
    sb.append("/").append(locator_.getForumCategoriesLocation()).append("//element(*,");
    sb.append(Utils.EXO_FORUM).append(")[jcr:like(exo:name, '%").append(forumName).append("%')]");

    NodeIterator iter = forumService_.search(sb.toString());
    if (iter.hasNext()) {
      Node forumNode = (Node) iter.next();
      if (forumNode.getParent() != null) {
        Node cateNode = forumNode.getParent();
        Category cat = new Category(cateNode.getName());
        cat.setPath(cateNode.getPath());
        PropertyReader reader = new PropertyReader(cateNode);
        cat.setOwner(reader.string(Utils.EXO_OWNER));
        cat.setCategoryName(reader.string(Utils.EXO_NAME));
        return cat;
      }
    }

    return null;
  }
}
