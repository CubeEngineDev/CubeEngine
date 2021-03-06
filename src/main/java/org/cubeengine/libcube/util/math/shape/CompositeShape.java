/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.libcube.util.math.shape;

import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CompositeShape implements Shape {

    // TODO implement me!!!

    private List<Shape> positiveShapes;
    private List<Shape> negativeShapes;

    public CompositeShape(Shape... positiveShapes)
    {
        this(Arrays.asList(positiveShapes), new ArrayList<>());
    }

    public CompositeShape(List<Shape> positiveShapes, List<Shape> negativeShapes)
    {
        this.positiveShapes = positiveShapes;
        this.negativeShapes = negativeShapes;
    }

    @Override
    public Shape setPoint(Vector3d point)
    {
        return this;
    }

    @Override
    public Vector3d getPoint()
    {
        return Vector3d.ZERO;
    }

    @Override
    public Shape rotate(Vector3d angle)
    {
        return null;
    }

    @Override
    public Shape setCenterOfRotation(Vector3d center)
    {
        return null;
    }

    @Override
    public Vector3d getRotationAngle()
    {
        return null;
    }

    @Override
    public Vector3d getCenterOfRotation()
    {
        return null;
    }

    @Override
    public Shape scale(Vector3d vector)
    {
        return null;
    }

    @Override
    public boolean contains(Vector3d point)
    {
        return false;
    }

    @Override
    public boolean contains(double x, double y, double z)
    {

        return false;
    }

    @Override
    public boolean intersects(Shape other)
    {
        return false;
    }

    @Override
    public boolean contains(Shape other)
    {
        return false;
    }

    @Override
    public Cuboid getBoundingCuboid()
    {
        return null;
    }

    @Override
    public Iterator<Vector3d> iterator()
    {
        return null;
    }
}
