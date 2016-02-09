package org.exoplatform.addons.sdpDemo.populator.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import juzu.SessionScoped;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.web.controller.regexp.RENode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Named("documentService")
@SessionScoped
public class DocumentService {

  private final Log LOG = ExoLogger.getLogger(DocumentService.class);

  RepositoryService repositoryService_;
  SessionProviderService sessionProviderService_;
  NodeHierarchyCreator nodeHierarchyCreator_;
  public static String FILE_CREATED_ACTIVITY         = "ActivityNotify.event.FileCreated";
  ListenerService listenerService_;
  OrganizationService organizationService_;

  @Inject
  public DocumentService(RepositoryService repositoryService, SessionProviderService sessionProviderService, NodeHierarchyCreator nodeHierarchyCreator, ListenerService listenerService, OrganizationService organizationService)
  {
    repositoryService_ = repositoryService;
    sessionProviderService_ = sessionProviderService;
    nodeHierarchyCreator_= nodeHierarchyCreator;
    listenerService_ = listenerService;
    organizationService_=organizationService;
  }


  public void uploadDocuments(JSONArray documents)
  {
    for (int i =0;i<documents.length();i++) {
      try {
        JSONObject document = documents.getJSONObject(i);
        String filename=document.getString("filename");
        String owner=document.getString("owner");
        String path = document.has("path") ? document.getString("path") : null;
        boolean isPrivate = document.getBoolean("isPrivate");
        String spaceName = document.has("spaceName") ? document.getString("spaceName") : "";
        storeFile(filename,spaceName,isPrivate,null,owner,path,"collaboration");
         //createOrEditPage(wiki, wiki.has("parent") ? wiki.getString("parent") : "");
      } catch (JSONException e) {
        LOG.error("Syntax error on document n°" + i, e);

      }
    }
  }

  protected void storeFile(String filename, String name, boolean isPrivateContext, String uuid, String username, String path, String workspace)
  {
    SessionProvider sessionProvider = null;
    if (!"root".equals(username)) {
      sessionProvider = startSessionAs(username);
    } else {
      sessionProvider = SessionProvider.createSystemProvider();
    }

    try
    {
      //get info
      Session session = sessionProvider.getSession(workspace, repositoryService_.getCurrentRepository());

      Node homeNode;

      if (isPrivateContext)
      {
        Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, username);
        homeNode = userNode.getNode("Private");
      }
      else
      {
        Node rootNode = session.getRootNode();
        homeNode = rootNode.getNode(getSpacePath(name));
      }

      Node docNode = homeNode.getNode("Documents");

      if (path!=null)
      {
        Node rootNode = session.getRootNode();
        docNode = rootNode.getNode(path.substring(1));
      }

      if (!docNode.hasNode(filename) && (uuid==null || "---".equals(uuid)))
      {
        Node fileNode = docNode.addNode(filename, "nt:file");
        Node jcrContent = fileNode.addNode("jcr:content", "nt:resource");
        InputStream inputStream = Utils.getFile(filename);
        jcrContent.setProperty("jcr:data", inputStream);
        jcrContent.setProperty("jcr:lastModified", Calendar.getInstance());
        jcrContent.setProperty("jcr:encoding", "UTF-8");
        if (filename.endsWith(".jpg"))
          jcrContent.setProperty("jcr:mimeType", "image/jpeg");
        else if (filename.endsWith(".png"))
          jcrContent.setProperty("jcr:mimeType", "image/png");
        else if (filename.endsWith(".pdf"))
          jcrContent.setProperty("jcr:mimeType", "application/pdf");
        else if (filename.endsWith(".doc"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-word");
        else if (filename.endsWith(".xls"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-excel");
        else if (filename.endsWith(".ppt"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-powerpoint");
        else if (filename.endsWith(".docx"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        else if (filename.endsWith(".xlsx"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        else if (filename.endsWith(".pptx"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        else if (filename.endsWith(".odp"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.presentation");
        else if (filename.endsWith(".odt"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
        else if (filename.endsWith(".ods"))
          jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.spreadsheet");
        session.save();
        if (!"root".equals(username)) {
          listenerService_.broadcast(FILE_CREATED_ACTIVITY, null, fileNode);
        }

      }


    }
    catch (Exception e)
    {
      System.out.println("JCR::" + e.getMessage());
    }
    endSession();
  }

  protected void storeVideos(String filename, String name, boolean isPrivateContext, String uuid, String username, String path, String workspace, String type) {

    SessionProvider sessionProvider = startSessionAs(username);

    try {
      //get info
      Session session = sessionProvider.getSession(workspace, repositoryService_.getCurrentRepository());

      Node homeNode;

      Node rootNode = session.getRootNode();

      homeNode = rootNode.getNode(getSpacePath(name));

      Node docNode = homeNode.getNode("Documents");

      if (!docNode.hasNode(filename) && (uuid==null || "---".equals(uuid))) {
        Node fileNode = docNode.addNode(filename, "nt:file");
        Node jcrContent = fileNode.addNode("jcr:content", "nt:resource");
        InputStream inputStream = Utils.getFile(filename);
        jcrContent.setProperty("jcr:data", inputStream);
        jcrContent.setProperty("jcr:lastModified", Calendar.getInstance());
        jcrContent.setProperty("jcr:encoding", "UTF-8");
        if (filename.endsWith(".mp4")) {
          jcrContent.setProperty("jcr:mimeType", "video/mp4");
        }
        session.save();
        if (!"root".equals(name)) {
          listenerService_.broadcast(FILE_CREATED_ACTIVITY, null, fileNode);
        }

      }


    }
    catch (Exception e)
    {
      System.out.println("JCR::" + e.getMessage());
    }
    endSession();
  }

  private static String getSpacePath(String space)
  {
    return "Groups/spaces/"+space;
  }


  protected SessionProvider startSessionAs(String user) {
    Identity identity = new Identity(user);

    try {
      Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();

      Collection<Membership> memberships = organizationService_.getMembershipHandler().findMembershipsByUser(user);
      for (Membership membership : memberships) {
        membershipEntries.add(new MembershipEntry(membership.getGroupId(), membership.getMembershipType()));
      }
      identity.setMemberships(membershipEntries);
    } catch (Exception e) {
      LOG.info(e.getMessage());
    }
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
    sessionProviderService_.setSessionProvider(null, new SessionProvider(state));
    return sessionProviderService_.getSessionProvider(null);
  }

  protected void endSession() {
    sessionProviderService_.removeSessionProvider(null);
    ConversationState.setCurrent(null);
  }
}
