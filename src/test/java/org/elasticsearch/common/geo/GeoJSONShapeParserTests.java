/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.geo;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.ShapeCollection;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.spatial4j.core.shape.jts.JtsPoint;
import com.vividsolutions.jts.geom.*;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.test.hamcrest.ElasticsearchGeoAssertions;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.common.geo.builders.ShapeBuilder.SPATIAL_CONTEXT;


/**
 * Tests for {@link GeoJSONShapeParser}
 */
public class GeoJSONShapeParserTests extends ElasticsearchTestCase {

    private final static GeometryFactory GEOMETRY_FACTORY = SPATIAL_CONTEXT.getGeometryFactory();

    @Test
    public void testParse_simplePoint() throws IOException {
        String pointGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Point")
                .startArray("coordinates").value(100.0).value(0.0).endArray()
                .endObject().string();

        Point expected = GEOMETRY_FACTORY.createPoint(new Coordinate(100.0, 0.0));
        assertGeometryEquals(new JtsPoint(expected, SPATIAL_CONTEXT), pointGeoJson);
    }

    @Test
    public void testParse_lineString() throws IOException {
        String lineGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "LineString")
                .startArray("coordinates")
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> lineCoordinates = new ArrayList<>();
        lineCoordinates.add(new Coordinate(100, 0));
        lineCoordinates.add(new Coordinate(101, 1));

