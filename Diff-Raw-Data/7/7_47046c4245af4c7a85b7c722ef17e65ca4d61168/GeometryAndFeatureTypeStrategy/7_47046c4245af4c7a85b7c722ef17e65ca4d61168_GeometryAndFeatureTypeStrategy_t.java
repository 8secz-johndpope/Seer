 /*
  * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 
 package org.esa.beam.dataio.geometry;
 
 import com.bc.ceres.binding.ConversionException;
 import com.bc.ceres.binding.Converter;
 import com.bc.ceres.binding.ConverterRegistry;
 import com.vividsolutions.jts.geom.Geometry;
 import org.esa.beam.framework.datamodel.GeoCoding;
 import org.geotools.feature.simple.SimpleFeatureBuilder;
 import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
 import org.opengis.feature.simple.SimpleFeature;
 import org.opengis.feature.simple.SimpleFeatureType;
 
 import java.io.IOException;
 
 /**
  * @author Olaf Danne
  * @author Thomas Storm
  */
 class GeometryAndFeatureTypeStrategy implements GeometryStrategy {
 
     private GeoCoding geoCoding;
     private String geometryName;
     private String featureTypeName;
 
     GeometryAndFeatureTypeStrategy(GeoCoding geoCoding, String geometryName, String featureTypeName) {
         this.geoCoding = geoCoding;
         this.geometryName = geometryName;
         this.featureTypeName = featureTypeName;
     }
 
     @Override
     public void setDefaultGeometry(SimpleFeatureTypeBuilder builder) {
         builder.setDefaultGeometry(geometryName);
     }
 
     @Override
     public void setName(SimpleFeatureTypeBuilder builder) {
         builder.setName(featureTypeName);
     }
 
     @Override
     public int computeExpectedTokenCount(int attributeCount) {
         return attributeCount + 1;  // (has feature type name);
     }
 
     @Override
     public void interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException {
         for (int columnIndex = 1; columnIndex < tokens.length; columnIndex++) {
             String token = tokens[columnIndex];
             token = VectorDataNodeIO.decodeTabString(token);
            int attributeIndex = columnIndex - 1;
             Object value = null;
             if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                Class<?> attributeType = simpleFeatureType.getType(attributeIndex).getBinding();
                 ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
                 Converter<?> converter = converterRegistry.getConverter(attributeType);
                 if (converter == null) {
                     throw new IOException(String.format("No converter for type %s found.", attributeType));
                 }
                 value = converter.parse(token);
             }
            builder.set(simpleFeatureType.getDescriptor(attributeIndex).getLocalName(), value);
         }
     }
 
     @Override
     public void transformGeoPosToPixelPos(SimpleFeature simpleFeature) {
         Geometry defaultGeometry = (Geometry) simpleFeature.getDefaultGeometry();
         defaultGeometry.apply(new GeoPosToPixelPosFilter(defaultGeometry.getCoordinates().length, geoCoding));
     }
 
     @Override
     public String getFeatureId(String[] tokens) {
         return tokens[0];
     }
 }
