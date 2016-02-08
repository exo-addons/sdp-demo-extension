package org.exoplatform.addons.sdpDemo.populator.services;

import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.model.AvatarAttachment;

import java.io.InputStream;
import java.util.Calendar;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 05/02/16.
 */
public class Utils {

    public static AvatarAttachment getAvatarAttachment(String fileName) throws Exception
    {
        String mimeType = "image/png";
        int WIDTH = 200;
        InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("/medias/images/"+fileName);

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
            avatarAttachment = new AvatarAttachment(null,
                    fileName,
                    mimeType,
                    inputStream,
                    null,
                    System.currentTimeMillis());
        }

        return avatarAttachment;
    }

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

    public static int getHourAsInt(String hourString) {
        String[] start = hourString.split(":");
        Integer hour = Integer.parseInt(start[0]);
        return hour;
    }
    public static int getMinuteAsInt(String hourString) {
        String[] start = hourString.split(":");
        Integer minutes = Integer.parseInt(start[1]);
        return minutes;
    }
}
