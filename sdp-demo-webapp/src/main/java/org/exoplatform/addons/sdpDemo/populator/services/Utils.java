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

import org.apache.commons.io.IOUtils;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.model.AvatarAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 05/02/16.
 */
public class Utils {

  /**
   * Gets the avatar attachment.
   *
   * @param fileName the file name
   * @return the avatar attachment
   * @throws Exception the exception
   */
  public static AvatarAttachment getAvatarAttachment(String fileName) throws Exception {
    String mimeType = "image/png";
    int WIDTH = 120;
    InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("/medias/images/" + fileName);
    // Resize avatar to fixed width if can't(avatarAttachment == null) keep
    // origin avatar
    AvatarAttachment avatarAttachment = ImageUtils.createResizedAvatarAttachment(inputStream,
                                                                                 WIDTH,
                                                                                 0,
                                                                                 null,
                                                                                 fileName,
                                                                                 mimeType,
                                                                                 null);
    if (avatarAttachment == null) {
      avatarAttachment = new AvatarAttachment(null, fileName, mimeType, inputStream, null, System.currentTimeMillis());
    }
    return avatarAttachment;
  }

  /**
   * Gets the day as int.
   *
   * @param day the day
   * @return the day as int
   */
  public static int getDayAsInt(String day) {
    if ("monday".equals(day))
      return Calendar.MONDAY;
    else if ("tuesday".equals(day))
      return Calendar.TUESDAY;
    else if ("wednesday".equals(day))
      return Calendar.WEDNESDAY;
    else if ("thursday".equals(day))
      return Calendar.THURSDAY;
    else if ("friday".equals(day))
      return Calendar.FRIDAY;
    else if ("saturday".equals(day))
      return Calendar.SATURDAY;
    else if ("sunday".equals(day))
      return Calendar.SUNDAY;
    return Calendar.MONDAY;
  }

  /**
   * Gets the hour as int.
   *
   * @param hourString the hour string
   * @return the hour as int
   */
  public static int getHourAsInt(String hourString) {
    String[] start = hourString.split(":");
    Integer hour = Integer.parseInt(start[0]);
    return hour;
  }

  /**
   * Gets the minute as int.
   *
   * @param hourString the hour string
   * @return the minute as int
   */
  public static int getMinuteAsInt(String hourString) {
    String[] start = hourString.split(":");
    Integer minutes = Integer.parseInt(start[1]);
    return minutes;
  }

  /**
   * Gets the wiki page.
   *
   * @param fileName the file name
   * @return the wiki page
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static String getWikiPage(String fileName) throws IOException {
    if (fileName.equals("")) {
      return "";
    }
    InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("/medias/contents/" + fileName);

    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer);

    return writer.toString();
  }

  /**
   * Gets the file.
   *
   * @param fileName the file name
   * @param fileType the file type
   * @return the file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static InputStream getFile(String fileName, String fileType) throws IOException {

    if (fileName.equals("")) {
      return null;
    }
    InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("/medias/" + fileType + "/" + fileName);
    return inputStream;
  }

}
