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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.calendar.service.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

import juzu.SessionScoped;

/**
 * The Class CalendarService.
 */
@Named("calendarService")
@SessionScoped
public class CalendarService {

  /** The log. */
  private final Log                                LOG = ExoLogger.getLogger(CalendarService.class);

  /** The calendar service. */
  org.exoplatform.calendar.service.CalendarService calendarService_;

  /** The organization service. */
  OrganizationService                              organizationService_;

  /**
   * Instantiates a new calendar service.
   *
   * @param calendarService the calendar service
   * @param organizationService the organization service
   */
  @Inject
  public CalendarService(org.exoplatform.calendar.service.CalendarService calendarService,
                         OrganizationService organizationService) {
    calendarService_ = calendarService;
    organizationService_ = organizationService;
  }

  /**
   * Sets the calendar colors.
   *
   * @param calendars the calendars
   * @param populatorService_ the populator service
   */
  public void setCalendarColors(JSONArray calendars, PopulatorService populatorService_) {
    for (int i = 0; i < calendars.length(); i++) {
      try {
        JSONObject calendarObject = calendars.getJSONObject(i);
        String username = calendarObject.getString("user");
        JSONArray userCalendars = calendarObject.getJSONArray("calendars");
        Map<String, JSONObject> map = new HashMap();
        for (int j = 0; j < userCalendars.length(); j++) {
          JSONObject userCalendar = userCalendars.getJSONObject(j);
          map.put(userCalendar.getString("name"), userCalendar);
        }

        String filtered = null;
        try {
          String[] calendarIdList = getCalendarsIdList(username);
          for (String calId : calendarIdList) {
            Calendar calendar = calendarService_.getCalendarById(calId);
            String calName = calendar.getName();
            if (map.containsKey(calName)) {
              JSONObject calTemp = map.get(calName);
              calendar.setCalendarColor(calTemp.getString("color"));
              if (calTemp.has("type") && calTemp.getString("type").equals("user")) {
                calendarService_.saveUserCalendar(username, calendar, true);
              } else
                calendarService_.savePublicCalendar(calendar, false);
            } else {
              filtered = calendar.getId();
            }
          }
          if (filtered != null) {
            CalendarSetting setting = calendarService_.getCalendarSetting(username);
            setting.setFilterPublicCalendars(new String[] { filtered });
            calendarService_.saveCalendarSetting(username, setting);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } catch (JSONException e) {
        LOG.error("Syntax error on calendar n°" + i, e);

      }
      // we loop on calendars twice, so, completion is adapted
      populatorService_.setCompletion(populatorService_.CALENDAR, ((i + 1) * 100) / (calendars.length() * 2));

    }
  }

  /**
   * Creates the events.
   *
   * @param calendars the calendars
   * @param populatorService_ the populator service
   */
  public void createEvents(JSONArray calendars, PopulatorService populatorService_) {

    try {

      for (int i = 0; i < calendars.length(); i++) {
        try {
          JSONObject calendarObject = calendars.getJSONObject(i);
          String username = calendarObject.getString("user");
          Map<String, String> map = getCalendarsMap(username);

          if (calendarObject.has("clearAll") && calendarObject.getBoolean("clearAll")) {
            removeAllEvents(username);
          }
          JSONArray userCalendars = calendarObject.getJSONArray("calendars");
          for (int j = 0; j < userCalendars.length(); j++) {
            JSONObject userCalendar = userCalendars.getJSONObject(j);
            JSONArray events = userCalendar.getJSONArray("events");
            for (int k = 0; k < events.length(); k++) {
              JSONObject event = events.getJSONObject(k);
              saveEvent(username,
                        userCalendar.has("type") && userCalendar.getString("type").equals("user"),
                        map.get(userCalendar.getString("name")),
                        event.getString("title"),
                        Utils.getDayAsInt(event.getString("day")),
                        Utils.getHourAsInt(event.getString("start")),
                        Utils.getMinuteAsInt(event.getString("start")),
                        Utils.getHourAsInt(event.getString("end")),
                        Utils.getMinuteAsInt(event.getString("end")));
            }
          }
        } catch (JSONException e) {
          LOG.error("Syntax error on calendar n°" + i, e);

        }
        // we loop on calendars twice, so, completion is adapted
        populatorService_.setCompletion(populatorService_.CALENDAR,
                                        ((i + 1 + calendars.length()) * 100) / (calendars.length() * 2));
      }

    } catch (Exception e) {
      e.printStackTrace(); // To change body of catch statement use File |
                           // Settings | File Templates.
    }
  }

  /**
   * Save event.
   *
   * @param username the username
   * @param isUserEvent the is user event
   * @param calId the cal id
   * @param summary the summary
   * @param day the day
   * @param fromHour the from hour
   * @param fromMin the from min
   * @param toHour the to hour
   * @param toMin the to min
   * @throws Exception the exception
   */
  private void saveEvent(String username,
                         boolean isUserEvent,
                         String calId,
                         String summary,
                         int day,
                         int fromHour,
                         int fromMin,
                         int toHour,
                         int toMin) throws Exception {
    CalendarEvent event = new CalendarEvent();
    event.setCalendarId(calId);
    event.setSummary(summary);
    event.setEventType(CalendarEvent.TYPE_EVENT);
    event.setRepeatType(CalendarEvent.RP_NOREPEAT);
    event.setPrivate(isUserEvent);
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    calendar.setTimeInMillis(calendar.getTime().getTime());
    calendar.set(java.util.Calendar.DAY_OF_WEEK, day);
    calendar.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
    calendar.set(java.util.Calendar.MINUTE, fromMin);
    event.setFromDateTime(calendar.getTime());
    calendar.set(java.util.Calendar.HOUR_OF_DAY, toHour);
    calendar.set(java.util.Calendar.MINUTE, toMin);
    event.setToDateTime(calendar.getTime());
    if (isUserEvent)
      calendarService_.saveUserEvent(username, calId, event, true);
    else
      calendarService_.savePublicEvent(calId, event, true);
  }

  /**
   * Removes the all events.
   *
   * @param username the username
   * @throws Exception the exception
   */
  private void removeAllEvents(String username) throws Exception {
    List<CalendarEvent> events = getEvents(username);
    for (CalendarEvent event : events) {
      if (event.isPrivate()) {
        calendarService_.removeUserEvent(username, event.getCalendarId(), event.getId());
      } else {
        calendarService_.removePublicEvent(event.getCalendarId(), event.getId());
      }
    }
  }

  /**
   * Gets the calendars map.
   *
   * @param username the username
   * @return the calendars map
   */
  private Map<String, String> getCalendarsMap(String username) {
    Map<String, String> map = new HashMap<String, String>();
    String[] calendarIdList = getCalendarsIdList(username);
    for (String calId : calendarIdList) {
      Calendar calendar = null;
      try {
        calendar = calendarService_.getCalendarById(calId);
        String calName = calendar.getName();
        map.put(calName, calId);
      } catch (Exception e) {
      }
    }
    return map;
  }

  /**
   * Gets the calendars id list.
   *
   * @param username the username
   * @return the calendars id list
   */
  private String[] getCalendarsIdList(String username) {
    StringBuilder sb = new StringBuilder();
    List<GroupCalendarData> listgroupCalendar = null;
    List<org.exoplatform.calendar.service.Calendar> listUserCalendar = null;
    try {
      listgroupCalendar = calendarService_.getGroupCalendars(getUserGroups(username), true, username);
      listUserCalendar = calendarService_.getUserCalendars(username, true);
    } catch (Exception e) {
      LOG.info("Error while checking User Calendar :" + e.getMessage());
    }
    for (GroupCalendarData g : listgroupCalendar) {
      for (org.exoplatform.calendar.service.Calendar c : g.getCalendars()) {
        sb.append(c.getId()).append(",");
      }
    }
    for (org.exoplatform.calendar.service.Calendar c : listUserCalendar) {
      sb.append(c.getId()).append(",");
    }
    String[] list = sb.toString().split(",");
    return list;
  }

  /**
   * Gets the events.
   *
   * @param username the username
   * @return the events
   */
  private List<CalendarEvent> getEvents(String username) {
    String[] calList = getCalendarsIdList(username);

    EventQuery eventQuery = new EventQuery();

    eventQuery.setOrderBy(new String[] { org.exoplatform.calendar.service.Utils.EXO_FROM_DATE_TIME });

    eventQuery.setCalendarId(calList);
    List<CalendarEvent> userEvents = null;
    try {
      userEvents = calendarService_.getEvents(username, eventQuery, calList);

    } catch (Exception e) {
      LOG.info("Error while checking User Events:" + e.getMessage());
    }
    return userEvents;
  }

  /**
   * Gets the user groups.
   *
   * @param username the username
   * @return the user groups
   * @throws Exception the exception
   */
  private String[] getUserGroups(String username) throws Exception {

    Object[] objs = organizationService_.getGroupHandler().findGroupsOfUser(username).toArray();
    String[] groups = new String[objs.length];
    for (int i = 0; i < objs.length; i++) {
      groups[i] = ((Group) objs[i]).getId();
    }
    return groups;
  }

}
