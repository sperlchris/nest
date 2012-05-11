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
package org.esa.nest.dataio.dem;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseElevationModel implements ElevationModel, Resampling.Raster {

    protected final int NUM_X_TILES;
    protected final int NUM_Y_TILES;
    protected final int NUM_PIXELS_PER_TILE;
    protected final float NO_DATA_VALUE;
    protected final int DEGREE_RES;
    protected final int RASTER_WIDTH;
    protected final int RASTER_HEIGHT;
    protected final float DEGREE_RES_BY_NUM_PIXELS_PER_TILE;

    protected final ElevationModelDescriptor descriptor;
    private final ElevationFile[][] elevationFiles;
    private final Resampling resampling;
    private final Resampling.Index resamplingIndex;
    private final Resampling.Raster resamplingRaster;

    private final List<ElevationTile> elevationTileCache = new ArrayList<ElevationTile>(20);
    private int maxCacheSize = 60;

    public BaseElevationModel(final ElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        this.descriptor = descriptor;
        this.resampling = resamplingMethod;
        this.resamplingIndex = resampling.createIndex();
        this.resamplingRaster = this;

        NUM_X_TILES = descriptor.getNumXTiles();
        NUM_Y_TILES = descriptor.getNumYTiles();
        NO_DATA_VALUE = descriptor.getNoDataValue();
        NUM_PIXELS_PER_TILE = descriptor.getPixelRes();
        DEGREE_RES = descriptor.getDegreeRes();

        RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
        RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

        DEGREE_RES_BY_NUM_PIXELS_PER_TILE = DEGREE_RES / (float)NUM_PIXELS_PER_TILE;

        elevationFiles = createElevationFiles();    // must be last
    }

    public Resampling getResampling() {
        return resampling;
    }

    public ElevationModelDescriptor getDescriptor() {
        return descriptor;
    }

    protected void setMaxCacheSize(final int size) {
        maxCacheSize = size;
    }

    public final synchronized float getElevation(final GeoPos geoPos) throws Exception {
        final PixelPos pixel = getIndex(geoPos);
        if (pixel.y < 0) {
            return NO_DATA_VALUE;
        }

        resampling.computeIndex(pixel.x, pixel.y, RASTER_WIDTH, RASTER_HEIGHT, resamplingIndex);

        final float elevation = resampling.resample(resamplingRaster, resamplingIndex);
        if (Float.isNaN(elevation)) {
            return NO_DATA_VALUE;
        }
        return elevation;
    }

    public abstract PixelPos getIndex(final GeoPos geoPos);

    public abstract GeoPos getGeoPos(final PixelPos pixelPos);

    public void dispose() {
        for (ElevationTile tile : elevationTileCache) {
            tile.dispose();
        }
        elevationTileCache.clear();
        for (ElevationFile[] elevationFile : elevationFiles) {
            for (ElevationFile anElevationFile : elevationFile) {
                anElevationFile.dispose();
            }
        }
    }

    public int getWidth() {
        return RASTER_WIDTH;
    }

    public int getHeight() {
        return RASTER_HEIGHT;
    }

    public final float getSample(final int pixelX, final int pixelY) throws IOException {
        final int tileXIndex = pixelX / NUM_PIXELS_PER_TILE;
        final int tileYIndex = pixelY / NUM_PIXELS_PER_TILE;
        final ElevationTile tile = elevationFiles[tileXIndex][tileYIndex].getTile();
        if (tile == null) {
            return Float.NaN;
        }
        final float sample = tile.getSample(pixelX - tileXIndex * NUM_PIXELS_PER_TILE, 
                                            pixelY - tileYIndex * NUM_PIXELS_PER_TILE);
        if (sample == NO_DATA_VALUE)
            return Float.NaN;
        return sample;
    }

    private ElevationFile[][] createElevationFiles() {
        final ElevationFile[][] elevationFiles = new ElevationFile[NUM_X_TILES][NUM_Y_TILES];
        final File demInstallDir = descriptor.getDemInstallDir();
        for (int x = 0; x < elevationFiles.length; x++) {
            for (int y = 0; y < elevationFiles[x].length; y++) {

                createElevationFile(elevationFiles, x, y, demInstallDir);
            }
        }
        return elevationFiles;
    }

    protected abstract void createElevationFile(final ElevationFile[][] elevationFiles,
                                                final int x, final int y, final File demInstallDir);

    public void updateCache(final ElevationTile tile) {
        elevationTileCache.remove(tile);
        elevationTileCache.add(0, tile);
        while (elevationTileCache.size() > maxCacheSize) {
            final int index = elevationTileCache.size() - 1;
            final ElevationTile lastTile = elevationTileCache.get(index);
            lastTile.clearCache();
            elevationTileCache.remove(index);
        }
    }

    protected static ProductReaderPlugIn getReaderPlugIn(final String formatName) {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName);
        return (ProductReaderPlugIn) readerPlugIns.next();
    }
}