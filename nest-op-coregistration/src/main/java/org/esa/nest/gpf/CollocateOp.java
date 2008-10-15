package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * The "Collocate" operator.
 *
 */
@OperatorMetadata(alias = "Collocator",
                  description = "Collocates two products based on their geo-codings.")
public class CollocateOp extends Operator {

    public static final String SOURCE_NAME_REFERENCE = "${ORIGINAL_NAME}";
    public static final String DEFAULT_MASTER_COMPONENT_PATTERN = "${ORIGINAL_NAME}_M";
    public static final String DEFAULT_SLAVE_COMPONENT_PATTERN = "${ORIGINAL_NAME}_S";
    private static final String NEAREST_NEIGHBOUR = "NEAREST_NEIGHBOUR";
    private static final String BILINEAR_INTERPOLATION = "BILINEAR_INTERPOLATION";
    private static final String CUBIC_CONVOLUTION = "CUBIC_CONVOLUTION";

    @SourceProducts(count = 2)
    private Product[] sourceProduct;

    private Product masterProduct;
    private Product slaveProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct;

    @Parameter(valueSet = {NEAREST_NEIGHBOUR, BILINEAR_INTERPOLATION, CUBIC_CONVOLUTION},
               defaultValue = BILINEAR_INTERPOLATION, description = "The method to be used when resampling the slave grid onto the master grid.",
               label="Resampling Type")
    private String resamplingType;

    private transient Map<Band, RasterDataNode> sourceRasterMap;

    public Product getMasterProduct() {
        return masterProduct;
    }

    public void setMasterProduct(Product masterProduct) {
        this.masterProduct = masterProduct;
    }

    public Product getSlaveProduct() {
        return slaveProduct;
    }

    public void setSlaveProduct(Product slaveProduct) {
        this.slaveProduct = slaveProduct;
    }

