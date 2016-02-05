package org.exoplatform.addons.sdpDemo.populator.portlet.populator;

import juzu.*;
import juzu.plugin.ajax.Ajax;
import juzu.template.Template;
import org.exoplatform.addons.sdpDemo.populator.services.PopulatorService;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 04/02/16.
 */
public class Populator {

    /** . */
    @Inject
    @Path("index.gtmpl")
    Template index;


    @Inject
    PopulatorService populatorService_;

    @View
    public Response.Content index()
    {
        Collection<JSONObject> scenarios = populatorService_.getScenarios().values();
        return index.with().set("scenarios", scenarios).ok();
    }



    @Ajax
    @Resource
    public Response.Content populate(String scenarioName)
    {
        populatorService_.populate(scenarioName);
        StringBuilder sb = new StringBuilder() ;
        sb.append("{\"status\": \"OK\"}");
        return Response.ok(sb.toString()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
    }


}
