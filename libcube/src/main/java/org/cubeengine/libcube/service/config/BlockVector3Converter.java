/**
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
package org.cubeengine.libcube.service.config;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.converter.SingleClassConverter;
import org.cubeengine.converter.node.IntNode;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.libcube.util.math.BlockVector3;

public class BlockVector3Converter extends SingleClassConverter<BlockVector3>
{
    @Override
    public Node toNode(BlockVector3 blockVector3, ConverterManager converterManager) throws ConversionException
    {
        MapNode mapNode = MapNode.emptyMap();
        mapNode.set("x", new IntNode(blockVector3.x));
        mapNode.set("y", new IntNode(blockVector3.y));
        mapNode.set("z", new IntNode(blockVector3.z));
        return mapNode;
    }

    @Override
    public BlockVector3 fromNode(Node node, ConverterManager converterManager) throws ConversionException
    {
        if (node instanceof MapNode)
        {
            Node x = ((MapNode)node).get("x");
            Node y = ((MapNode)node).get("y");
            Node z = ((MapNode)node).get("z");
            return new BlockVector3((Integer)converterManager.convertFromNode(x, Integer.class),
                                    (Integer)converterManager.convertFromNode(y, Integer.class),
                                    (Integer)converterManager.convertFromNode(z, Integer.class));
        }
        throw ConversionException.of(this, node, "Node is not a MapNode!");
    }
}
