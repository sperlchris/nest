package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.glayer.RasterImageLayerType;

import java.awt.geom.AffineTransform;

public class RasterImageLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public RasterImageLayerConfigurationPersistencyTest() {
        super(LayerType.getLayerType(RasterImageLayerType.class.getName()));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final ValueContainer configuration = layerType.getConfigurationTemplate();
        final Band raster = getProductManager().getProduct(0).getBandAt(0);
        configuration.setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, raster);
        configuration.setValue("imageToModelTransform", new AffineTransform());

        return layerType.createLayer(null, configuration);
    }
}