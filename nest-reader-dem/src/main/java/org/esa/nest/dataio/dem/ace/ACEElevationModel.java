/*
 * Copyright (C) 2011 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dataio.dem.ace;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.nest.dataio.dem.ElevationTile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ACEElevationModel implements ElevationModel, Resampling.Raster {

    private static final int NUM_X_TILES = ACEElevationModelDescriptor.NUM_X_TILES;
    private static final int NUM_Y_TILES = ACEElevationModelDescriptor.NUM_Y_TILES;
    private static final int DEGREE_RES = ACEElevationModelDescriptor.DEGREE_RES;
    private static final int NUM_PIXELS_PER_TILE = ACEElevationModelDescriptor.PIXEL_RES;
    private static final int NO_DATA_VALUE = ACEElevationModelDescriptor.NO_DATA_VALUE;
    private static final int RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

    private static final float DEGREE_RES_BY_NUM_PIXELS_PER_TILE = DEGREE_RES * (1.0f/NUM_PIXELS_PER_TILE);

    private final ACEElevationModelDescriptor descriptor;
    private final ACEFile[][] elevationFiles;
    private final List<ACEElevationTile> elevationTileCache = new ArrayList<ACEElevationTile>();
    private final Resampling resampling;
    private final Resampling.Index resamplingIndex;
    private final Resampling.Raster resamplingRaster;
    private static final ProductReaderPlugIn productReaderPlugIn = getACEReaderPlugIn();

    public ACEElevationModel(ACEElevationModelDescriptor descriptor, Resampling resamplingMethod) throws IOException {
        this.descriptor = descriptor;
        resampling = resamplingMethod;
        resamplingIndex = resampling.createIndex();
        resamplingRaster = this;
        this.elevationFiles = createElevationFiles();
    }

    /**
     * @return The resampling method used.
     * @since BEAM 4.6
     */
    public Resampling getResampling() {
        return resampling;
    }

    public ElevationModelDescriptor getDescriptor() {
        return descriptor;
    }

    public float getElevation(GeoPos geoPos) throws Exception {
        float pixelX = (geoPos.lon + 180.0f) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5
        float pixelY = RASTER_HEIGHT - (geoPos.lat + 90.0f) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5, y = (90 - lon) / DEGREE_RES * NUM_PIXELS_PER_TILE;
        final float elevation;
        synchronized (resampling) {
            resampling.computeIndex(pixelX, pixelY,
                                 RASTER_WIDTH,
                                 RASTER_HEIGHT,
                resamplingIndex);

            elevation = resampling.resample(resamplingRaster, resamplingIndex);
        }
        if (Float.isNaN(elevation)) {
            return NO_DATA_VALUE;
        }
        return elevation;
    }

    @Override
    public PixelPos getIndex(GeoPos geoPos) throws Exception {
        float pixelY = RASTER_HEIGHT - (geoPos.lat + 90.0f) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE; //DEGREE_RES * NUM_PIXELS_PER_TILE;
        float pixelX = (geoPos.lon + 180.0f) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE; // DEGREE_RES * NUM_PIXELS_PER_TILE;
        return new PixelPos(pixelX, pixelY);
    }

    @Override
    public synchronized GeoPos getGeoPos(PixelPos pixelPos) throws Exception {
        float pixelLat = (RASTER_HEIGHT - pixelPos.y) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 90.0f;
        float pixelLon = pixelPos.x / DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0f;
        return new GeoPos(pixelLat, pixelLon);
    }

    public void dispose() {
        for(ACEElevationTile tile : elevationTileCache) {
            tile.dispose();
        }
        elevationTileCache.clear();
        for (ACEFile[] elevationFile : elevationFiles) {
            for (ACEFile file : elevationFile) {
                file.dispose();
            }
        }
    }

    public int getWidth() {
        return RASTER_WIDTH;
    }

    public int getHeight() {
        return RASTER_HEIGHT;
    }

    public float getSample(int pixelX, int pixelY) throws IOException {
        final int tileXIndex = pixelX / NUM_PIXELS_PER_TILE;
        final int tileYIndex = pixelY / NUM_PIXELS_PER_TILE;
        final ElevationTile tile = elevationFiles[tileXIndex][tileYIndex].getTile();
        if(tile == null) {
            return Float.NaN;
        }
        final int tileX = pixelX - tileXIndex * NUM_PIXELS_PER_TILE;
        final int tileY = pixelY - tileYIndex * NUM_PIXELS_PER_TILE;
        final float sample = tile.getSample(tileX, tileY);
        if (sample == NO_DATA_VALUE)
            return Float.NaN;
        return sample;
    }

    private ACEFile[][] createElevationFiles() throws IOException {
        final ACEFile[][] elevationFiles = new ACEFile[NUM_X_TILES][NUM_Y_TILES];

        final File demInstallDir = descriptor.getDemInstallDir();
        for (int x = 0; x < elevationFiles.length; x++) {
            final int minLon = x * DEGREE_RES - 180;
            for (int y = 0; y < elevationFiles[x].length; y++) {
                final int minLat = y * DEGREE_RES - 90;
                final String fileName = ACEElevationModelDescriptor.createTileFilename(minLat, minLon);
                final File localFile = new File(demInstallDir, fileName);
                elevationFiles[x][NUM_Y_TILES - 1 - y] = new ACEFile(this, localFile, productReaderPlugIn.createReaderInstance());
            }
        }
        return elevationFiles;
    }

    public void updateCache(ACEElevationTile tile) {
        elevationTileCache.remove(tile);
        elevationTileCache.add(0, tile);
        while (elevationTileCache.size() > 60) {
            final int index = elevationTileCache.size() - 1;
            final ACEElevationTile lastTile = elevationTileCache.get(index);
            lastTile.clearCache();
            elevationTileCache.remove(index);
        }
    }

    private static ACEReaderPlugIn getACEReaderPlugIn() {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(
                ACEReaderPlugIn.FORMAT_NAME);
        return (ACEReaderPlugIn) readerPlugIns.next();
    }
}
