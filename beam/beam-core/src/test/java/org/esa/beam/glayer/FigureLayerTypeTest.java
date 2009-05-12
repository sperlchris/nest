package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.draw.LineFigure;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;

public class FigureLayerTypeTest extends LayerTypeTest {

    public FigureLayerTypeTest() {
        super(FigureLayerType.class);
    }

    @Test
    public void testConfigurationTemplate() {
        final ValueContainer template = getLayerType().getConfigurationTemplate();

        assertNotNull(template);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_TRANSFORM, AffineTransform.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_FIGURE_LIST, ArrayList.class);

    }

    @Test
    public void testCreateLayer() throws ValidationException {
        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);

        final ValueContainer config = getLayerType().getConfigurationTemplate();
        final ArrayList figureList = new ArrayList();
        figureList.add(new LineFigure(new Rectangle(0, 0, 10, 10), Collections.EMPTY_MAP));
        config.setValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST, figureList);
        config.setValue(FigureLayer.PROPERTY_NAME_TRANSFORM, new AffineTransform());

        final Layer layer = getLayerType().createLayer(null, config);
        assertNotNull(layer);
        assertSame(getLayerType(), layer.getLayerType());
        assertTrue(layer instanceof FigureLayer);
    }
}