    @Override
    public void initialize() throws OperatorException {
        masterProduct = sourceProduct[0];
        slaveProduct = sourceProduct[1];

        if (masterProduct.getGeoCoding() == null) {
            throw new OperatorException(
                    MessageFormat.format("Product ''{0}'' has no geo-coding.", masterProduct.getName()));
        }
        if (slaveProduct.getGeoCoding() == null) {
            throw new OperatorException(
                    MessageFormat.format("Product ''{0}'' has no geo-coding.", slaveProduct.getName()));
        }

        sourceRasterMap = new HashMap<Band, RasterDataNode>(31);

        targetProduct = new Product(slaveProduct.getName(),
                                    masterProduct.getProductType(),
                                    masterProduct.getSceneRasterWidth(),
                                    masterProduct.getSceneRasterHeight());

        final ProductData.UTC utc1 = masterProduct.getStartTime();
        if (utc1 != null) {
            targetProduct.setStartTime(new ProductData.UTC(utc1.getMJD()));
        }
        final ProductData.UTC utc2 = masterProduct.getEndTime();
        if (utc2 != null) {
            targetProduct.setEndTime(new ProductData.UTC(utc2.getMJD()));
        }

        ProductUtils.copyMetadata(masterProduct, targetProduct);
        ProductUtils.copyTiePointGrids(masterProduct, targetProduct);
        ProductUtils.copyGeoCoding(masterProduct, targetProduct);

        for (final Band sourceBand : slaveProduct.getBands()) {
            String targetBandName = sourceBand.getName();
            //final Band targetBand = targetProduct.addBand(targetBandName, sourceBand.getDataType());
            final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
            sourceRasterMap.put(targetBand, sourceBand);
        }

        for (final Band targetBandOuter : targetProduct.getBands()) {
            for (final Band targetBandInner : targetProduct.getBands()) {
                final RasterDataNode sourceRaster = sourceRasterMap.get(targetBandInner);
                if (sourceRaster != null) {
                    if (sourceRaster.getProduct() == slaveProduct) {
                        targetBandOuter.updateExpression(
                                BandArithmetic.createExternalName(sourceRaster.getName()),
                                BandArithmetic.createExternalName(targetBandInner.getName()));
                    }
                }
            }
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Collocating bands...", targetProduct.getNumBands() + 1);
        try {
            final PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                    slaveProduct.getGeoCoding(),
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight(),
                    masterProduct.getGeoCoding(),
                    targetRectangle);
            final Rectangle sourceRectangle = getBoundingBox(
                    sourcePixelPositions,
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight());
            pm.done();

            for (final Band targetBand : targetProduct.getBands()) {
                checkForCancelation(pm);
                final RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);
                final Tile targetTile = targetTileMap.get(targetBand);

                if (sourceRaster.getProduct() == slaveProduct) {
                    collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile,
                                        SubProgressMonitor.create(pm, 1));
                } else {
                    targetTile.setRawSamples(getSourceTile(sourceRaster, targetTile.getRectangle(), pm).getRawSamples());
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);

        if (sourceRaster.getProduct() == slaveProduct) {
            final PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                    slaveProduct.getGeoCoding(),
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight(),
                    masterProduct.getGeoCoding(),
                    targetTile.getRectangle());
            final Rectangle sourceRectangle = getBoundingBox(
                    sourcePixelPositions,
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight());

            collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile, pm);
        } else {
            targetTile.setRawSamples(getSourceTile(sourceRaster, targetTile.getRectangle(), pm).getRawSamples());
        }
    }

    @Override
    public void dispose() {
        sourceRasterMap = null;
    }

    private void collocateSourceBand(RasterDataNode sourceBand, Rectangle sourceRectangle, PixelPos[] sourcePixelPositions,
                                     Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask(MessageFormat.format("collocating band {0}", sourceBand.getName()), targetTile.getHeight());
        try {
            final RasterDataNode targetBand = targetTile.getRasterDataNode();
            final Rectangle targetRectangle = targetTile.getRectangle();

            final int sourceRasterHeight = slaveProduct.getSceneRasterHeight();
            final int sourceRasterWidth = slaveProduct.getSceneRasterWidth();

            final Resampling resampling;
            if (isFlagBand(sourceBand) || isValidPixelExpressionUsed(sourceBand)) {
                resampling = Resampling.NEAREST_NEIGHBOUR;
            } else {
                if(resamplingType.equals(NEAREST_NEIGHBOUR))
                    resampling = Resampling.NEAREST_NEIGHBOUR;
                else if(resamplingType.equals(BILINEAR_INTERPOLATION))
                    resampling = Resampling.BILINEAR_INTERPOLATION;
                else 
                    resampling = (Resampling.CUBIC_CONVOLUTION);
            }
            final Resampling.Index resamplingIndex = resampling.createIndex();
            final float noDataValue = (float) targetBand.getGeophysicalNoDataValue();

            if (sourceRectangle != null) {
                final Tile sourceTile = getSourceTile(sourceBand, sourceRectangle, pm);
                final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        checkForCancelation(pm);
                        final PixelPos sourcePixelPos = sourcePixelPositions[index];

                        if (sourcePixelPos != null) {
                            resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                                    sourceRasterWidth, sourceRasterHeight, resamplingIndex);
                            try {
                                float sample = resampling.resample(resamplingRaster, resamplingIndex);
                                if (Float.isNaN(sample)) {
                                    sample = noDataValue;
                                }
                                targetTile.setSample(x, y, sample);
                            } catch (Exception e) {
                                throw new OperatorException(e.getMessage());
                            }
                        } else {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                    pm.worked(1);
                }
            } else {
                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        checkForCancelation(pm);
                        targetTile.setSample(x, y, noDataValue);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private static Rectangle getBoundingBox(PixelPos[] pixelPositions, int maxWidth, int maxHeight) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final PixelPos pixelsPos : pixelPositions) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 2, 0);
        maxX = Math.min(maxX + 2, maxWidth - 1);
        minY = Math.max(minY - 2, 0);
        maxY = Math.min(maxY + 2, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isFlagBand(RasterDataNode sourceRaster) {
        return (sourceRaster instanceof Band && ((Band) sourceRaster).isFlagBand());
    }

    private static boolean isValidPixelExpressionUsed(RasterDataNode sourceRaster) {
        final String validPixelExpression = sourceRaster.getValidPixelExpression();
        return validPixelExpression != null && !validPixelExpression.trim().isEmpty();
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final boolean usesNoData;

        public ResamplingRaster(Tile tile) {
            this.tile = tile;

            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            usesNoData = rasterDataNode.isNoDataValueUsed();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public final float getSample(int x, int y) throws Exception {
            final double sample = tile.getSampleDouble(x, y);

            if (usesNoData && isNoDataValue(sample)) {
                return Float.NaN;
            }

            return (float) sample;
        }

        private boolean isNoDataValue(double sample) {
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();

            if (rasterDataNode.isNoDataValueUsed()) {
                if (rasterDataNode.isScalingApplied()) {
                    return rasterDataNode.getGeophysicalNoDataValue() == sample;
                } else {
                    return rasterDataNode.getNoDataValue() == sample;
                }
            }

            return false;
        }
    }

    /**
     * Collocation operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CollocateOp.class);
        }
    }
}