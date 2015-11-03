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
import java.util.List;

import javax.swing.JOptionPane;

import org.cresques.cts.IProjection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
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
import org.gvsig.fmap.dal.coverage.store.parameter.RasterDataParameters;
import org.gvsig.fmap.dal.serverexplorer.filesystem.FilesystemStoreParameters;
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
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.tmsgenerator.OmsTmsGenerator;
import org.jgrasstools.gears.utils.files.FileUtilities;
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
        IconThemeHelper.registerIcon("action", "icon_tiles_16x", this);

        i18nManager = ToolsLocator.getI18nManager();
        applicationManager = ApplicationLocator.getManager();

        projectManager = applicationManager.getProjectManager();
        dialogManager = ToolsSwingLocator.getThreadSafeDialogsManager();
    }

    public void postInitialize() {
        AddLayer.addWizard(GeopaparazziLayerWizard.class);
    }

    /**
     * Execute the actions associated to this extension.
     */
    public void execute( String actionCommand ) {
        if (ACTION_GPAPTILES.equalsIgnoreCase(actionCommand)) {
            // Set the tool in the mapcontrol of the active view.

            IWindow activeWindow = applicationManager.getActiveWindow();
            if (activeWindow == null) {
                return;
            }
            try {
                /*
                 * TODO check if the active view is the right one
                 * and if the right layers are present.
                 */

                Document activeDocument = projectManager.getCurrentProject().getActiveDocument();
                ViewDocument view = (ViewDocument) activeDocument;
                MapContext mapContext = view.getMapContext();
                FLayers layers = mapContext.getLayers();

                int layersCount = layers.getLayersCount();
                if (layersCount == 0) {
                    dialogManager.messageDialog("No compatible layers found to generate mbtiles database.", "WARNING",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                List<String> vectorPaths = new ArrayList<String>();
                List<String> rasterPaths = new ArrayList<String>();
                for( int i = 0; i < layersCount; i++ ) {
                    FLayer layer = layers.getLayer(i);
                    if (layer == null)
                        continue;
                    if (layer instanceof FLyrVect) {
                        FLyrVect vectorLayer = (FLyrVect) layer;
                      FilesystemStoreParameters fsSParams = (FilesystemStoreParameters)  vectorLayer.getDataStore().getParameters();
                      File file = fsSParams.getFile();
//                      vectorLayer.getFeatureStore().getDynValue(DataStore.METADATA_CRS);
                      
                      
                      
                        String path = ""; // TODO
                        vectorPaths.add(path);
                    }
                    if (layer instanceof FLyrRaster) {
                        FLyrRaster rasterLayer = (FLyrRaster) layer;
                        RasterDataParameters rdParams = ((RasterDataParameters)rasterLayer.getDataStore().getParameters());
//                        rdParams.getURI()
                        String path = ""; // TODO
                        vectorPaths.add(path);
                    }
                    // TODO raster path
                }

                IProjection projection = mapContext.getProjection();
                Crs crsObject = (Crs) projection;
                String crsWkt = crsObject.getWKT();

                Envelope envelope = mapContext.getViewPort().getEnvelope();

                Point ll = envelope.getLowerCorner();
                Point ur = envelope.getUpperCorner();

                CoordinateReferenceSystem crs = CRS.parseWKT(crsWkt);
                // try {
                // crs = CRS.decode(fullCode);
                // } catch (Exception e) {
                // String epsgCode = dialogManager
                // .inputDialog("Could not decode CRS, please insert an EPSG code manually (ex.
                // 4326)", "EPSG code");
                // if (epsgCode == null || epsgCode.trim().length() == 0) {
                // return;
                // }
                // crs = CRS.decode("EPSG:" + epsgCode);
                // e.printStackTrace();
                // }
                ReferencedEnvelope bounds = new ReferencedEnvelope(ll.getX(), ur.getX(), ll.getY(), ur.getY(), crs);
                process(bounds, vectorPaths, rasterPaths);
            } catch (Exception e) {
                logger.error("ERROR", e);
                dialogManager.messageDialog("An error occurred while generating tiles.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private void process( ReferencedEnvelope bounds, List<String> vectorPaths, List<String> rasterPaths ) throws Exception {

        final GenerateTilesParametersPanel parametersPanel = new GenerateTilesParametersPanel();
        WindowManager windowManager = ToolsSwingLocator.getWindowManager();
        windowManager.showWindow(parametersPanel.asJComponent(), "Epanet Results Browser", MODE.DIALOG);

        int maxZoom = parametersPanel.getMaxZoom();
        int minZoom = parametersPanel.getMinZoom();
        String dbName = parametersPanel.getDbName();
        String dbFolder = parametersPanel.getDbFolder();
        String imageType = parametersPanel.getImageType();

        if (dbName.trim().length() == 0) {
            dbName = "new_dataset";
        }
        if (dbFolder.trim().length() == 0 || !new File(dbFolder).exists()) {
            dialogManager.messageDialog("The output folder needs to be an existing folder (got: " + dbFolder + ")", "ERROR",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        IJGTProgressMonitor pm = new LogProgressMonitor();
        OmsTmsGenerator gen = new OmsTmsGenerator();
        if (rasterPaths.size() > 0)
            gen.inRasterFile = FileUtilities.stringListAsTmpFile(rasterPaths).getAbsolutePath();
        if (vectorPaths.size() > 0)
            gen.inVectorFile = FileUtilities.stringListAsTmpFile(vectorPaths).getAbsolutePath();
        gen.pMinzoom = minZoom;
        gen.pMaxzoom = maxZoom;
        gen.pName = dbName;
        gen.inPath = dbFolder;
        gen.pWest = bounds.getMinX();
        gen.pEast = bounds.getMaxX();
        gen.pNorth = bounds.getMaxY();
        gen.pSouth = bounds.getMinY();
        // gen.pEpsg = "EPSG:32632";
        gen.dataCrs = bounds.getCoordinateReferenceSystem();
        gen.doMbtiles = true;

        // gen.inZoomLimitVector = inZoomLimitROI;
        // gen.pZoomLimit = pZoomLimit;

        if (imageType.equals("jpg")) {
            gen.pImagetype = 1;
        } else {
            // case "png":
            gen.pImagetype = 0;
        }
        gen.pm = pm;
        gen.process();

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