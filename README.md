SDP Demo Extension
=======

SDP (Solution Development Prompte) Demo Extension, based on predefined scenario.


This extension allow user to populate an eXo Platform with predefined datas, described in text file.

On the demo page, select the scenario to load, and click on "Start Populating"
![Populating ...](https://raw.githubusercontent.com/exo-addons/sdp-demo-extension/master/data/populating.png)


#Scenario Syntax

The syntax used for scenario is JSON format.
To create new scenario, add a json file in WEB-INF/classes/medias/scenarios. Then it will be available in the select box.
```
{
  "scenarioName":"HR_Knowledge_Management",
  "description":"Design expert guy sharing knowledge items obtained after attending an industry workshop.  Pre-retirement product marketing manager contributes info that proves valuable to employees after her retirement. Corporate communications lady preparing for a press release finds all the info she needs prior to a meeting, saves time going over basics. Sales rep back from maternity leave gets up to speed after searching/finding new key info that was provided while she was away.",
  "scriptData":"HR_Knowledge_Management.zip",
  "data": {...}
}
```

**scenarioName** is the name of the scenario
**description** is used to describe the scenario
**scriptData** is optional. It is a file, stored in /WEB-INF/classes/medias/scriptData/. This file can be downloaded at the end of the populating, and contains all stuff needed for running the scenario script, like wiki page content, read to be copy/pasted, documents to upload ...
Then, anywhere, without any support, you can find all contents to run the demo.

**data** contains the scenario datas :
```
 "data": {
    "users":[...],
    "relations":[...],
    "spaces":[...],
    "calendars":[...],
    "wikis":[...],
    "activities":[...],
    "documents":[...],
    "forums":[...]
 }
```

Each data category is optional.

# Users 
**users** contains a list of users :
```
{
    "username":"scott",
    "firstname":"Scott",
    "lastname":"Wilson",
    "password":"gtngtn",
    "email":"scott.wilson@acme.com",
    "position":"Product Designer",
    "isadmin":"false",
    "avatar":"eXo-Face-John.png"
}
```

**username** is the user login
**firstname** and **lastname** will be combined to create the displayName, here : "Scott Wilson"
**password** will be used to log in the platform
**position** will be stored in the user profile 
**isAdmin** says if user is member of /platform/administrators group
**avatar** is the user image. This image is stored in /WEB-INF/classes/medias/images/

# Relations
**relations** described the relations between users to create in the platform :
```
{
    "inviting":"scott",
    "invited":"philip",
    "confirmed":true
}
```

**inviting** is the user which make the request
**invited** is the user which receive the request
**confirmed** says if the request is already validated or not. This parameter is optional.

# Spaces
**spaces** described spaces to create:
```
{
    "displayName": "Product Team",
    "creator": "kate",
    "avatar": "eXo-Space-Public-color.png",
    "members":["kate","scott"]
}
```

**displayName** is the visible name of the space
**creator** is manager of the space
**avatar** define the image with is the space avatar. The image is stored in /WEB-INF/classes/medias/images/
**members** describe which users are members of the space. Members must be described in "users" part.
 
# Calendars
**calendars** defines calendars to create, and event to add.
```
{
    "user":"scott",
    "clearAll":false,
    "calendars": [
        {
            "name": "Scott Wilson",
            "color": "powder_blue",
            "type": "user",
            "events":[
                {
                "title": "Spec Review",
                "day": "monday",
                "start": "17:00",
                "end": "19:00"
                }
            ]
        }
    ]
}
```
**user** define the owner of calendars
**clearAll** : optionnal : if set to true, all existing events for theses calendars will be removed.
**calendars** defines the calendars to add :
    **type** define the type of the calendar : if type is "user", the calendar is the user private calendar, else, or if not present, calendar is a public calendar (like space calendar)
    **name** is the owner of the calendar : if type=user, name contains the name of a use rof the platforme. Else, name contains, the name of the space which own the calendar.
    **color** defines the color of the calendar
    **events** define the events to add in the calendar.
        **title** is the name of the event
        **day** is the day on which the event will be added. If this value is "monday", the event will be added on the next monday.
        **start** and **end** define the start hour and the end hour of the event.
        
Here an example to add event in a space calendar : notice the calendar name, "Sales". Events will be added in Sales space calendar.
```
{
    "user":"evan",
    "clearAll":"false",
    "calendars": [
        {
            "name": "Sales",
            "color": "moss_green",
            "events":[
                {
                    "title": "Pipe Review",
                    "day": "monday",
                    "start": "10:00",
                    "end": "11:30"
                },
                {
                    "title": "Sales Webinar",
                    "day": "thursday",
                    "start": "11:00",
                    "end": "12:00"
                }
            ]
        }
    ]
}
```
        
#Wikis 
Wiki page can be added in portal wiki, group (space) wiki, or user wiki.
```
{
    "title": "General Knowledge",
    "parent": "WikiHome",
    "owner": "intranet",
    "type": "portal",
    "filename": "activity-stream-engagement.txt",
    "wikis": [
        {
            "title": "New in the Company",
            "type": "portal",
            "owner": "intranet"
        },
        {
            "title": "Intranet Team Organization",
            "type": "portal",
            "owner": "intranet"
        }
    ]
}
```
**title** is the title of the page
**parent** is the name of the parent page. This page will be created under the one defined in parent. Optional for main page of the wiki, but if not present, name must be "WikiHome"
**owner** is the wiki in which the page will be added. Depends of type parameter
**type** : can be "portal", "group" or "user". Define the wiki type. if type is group, wiki name must be the name of the group owning the wiki. For example to add a page in the wiki of space "Product Team", "owner" will be : "/spaces/product_team"
**filename** define the content to load in the wiki page. The file must be in /WEB-INF/classes/medias/contents/
**wikis** define a list of subpage.

For subpages, parent is not necessary

#Activities 
```
{
    "from": "kate",
    "body": "Don't forget our Public Webinar next week",
    "likes":["scott","amber","amanda","evan"],
    "comments": [
        {
            "from": "scott",
            "body": "in the main room?"
        },
        {
            "from": "kate",
            "body": "yes and it will be also shared online"
        },
        {
            "from": "amanda",
            "body": "btw @kate , remind me to send a formal invitation"
        },
        {
            "from": "kate",
            "body": "of course @amanda"
        }
    ]
}
```
**from** define the author of the activity
**body** is the content of the activity
**likes** is a list of user which have liked the activity
**comments** define a list of comments under the activity
    **from** is the author of the comment
    **body** is the content of the comment
    
    
#Documents
```
{
    "filename":"Emerging Trends in Cosmetics Design.pdf",
    "owner":"scott",
    "isPrivate":false,
    "spaceName":"product_team"
},
{
    "filename":"Best Practices in Cosmetics Design.pdf",
    "owner":"scott",
    "isPrivate":true
}
```
**filename** define the name of the document to upload. The document must be stored in /WEB-INF/classes/medias/documents/
**owner** is the name of the user which have upload the document.
**isPrivate** define if the document is a user document or a space document. If set to true, document is store in the private folder of the user. Else, a spaceName must be defined
**spaceName** the name of the space in which store the document if isPrivate is false.

#Forum
For forum, you will be able to define category, forums, topics, and posts
```
 {
        "categoryTitle":"General",
        "owner":"scott",
        "description":"General Category",
        "forums":[
          {
            "forumTitle":"Public discussions",
            "owner":"scott",
            "description":"General Forum",
            "topics":[
              {
                "topicTitle":"How to use this forum ?",
                "owner":"scott",
                "content":"This is the content of the message.",
                "posts": [
                  {
                    "content":"You're right !",
                    "owner" : "kate"
                  }
                ]
              }
            ]
          }
        ]
      }
```
**categoryTitle** is the name of the forum category. If exists, the category is not recreated. If forums must be created in a space, categoryTitles must be "spaces" which is the default category for space forums.
**owner** is the name of the category creator
**description** is the description of the category
**forums** define a list of forums to create in the category
    **forumTitle** define the name of the forum to create. If exists, the forum is not recreated.
    **owner** is the name of the forum creator
    **description** is the description of the forum
    **topics** define a list of topics to create
        **topicTitle** define the name of the topic to create. If exists, the topic is not recreated.
        **owner** is the name of the topic creator
        **description** is the description of the topic
        **posts** define a list of posts to create
            **content** : the content of the post
            **owner** the author of the post