        LineString expected = GEOMETRY_FACTORY.createLineString(
                lineCoordinates.toArray(new Coordinate[lineCoordinates.size()]));
        assertGeometryEquals(jtsGeom(expected), lineGeoJson);
    }

    @Test
    public void testParse_multiLineString() throws IOException {
        String multilinesGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "MultiLineString")
                .startArray("coordinates")
                .startArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .endArray()
                .startArray()
                .startArray().value(102.0).value(2.0).endArray()
                .startArray().value(103.0).value(3.0).endArray()
                .endArray()
                .endArray()
                .endObject().string();

        MultiLineString expected = GEOMETRY_FACTORY.createMultiLineString(new LineString[]{
                GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                        new Coordinate(100, 0),
                        new Coordinate(101, 1),
                }),
                GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                        new Coordinate(102, 2),
                        new Coordinate(103, 3),
                }),
        });
        assertGeometryEquals(jtsGeom(expected), multilinesGeoJson);
    }

    @Test
    public void testParse_polygonNoHoles() throws IOException {
        String polygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Polygon")
                .startArray("coordinates")
                .startArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        Polygon expected = GEOMETRY_FACTORY.createPolygon(shell, null);
        assertGeometryEquals(jtsGeom(expected), polygonGeoJson);
    }

    @Test
    public void testParse_polygonWithHole() throws IOException {
        String polygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Polygon")
                .startArray("coordinates")
                .startArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .endArray()
                .startArray()
                .startArray().value(100.2).value(0.8).endArray()
                .startArray().value(100.2).value(0.2).endArray()
                .startArray().value(100.8).value(0.2).endArray()
                .startArray().value(100.8).value(0.8).endArray()
                .startArray().value(100.2).value(0.8).endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        List<Coordinate> holeCoordinates = new ArrayList<>();
        holeCoordinates.add(new Coordinate(100.2, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.2));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(
                shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        LinearRing[] holes = new LinearRing[1];
        holes[0] = GEOMETRY_FACTORY.createLinearRing(
                holeCoordinates.toArray(new Coordinate[holeCoordinates.size()]));
        Polygon expected = GEOMETRY_FACTORY.createPolygon(shell, holes);
        assertGeometryEquals(jtsGeom(expected), polygonGeoJson);
    }

    @Test
    public void testParse_multiPoint() throws IOException {
        String multiPointGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "MultiPoint")
                .startArray("coordinates")
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .endArray()
                .endObject().string();

        ShapeCollection expected = shapeCollection(
                SPATIAL_CONTEXT.makePoint(100, 0),
                SPATIAL_CONTEXT.makePoint(101, 1.0));
        assertGeometryEquals(expected, multiPointGeoJson);
    }

    @Test
    public void testParse_multiPolygon() throws IOException {
        String multiPolygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "MultiPolygon")
                .startArray("coordinates")
                .startArray()//first poly (without holes)
                .startArray()
                .startArray().value(102.0).value(2.0).endArray()
                .startArray().value(103.0).value(2.0).endArray()
                .startArray().value(103.0).value(3.0).endArray()
                .startArray().value(102.0).value(3.0).endArray()
                .startArray().value(102.0).value(2.0).endArray()
                .endArray()
                .endArray()
                .startArray()//second poly (with hole)
                .startArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .endArray()
                .startArray()//hole
                .startArray().value(100.2).value(0.8).endArray()
                .startArray().value(100.2).value(0.2).endArray()
                .startArray().value(100.8).value(0.2).endArray()
                .startArray().value(100.8).value(0.8).endArray()
                .startArray().value(100.2).value(0.8).endArray()
                .endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        List<Coordinate> holeCoordinates = new ArrayList<>();
        holeCoordinates.add(new Coordinate(100.2, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.2));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        LinearRing[] holes = new LinearRing[1];
        holes[0] = GEOMETRY_FACTORY.createLinearRing(holeCoordinates.toArray(new Coordinate[holeCoordinates.size()]));
        Polygon withHoles = GEOMETRY_FACTORY.createPolygon(shell, holes);

        shellCoordinates = new ArrayList<>();
        shellCoordinates.add(new Coordinate(102, 3));
        shellCoordinates.add(new Coordinate(103, 3));
        shellCoordinates.add(new Coordinate(103, 2));
        shellCoordinates.add(new Coordinate(102, 2));
        shellCoordinates.add(new Coordinate(102, 3));


        shell = GEOMETRY_FACTORY.createLinearRing(shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        Polygon withoutHoles = GEOMETRY_FACTORY.createPolygon(shell, null);

        Shape expected = shapeCollection(withoutHoles, withHoles);

        assertGeometryEquals(expected, multiPolygonGeoJson);
    }

    @Test
    public void testParse_geometryCollection() throws IOException {
        String geometryCollectionGeoJson = XContentFactory.jsonBuilder().startObject()
                .field("type","GeometryCollection")
                .startArray("geometries")
                    .startObject()
                        .field("type", "LineString")
                        .startArray("coordinates")
                            .startArray().value(100.0).value(0.0).endArray()
                            .startArray().value(101.0).value(1.0).endArray()
                        .endArray()
                    .endObject()
                    .startObject()
                        .field("type", "Point")
                        .startArray("coordinates").value(102.0).value(2.0).endArray()
                    .endObject()
                .endArray()
                .endObject()
                .string();

        Shape[] expected = new Shape[2];
        LineString expectedLineString = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(100, 0),
                new Coordinate(101, 1),
        });
        expected[0] = jtsGeom(expectedLineString);
        Point expectedPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(102.0, 2.0));
        expected[1] = new JtsPoint(expectedPoint, SPATIAL_CONTEXT);

        //equals returns true only if geometries are in the same order
        assertGeometryEquals(shapeCollection(expected), geometryCollectionGeoJson);
    }

    @Test
    public void testThatParserExtractsCorrectTypeAndCoordinatesFromArbitraryJson() throws IOException {
        String pointGeoJson = XContentFactory.jsonBuilder().startObject()
                .startObject("crs")
                    .field("type", "name")
                    .startObject("properties")
                        .field("name", "urn:ogc:def:crs:OGC:1.3:CRS84")
                    .endObject()
                .endObject()
                .field("bbox", "foobar")
                .field("type", "point")
                .field("bubu", "foobar")
                .startArray("coordinates").value(100.0).value(0.0).endArray()
                .startObject("nested").startArray("coordinates").value(200.0).value(0.0).endArray().endObject()
                .startObject("lala").field("type", "NotAPoint").endObject()
                .endObject().string();

        Point expected = GEOMETRY_FACTORY.createPoint(new Coordinate(100.0, 0.0));
        assertGeometryEquals(new JtsPoint(expected, SPATIAL_CONTEXT), pointGeoJson);
    }

    private void assertGeometryEquals(Shape expected, String geoJson) throws IOException {
        XContentParser parser = JsonXContent.jsonXContent.createParser(geoJson);
        parser.nextToken();
        ElasticsearchGeoAssertions.assertEquals(expected, ShapeBuilder.parse(parser).build());
    }

    private ShapeCollection<Shape> shapeCollection(Shape... shapes) {
        return new ShapeCollection<>(Arrays.asList(shapes), SPATIAL_CONTEXT);
    }

    private ShapeCollection<Shape> shapeCollection(Geometry... geoms) {
        List<Shape> shapes = new ArrayList<>(geoms.length);
        for (Geometry geom : geoms) {
            shapes.add(jtsGeom(geom));
        }
        return new ShapeCollection<>(shapes, SPATIAL_CONTEXT);
    }

    private JtsGeometry jtsGeom(Geometry geom) {
        return new JtsGeometry(geom, SPATIAL_CONTEXT, false, false);
    }

}
