/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 04/02/16.
 */

@Scripts(
        {
                @Script(value = "js/jquery-1.8.3.min.js", id = "jquery",location = AssetLocation.SERVER),
                @Script(value = "js/populator.js", id = "populatorjs",location = AssetLocation.SERVER, depends = "jquery")
        }
)


@Stylesheets(
        {
                @Stylesheet(value = "/org/exoplatform/addons/sdpDemo/populator/portlet/populator/assets/populator.css", location = AssetLocation.APPLICATION, id = "populatorcss")
        }

)
@Bindings(
        {

                @Binding(value = org.exoplatform.injection.core.module.ActivityModule.class),
                @Binding(value = org.exoplatform.injection.core.module.CalendarModule.class),
                @Binding(value = org.exoplatform.injection.core.module.DocumentModule.class),
                @Binding(value = org.exoplatform.injection.core.module.ForumModule.class),
                @Binding(value = org.exoplatform.injection.core.module.SpaceModule.class),
                @Binding(value = org.exoplatform.injection.core.module.UserModule.class),
                @Binding(value = org.exoplatform.injection.core.module.WikiModule.class),
                @Binding(value = org.exoplatform.injection.services.DataInjector.class)
        }
)

@Less(value = {"populator.less"}, minify = true)

@Application
@Portlet
@Assets("*")

package org.exoplatform.addons.sdpDemo.populator.portlet.populator;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.*;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

