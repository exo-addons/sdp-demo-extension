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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.services.deployment.DeploymentUtils;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import juzu.SessionScoped;

/**
 * The Class WcmService.
 */
@Named("WcmService")
@SessionScoped
public class WcmService {

  /** The log. */
  private final Log              LOG = ExoLogger.getLogger(WcmService.class);

  /** The session provider service. */
  private SessionProviderService sessionProviderService_;

  /** The repository service. */
  private RepositoryService      repositoryService_;

  /**
   * Instantiates a new WCM service.
   *
   * @param repositoryService the repository service
   * @param sessionProviderService the session provider service
   */
  @Inject
  public WcmService(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    repositoryService_ = repositoryService;
    sessionProviderService_ = sessionProviderService;
  }

  /**
   * Upload documents.
   *
   * @param webContents the documents
   * @param scenario the scenario
   * @param populatorService_ the populator service
   */
  public void uploadWebContents(JSONArray webContents, String scenario, PopulatorService populatorService_) {
    for (int i = 0; i < webContents.length(); i++) {
      try {
        JSONObject webcontent = webContents.getJSONObject(i);
        String filename = webcontent.has("filename") ? webcontent.getString("filename") : null;
        String sourcePath = Utils.getMediaPath(filename, Utils.CONTENT_TYPE, scenario);
        String targetPath = webcontent.has("targetPath") ? webcontent.getString("targetPath") : null;
        boolean cleanupPublication = webcontent.has("cleanupPublication") ? webcontent.getBoolean("cleanupPublication") : null;
        InputStream inputStream = Utils.getFile(sourcePath);
        importXML(sessionProviderService_.getSessionProvider(null), inputStream, sourcePath, targetPath, cleanupPublication);

        populatorService_.setCompletion(populatorService_.WCM, ((i + 1) * 100) / webContents.length());
      } catch (JSONException e) {
        LOG.error("Syntax error on WebContent n°" + i, e);

      } catch (IOException e) {
        LOG.error("Cannot read WebContent n°" + i, e);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void importXML(SessionProvider sessionProvider,
                        InputStream inputStream,
                        String sourcePath,
                        String targetPath,
                        Boolean cleanupPublication) throws Exception {
    ManageableRepository repository = repositoryService_.getCurrentRepository();
    try {
      Session session = sessionProvider.getSession("collaboration", repository);
      session.importXML(targetPath, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      String nodeName = DeploymentUtils.getNodeName(inputStream);
      if (cleanupPublication) {
        /**
         * This code allows to cleanup the publication lifecycle and publish the
         * first version in the target folder after importing the data. By using
         * this, the publication live revision property will be re-initialized
         * and the content will be set as published directly. Thus, the content
         * will be visible in front side.
         */

        Node parent = (Node) session.getItem(targetPath + "/" + nodeName);
        cleanPublication(parent);
      }

      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info(sourcePath + " is deployed succesfully into " + targetPath);
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("deploy " + sourcePath + " into " + targetPath + " is FAILURE at " + new Date().toString() + "\n", ex);
      }
    }
  }

  /**
   * This method implement cleaning publication
   * 
   * @param node the target Node
   * @throws Exception
   * @throws NotInPublicationLifecycleException
   */
  private void cleanPublication(Node node) throws NotInPublicationLifecycleException, Exception {
    if (node.hasProperty("publication:liveRevision") && node.hasProperty("publication:currentState")) {
      if (LOG.isInfoEnabled()) {
        LOG.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
      }
      node.setProperty("publication:liveRevision", "");
      node.setProperty("publication:currentState", "published");
    }
    node.getSession().save();
    NodeIterator iter = node.getNodes();
    while (iter.hasNext()) {
      Node childNode = iter.nextNode();
      cleanPublication(childNode);
    }
  }

}
