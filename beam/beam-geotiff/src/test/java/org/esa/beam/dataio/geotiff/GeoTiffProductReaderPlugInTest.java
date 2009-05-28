package org.esa.beam.dataio.geotiff;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.util.io.BeamFileFilter;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: 1.2 $ $Date: 2009-05-28 14:17:58 $
 * @since BEAM 4.2
 */
public class GeoTiffProductReaderPlugInTest {

    private GeoTiffProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new GeoTiffProductReaderPlugIn();
    }

    @Test
    public void testDecodeQualificationForTIFFWithoutGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        final ImageInputStream inputStream = writeToInputStream(product);
        final DecodeQualification decodeQualification = GeoTiffProductReaderPlugIn.getDecodeQualificationImpl(
                inputStream);

        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
    }

    @Test
    public void testDecodeQualificationForTIFFWithGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        final MapInfo mapInfo = new MapInfo(UTM.createProjection(26, true), 0, 0, 0, 0, 1, 1, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
        final ImageInputStream inputStream = writeToInputStream(product);
        final DecodeQualification decodeQualification = GeoTiffProductReaderPlugIn.getDecodeQualificationImpl(inputStream);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);
    }

    @Test
    public void testFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        final List<String> extensionList = Arrays.asList(fileExtensions);
        assertEquals(2, extensionList.size());
        assertEquals(true, extensionList.contains(".tif"));
        assertEquals(true, extensionList.contains(".tiff"));
    }

    @Test
    public void testFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("GeoTIFF", formatNames[0]);
    }

    @Test
    public void testOutputTypes() {
        final Class[] classes = plugIn.getInputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    private static ImageInputStream writeToInputStream(Product product) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final ImageOutputStream outputStream = new MemoryCacheImageOutputStream(byteStream);
        final GeoTiffProductWriter writer = (GeoTiffProductWriter) new GeoTiffProductWriterPlugIn().createWriterInstance();
        product.setProductWriter(writer);
        writer.writeGeoTIFFProduct(outputStream, product);
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            band.writeRasterDataFully(ProgressMonitor.NULL);
        }
        outputStream.flush();
        return new MemoryCacheImageInputStream(new ByteArraySeekableStream(byteStream.toByteArray()));
    }

}