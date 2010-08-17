/*
 * Copyright (C) 2010 Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Test ERS CEOS Product Reader.
 *
 * @author lveci
 */
public class TestImageIOReader extends TestCase {

    ImageIOReaderPlugIn readerPlugin;
    ProductReader reader;

    String filePath = "P:\\nest\\nest\\ESA Data\\Other\\Imagefiles\\Submarine_operators_countries.png";

    public TestImageIOReader(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

        readerPlugin = new ImageIOReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    public void tearDown() throws Exception {
        super.tearDown();

        reader = null;
        readerPlugin = null;
    }

    public void testImageIO() throws IOException
    {
        String[] readerFormats = ImageIO.getReaderFormatNames();
        String[] readerSuffixes = ImageIO.getReaderFileSuffixes();
        String[] writerFormats = ImageIO.getWriterFormatNames();
        String[] writerSuffixes = ImageIO.getWriterFileSuffixes();

        for(String s : readerFormats)
            System.out.println("ImageIOreader: " + s);
        for(String s : readerSuffixes)
            System.out.println("ImageIOreaderSuffix: " + s);
        for(String s : writerFormats)
            System.out.println("ImageIOwriter: " + s);
        for(String s : writerSuffixes)
            System.out.println("ImageIOwriterSuffix: " + s);
    }

    public void testOpen() throws IOException
    {
        File file = new File(filePath);
        if(!file.exists()) return;

        Product product = reader.readProductNodes(file, null);
    }

}