/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gvsig.spatialtoolbox;

import org.gvsig.andami.IconThemeHelper;
import org.gvsig.andami.plugins.Extension;
import org.gvsig.app.ApplicationManager;
import org.gvsig.tools.swing.api.ToolsSwingLocator;
import org.gvsig.tools.swing.api.windowmanager.WindowManager;
import org.gvsig.tools.swing.api.windowmanager.WindowManager.MODE;
import org.jgrasstools.gui.spatialtoolbox.core.JGrasstoolsModulesManager;
import org.jgrasstools.gvsig.base.GvsigBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Andami extension to generate tiles from a view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialtoolboxExtension extends Extension {

    private static final Logger logger = LoggerFactory.getLogger(SpatialtoolboxExtension.class);

    private static final String ACTION_SPATIALTOOLBOX = "run-spatial-toolbox";

    private ApplicationManager applicationManager;

    private GvsigSpatialtoolboxController spatialtoolboxController;

    public void initialize() {
        IconThemeHelper.registerIcon("action", "blocks", this);
        IconThemeHelper.registerIcon("action", "category", this);
        IconThemeHelper.registerIcon("action", "favicon", this);
        IconThemeHelper.registerIcon("action", "generate_script", this);
        IconThemeHelper.registerIcon("action", "module_exp", this);
        IconThemeHelper.registerIcon("action", "module", this);
        IconThemeHelper.registerIcon("action", "processingregion_disabled", this);
        IconThemeHelper.registerIcon("action", "processingregion", this);
        IconThemeHelper.registerIcon("action", "run_script", this);
        IconThemeHelper.registerIcon("action", "start", this);
        IconThemeHelper.registerIcon("action", "stop", this);
        IconThemeHelper.registerIcon("action", "trash", this);

        // i18nManager = ToolsLocator.getI18nManager();
        // applicationManager = ApplicationLocator.getManager();
        //
        // projectManager = applicationManager.getProjectManager();
        // dialogManager = ToolsSwingLocator.getThreadSafeDialogsManager();
    }

    public void postInitialize() {
        new Thread(new Runnable(){
            public void run() {
                try {
                    JGrasstoolsModulesManager.getInstance().init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Execute the actions associated to this extension.
     */
    public void execute( String actionCommand ) {
        if (ACTION_SPATIALTOOLBOX.equalsIgnoreCase(actionCommand)) {
            spatialtoolboxController = new GvsigSpatialtoolboxController(new GvsigBridgeHandler());
            WindowManager windowManager = ToolsSwingLocator.getWindowManager();
            windowManager.showWindow(spatialtoolboxController.asJComponent(), "JGrasstools' Spatial Toolbox", MODE.WINDOW);
        }
    }

    /**
     * Check if tools of this extension are enabled.
     */
    public boolean isEnabled() {
        //
        // By default the tool is always enabled
        //
        return true;
    }

    /**
     * Check if tools of this extension are visible.
     */
    public boolean isVisible() {
        if (spatialtoolboxController != null)
            spatialtoolboxController.isVisibleTriggered();
        return true;
    }

}
