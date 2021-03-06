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
package org.jgrasstools.gvsig.geopaparazzi;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import org.cresques.cts.IProjection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.gvsig.andami.IconThemeHelper;
import org.gvsig.andami.plugins.Extension;
import org.gvsig.andami.ui.mdiManager.IWindow;
import org.gvsig.app.ApplicationLocator;
import org.gvsig.app.ApplicationManager;
import org.gvsig.app.extension.AddLayer;
import org.gvsig.app.project.ProjectManager;
import org.gvsig.app.project.documents.Document;
import org.gvsig.app.project.documents.view.ViewDocument;
import org.gvsig.crs.Crs;
import org.gvsig.fmap.geom.primitive.Envelope;
import org.gvsig.fmap.geom.primitive.Point;
import org.gvsig.fmap.mapcontext.MapContext;
import org.gvsig.fmap.mapcontext.layers.FLayer;
import org.gvsig.fmap.mapcontext.layers.FLayers;
import org.gvsig.fmap.mapcontext.layers.vectorial.FLyrVect;
import org.gvsig.raster.fmap.layers.FLyrRaster;
import org.gvsig.tools.ToolsLocator;
import org.gvsig.tools.i18n.I18nManager;
import org.gvsig.tools.swing.api.ToolsSwingLocator;
import org.gvsig.tools.swing.api.threadsafedialogs.ThreadSafeDialogsManager;
import org.gvsig.tools.swing.api.windowmanager.WindowManager;
import org.gvsig.tools.swing.api.windowmanager.WindowManager.MODE;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.tmsgenerator.OmsTmsGenerator;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gui.console.ProcessLogConsoleController;
import org.jgrasstools.gui.spatialtoolbox.core.SpatialToolboxConstants;
import org.jgrasstools.gui.spatialtoolbox.core.StageScriptExecutor;
import org.jgrasstools.gvsig.base.GtGvsigConversionUtilities;
import org.jgrasstools.gvsig.base.GvsigBridgeHandler;
import org.jgrasstools.gvsig.base.JGTUtilities;
import org.jgrasstools.gvsig.base.LayerUtilities;
import org.jgrasstools.gvsig.base.ProjectUtilities;
import org.jgrasstools.gvsig.base.utils.console.LogConsoleController;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Andami extension to generate tiles from a view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GenerateTilesExtension extends Extension {

    private static final Logger logger = LoggerFactory.getLogger(GenerateTilesExtension.class);

    private static final String ACTION_GPAPTILES = "create-geopaparazzi-tiles";

    private I18nManager i18nManager;

    private ApplicationManager applicationManager;

    private ProjectManager projectManager;

    private ThreadSafeDialogsManager dialogManager;

    public void initialize() {
        IconThemeHelper.registerIcon("action", "pictures", this);

        i18nManager = ToolsLocator.getI18nManager();
        applicationManager = ApplicationLocator.getManager();

        projectManager = applicationManager.getProjectManager();
        dialogManager = ToolsSwingLocator.getThreadSafeDialogsManager();
    }

    public void postInitialize() {
        // this enables the add layere geopaparazzi wizard tab
        AddLayer.addWizard(GeopaparazziLayerWizard.class);
    }

    /**
     * Execute the actions associated to this extension.
     */
    public void execute( String actionCommand ) {
        if (ACTION_GPAPTILES.equalsIgnoreCase(actionCommand)) {
            // Set the tool in the mapcontrol of the active view.

            MapContext mapContext = ProjectUtilities.getCurrentMapcontext();
            if (mapContext == null) {
                dialogManager.messageDialog("No active map available to take the data from.\nPlease open a map view.", "WARNING",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                FLayers layers = mapContext.getLayers();

                List<String> vectorPaths = new ArrayList<String>();
                List<String> rasterPaths = new ArrayList<String>();
                int layersCount = layers.getLayersCount();
                for( int i = 0; i < layersCount; i++ ) {
                    FLayer layer = layers.getLayer(i);
                    if (layer == null)
                        continue;
                    if (layer instanceof FLyrVect) {
                        FLyrVect vectorLayer = (FLyrVect) layer;

                        File vectorFile = LayerUtilities.getFileFromVectorFileLayer(vectorLayer);
                        String path = vectorFile.getAbsolutePath();
                        path = FileUtilities.replaceBackSlashesWithSlashes(path);
                        vectorPaths.add(path);
                    } else if (layer instanceof FLyrRaster) {
                        FLyrRaster rasterLayer = (FLyrRaster) layer;
                        File rasterFile = LayerUtilities.getFileFromRasterFileLayer(rasterLayer);
                        String path = rasterFile.getAbsolutePath();
                        path = FileUtilities.replaceBackSlashesWithSlashes(path);
                        rasterPaths.add(path);
                    }
                }

                if (vectorPaths.size() == 0 && rasterPaths.size() == 0) {
                    dialogManager.messageDialog("No compatible layers found to generate mbtiles database.", "WARNING",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                IProjection projection = mapContext.getProjection();
                Crs crsObject = (Crs) projection;
                CoordinateReferenceSystem crs = GtGvsigConversionUtilities.gvsigCrs2gtCrs(crsObject);

                Envelope envelope = mapContext.getViewPort().getEnvelope();

                Point ll = envelope.getLowerCorner();
                Point ur = envelope.getUpperCorner();

                ReferencedEnvelope bounds = new ReferencedEnvelope(ll.getX(), ur.getX(), ll.getY(), ur.getY(), crs);
                process(bounds, vectorPaths, rasterPaths);
            } catch (Exception e) {
                logger.error("ERROR", e);
                dialogManager.messageDialog("An error occurred while generating tiles.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private void process( final ReferencedEnvelope bounds, final List<String> vectorPaths, final List<String> rasterPaths )
            throws Exception {

        final GenerateTilesParametersPanelController parametersPanel = new GenerateTilesParametersPanelController();
        WindowManager windowManager = ToolsSwingLocator.getWindowManager();
        windowManager.showWindow(parametersPanel.asJComponent(), "Geopaparazzi Tiles Creator", MODE.DIALOG);

        if (!parametersPanel.okToRun) {
            return;
        }

        final int maxZoom = parametersPanel.maxZoom;
        final int minZoom = parametersPanel.minZoom;
        final String dbName = parametersPanel.dbName;
        String dbFolder = parametersPanel.outputFolder;
        final String imageType = parametersPanel.imageType;

        new Thread(new Runnable(){
            public void run() {
                try {
                    String folder = FileUtilities.replaceBackSlashesWithSlashes(dbFolder);
                    runModule(bounds, vectorPaths, rasterPaths, maxZoom, minZoom, dbName, folder, imageType);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void runModule( ReferencedEnvelope bounds, List<String> vectorPaths, List<String> rasterPaths, int maxZoom,
            int minZoom, String dbName, String dbFolder, String imageType ) throws Exception {

        CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
        String epsg = CrsUtilities.getCodeFromCrs(crs);

        StringBuilder sb = new StringBuilder();
        sb.append(
                "org.jgrasstools.gears.modules.r.tmsgenerator.OmsTmsGenerator gen = new org.jgrasstools.gears.modules.r.tmsgenerator.OmsTmsGenerator();\n");
        if (rasterPaths.size() > 0) {
            String tmpPath = FileUtilities.stringListAsTmpFile(rasterPaths).getAbsolutePath();
            tmpPath = FileUtilities.replaceBackSlashesWithSlashes(tmpPath);
            sb.append("gen.inRasterFile = \"" + tmpPath).append("\"\n");
        }
        if (vectorPaths.size() > 0) {
            String tmpPath = FileUtilities.stringListAsTmpFile(vectorPaths).getAbsolutePath();
            tmpPath = FileUtilities.replaceBackSlashesWithSlashes(tmpPath);
            sb.append("gen.inVectorFile = \"" + tmpPath).append("\"\n");
        }
        sb.append("gen.pMinzoom = " + minZoom).append("\n");
        sb.append("gen.pMaxzoom = " + maxZoom).append("\n");
        sb.append("gen.pName = \"" + dbName).append("\"\n");
        sb.append("gen.inPath = \"" + dbFolder).append("\"\n");
        sb.append("gen.pWest = " + bounds.getMinX()).append("\n");
        sb.append("gen.pEast = " + bounds.getMaxX()).append("\n");
        sb.append("gen.pNorth = " + bounds.getMaxY()).append("\n");
        sb.append("gen.pSouth = " + bounds.getMinY()).append("\n");
        sb.append("gen.pEpsg = \"" + epsg + "\"\n");
        sb.append("gen.doMbtiles = true\n");

        if (imageType.equals("jpg")) {
            sb.append("gen.pImagetype = 1;\n");
        } else {
            // case "png":
            sb.append("gen.pImagetype = 0;\n");
        }
        sb.append("gen.process();\n");

        String script = sb.toString();

        GvsigBridgeHandler guiBridge = new GvsigBridgeHandler();
        final ProcessLogConsoleController logConsole = new ProcessLogConsoleController();
        guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

        StageScriptExecutor exec = new StageScriptExecutor(guiBridge.getLibsFolder());
        exec.addProcessListener(logConsole);

        String logLevel = SpatialToolboxConstants.LOGLEVEL_GUI_OFF;

        int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        long maxMb = runtime.maxMemory() / mb;
        if (maxMb > 1000) {
            maxMb = 1000;
        }
        String ramLevel = "" + maxMb; // TODO change memory management

        String sessionId = "Geopaparazzi Tiles generation - "
                + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
        Process process = exec.exec(sessionId, script, logLevel, ramLevel, null);
        logConsole.beginProcess(process, sessionId);

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
        return true;
    }

}
