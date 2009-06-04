/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * This operator performs the same terrain correction as RangeDopplerGeocodingOp does except that it uses average
 * scene height from metadata instead of scene height from DEM.
 */

@OperatorMetadata(alias="Ellipsoid-Correction-RD", description="Ellipsoid correction with RD method and average scene height")
public final class EllipsoidCorrectionRDOp extends RangeDopplerGeocodingOp {

    @SourceProduct(alias="source")
    private Product sourceProduct2;
    @TargetProduct
    private Product targetProduct2;

    @Override
    public void initialize() throws OperatorException {
        sourceProduct = sourceProduct2;
        useAvgSceneHeight = true;
        super.initialize();
        targetProduct2 = targetProduct;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(EllipsoidCorrectionRDOp.class);
            setOperatorUI(EllipsoidCorrectionRDOpUI.class);
        }
    }
}