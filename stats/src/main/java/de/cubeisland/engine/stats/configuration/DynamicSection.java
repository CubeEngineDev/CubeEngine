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
package de.cubeisland.engine.stats.configuration;

import de.cubeisland.engine.configuration.StringUtils;
import de.cubeisland.engine.configuration.codec.ConverterManager;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;

public class DynamicSection
{
    private final ConverterManager cm;
    private final MapNode parent;

    public DynamicSection(ConverterManager cm)
    {
        this.cm = cm;
        this.parent = MapNode.emptyMap();
    }

    public DynamicSection(ConverterManager cm, MapNode source)
    {
        this.cm = cm;
        this.parent = source;
    }

    public void put(String key, Object value) throws ConversionException
    {
        this.put(key, value, new String[]{});
    }

    public void put(String key, Object value, String... comment) throws ConversionException
    {
        Node node = cm.convertToNode(value);
        node.setComments(comment);
        parent.setExactNode(StringUtils.fieldNameToPath(key), node);
    }

    /**
     * Check if there is an entry in this section with that key
     * @param key
     * @return
     */
    public boolean hasKey(String key)
    {
        return parent.getMappedNodes().containsKey(StringUtils.fieldNameToPath(key));
    }

    /**
     * Get the value with the given key
     *
     * @param key The key to look up the value by
     * @return
     */
    public Object get(String key)
    {
        Node node = parent.getMappedNodes().get(StringUtils.fieldNameToPath(key));
        if (node instanceof NullNode)
        {
            return null;
        }
        return node.getValue();
    }

    /**
     * Get the value with the given key and Convert it with the converter for the given class
     *
     * @param key  The key to look up the value by
     * @param type The type to look up a converter for
     * @return The stored value, or null if this Section didn't have a value with that key
     * @throws ConversionException
     */
    public Object get(String key, Class<?> type) throws ConversionException
    {

        Node node = parent.getMappedNodes().get(StringUtils.fieldNameToPath(key));
        if (node == null)
        {
            return null;
        }
        Converter converter = cm.matchConverter(type);
        return converter.fromNode(node, cm);
    }

    public MapNode getMapNode()
    {
        return this.parent;
    }

}
