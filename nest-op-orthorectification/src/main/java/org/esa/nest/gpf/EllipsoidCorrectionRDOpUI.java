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
package org.esa.nest.gpf;

import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.util.Map;

/**
 * User interface for EllipsoidCorrectionRDOp
 */
public class EllipsoidCorrectionRDOpUI extends RangeDopplerGeocodingOpUI {

    private final JList bandList = new JList();
    private final JComboBox imgResamplingMethod = new JComboBox(new String[] {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                                                                           ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                                                                           ResamplingFactory.CUBIC_CONVOLUTION_NAME});
    private final JButton crsButton = new JButton();
    private final MapProjectionHandler mapProjHandler = new MapProjectionHandler();

    public EllipsoidCorrectionRDOpUI() {
        useAvgSceneHeight = true;
    }

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        final JComponent pane = super.CreateOpTab(operatorName, parameterMap, appContext);

        return pane;
    }

    @Override
    public void initParameters() {
        super.initParameters();
    }

    @Override
    public void updateParameters() {

        super.updateParameters();
    }


}