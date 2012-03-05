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
package org.esa.nest.dataio.ceos.alos;

import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.ceos.CEOSLeaderFile;
import org.esa.nest.dataio.ceos.CeosRecordHeader;
import org.jdom.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This class represents a leader file of a product.
 *
 */
class AlosPalsarLeaderFile extends CEOSLeaderFile {

    protected final static String mission = "alos";
    private final static String leader_recordDefinitionFile = "leader_file.xml";

    private int productLevel = -1;

    private final static Document leaderXML = BinaryDBReader.loadDefinitionFile(mission, leader_recordDefinitionFile);
    private final static Document sceneXML = BinaryDBReader.loadDefinitionFile(mission, scene_recordDefinitionFile);
    private final static Document mapProjXML = BinaryDBReader.loadDefinitionFile(mission, mapproj_recordDefinitionFile);
    private final static Document platformXML = BinaryDBReader.loadDefinitionFile(mission, platformPosition_recordDefinitionFile);
    private final static Document attitudeXML = BinaryDBReader.loadDefinitionFile(mission, attitude_recordDefinitionFile);
    private final static Document radiometricXML = BinaryDBReader.loadDefinitionFile(mission, radiometric_recordDefinitionFile);
    private final static Document dataQualityXML = BinaryDBReader.loadDefinitionFile(mission, dataQuality_recordDefinitionFile);
    private final static Document facilityXML = BinaryDBReader.loadDefinitionFile(mission, facility_recordDefinitionFile);

    public AlosPalsarLeaderFile(final ImageInputStream stream) throws IOException {
        this(stream, leaderXML);
    }

    public AlosPalsarLeaderFile(final ImageInputStream stream, final Document fdrXML) throws IOException {

        final BinaryFileReader reader = new BinaryFileReader(stream);

        CeosRecordHeader header = new CeosRecordHeader(reader);
        _leaderFDR = new BinaryRecord(reader, -1, fdrXML);
        header.seekToEnd();

        for(int i=0; i < _leaderFDR.getAttributeInt("Number of data set summary records"); ++i) {
            header = new CeosRecordHeader(reader);
            _sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of map projection data records"); ++i) {
            header = new CeosRecordHeader(reader);
            _mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of platform pos. data records"); ++i) {
            header = new CeosRecordHeader(reader);
            _platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platformPosition_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of attitude data records"); ++i) {
            header = new CeosRecordHeader(reader);
            _attitudeRecord = new BinaryRecord(reader, -1, attitudeXML, attitude_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of radiometric data records"); ++i) {
            header = new CeosRecordHeader(reader);
            _radiometricRecord = new BinaryRecord(reader, -1, radiometricXML, radiometric_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of data quality summary records"); ++i) {
            header = new CeosRecordHeader(reader);
            _dataQualityRecord = new BinaryRecord(reader, -1, dataQualityXML, dataQuality_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of facility data records"); ++i) {
            header = new CeosRecordHeader(reader);
            int facilityRecordNum = 17;
            int level = getProductLevel();
            if(level != AlosPalsarConstants.LEVEL1_0 && level != AlosPalsarConstants.LEVEL1_1)
                facilityRecordNum = 18;
            while(header.getRecordNum() < facilityRecordNum) {
                header.seekToEnd();
                header = new CeosRecordHeader(reader);
            }

            _facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
            header.seekToEnd();
        }
        reader.close();
    }

    public final int getProductLevel() {
        if(productLevel < 0) {
            String level = _sceneHeaderRecord.getAttributeString("Product level code").trim();
            if(level.contains("1.5"))
                productLevel = AlosPalsarConstants.LEVEL1_5;
            else if(level.contains("1.1"))
                productLevel = AlosPalsarConstants.LEVEL1_1;
            else if(level.contains("1.0"))
                productLevel = AlosPalsarConstants.LEVEL1_0;
            else if(level.contains("4.1"))
                productLevel = AlosPalsarConstants.LEVEL4_1;
            else if(level.contains("4.2"))
                productLevel = AlosPalsarConstants.LEVEL4_2;
        }
        return productLevel;
    }

    public String getProductType() {
        return _sceneHeaderRecord.getAttributeString("Product type specifier");
    }
}