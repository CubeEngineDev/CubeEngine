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
package de.cubeisland.engine.core.config.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cubeisland.engine.configuration.Configuration;
import de.cubeisland.engine.configuration.codec.ConfigurationCodec;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.node.BooleanNode;
import de.cubeisland.engine.configuration.node.ByteNode;
import de.cubeisland.engine.configuration.node.CharNode;
import de.cubeisland.engine.configuration.node.DoubleNode;
import de.cubeisland.engine.configuration.node.FloatNode;
import de.cubeisland.engine.configuration.node.IntNode;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.LongNode;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;
import de.cubeisland.engine.configuration.node.ShortNode;
import de.cubeisland.engine.configuration.node.StringNode;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.EndTag;
import org.spout.nbt.FloatTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.LongTag;
import org.spout.nbt.ShortTag;
import org.spout.nbt.StringTag;
import org.spout.nbt.Tag;
import org.spout.nbt.stream.NBTInputStream;
import org.spout.nbt.stream.NBTOutputStream;
import org.spout.nbt.util.NBTMapper;

public class NBTCodec extends ConfigurationCodec
{
    @Override
    public String getExtension()
    {
        return "dat";
    }

    @Override
    protected final void save(MapNode node, OutputStream os, Configuration config) throws ConversionException
    {
        try
        {
            NBTOutputStream nbtOutputStream = new NBTOutputStream(os, false);
            nbtOutputStream.writeTag(this.convertMap(node));
            nbtOutputStream.flush();
            nbtOutputStream.close();
        }
        catch (IOException e)
        {
            throw ConversionException.of(this, null, "Could not write into NBTOutputStream", e);
        }
    }

    @Override
    protected final MapNode load(InputStream is, Configuration config)
    {
        try
        {
            NBTInputStream nbtInputStream = new NBTInputStream(is, false);
            Tag tag = nbtInputStream.readTag();
            CompoundMap tags = NBTMapper.toTagValue(tag, CompoundMap.class, null);
            return this.toMapNode(tags);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private MapNode toMapNode(CompoundMap tags)
    {
        MapNode values = MapNode.emptyMap();
        for (Entry<String, Tag<?>> entry : tags.entrySet())
        {
            values.setExactNode(entry.getKey(), this.toNode(entry.getValue()));
        }
        return values;
    }

    private void toMapNode(MapNode values, CompoundMap tags)
    {
        for (Entry<String, Tag<?>> entry : tags.entrySet())
        {
            values.setExactNode(entry.getKey(),this.toNode(entry.getValue()));
        }
    }

    private Node toNode(Object value)
    {
        if (value instanceof Tag)
        {
            if (value instanceof CompoundTag)
            {
                MapNode mapNode = MapNode.emptyMap();
                this.toMapNode(mapNode,((CompoundTag)value).getValue());
                return mapNode;
            }
            else if (value instanceof ListTag)
            {
                ListNode listNode = ListNode.emptyList();
                for (Object o : ((ListTag)value).getValue())
                {
                    listNode.addNode(this.toNode(o));
                }
                return listNode;
            }
            else if (value instanceof ByteTag
                || value instanceof StringTag
                || value instanceof DoubleTag
                || value instanceof FloatTag
                || value instanceof IntTag
                || value instanceof LongTag
                || value instanceof ShortTag)
            {
                try
                {
                    return this.getConverterManager().convertToNode(((Tag)value).getValue());
                }
                catch (ConversionException e)
                {
                    throw new IllegalStateException("Could not convert a value!", e);
                }
            }
            else if (value instanceof EndTag)
            {
                return NullNode.emptyNode();
            }
        }
        throw new IllegalStateException("Unknown Tag! "+ value.getClass().getName());
    }

    private CompoundTag convertMap(MapNode baseNode)
    {
        LinkedHashMap<String,Node> map = baseNode.getMappedNodes();
        CompoundTag result = new CompoundTag("root", new CompoundMap());
        if (map.isEmpty()) return result;
        this.convertMap(result.getValue(),map);
        return result;
    }

    private void convertMap(CompoundMap rootMap, Map<String,Node> map)
    {
        for (Entry<String, Node> entry : map.entrySet())
        {
            rootMap.put(this.convertValue(entry.getKey(),entry.getValue()));
        }
    }

    private Tag convertValue(String name, Node value)
    {
        if (value instanceof MapNode)
        {
            CompoundMap map = new CompoundMap();
            this.convertMap(map,((MapNode)value).getMappedNodes());
            return new CompoundTag(name,map);
        }
        else if (value instanceof ListNode)
        {
            List<Tag> tagList = new ArrayList<>();
            java.lang.Integer i = 0;
            for (Node node : ((ListNode)value).getListedNodes())
            {
                i++;
                tagList.add(this.convertValue(i.toString(),node));
            }
            return new ListTag(name,Tag.class,tagList);
        }
        else if (value instanceof BooleanNode)
        {
            return new ByteTag(name, (Boolean)value.getValue());
        }
        else if (value instanceof ByteNode)
        {
            return new ByteTag(name, (Byte)value.getValue());
        }
        else if (value instanceof CharNode)
        {
            return new StringTag(name, value.getValue().toString());
        }
        else if (value instanceof DoubleNode)
        {
            return new DoubleTag(name, (Double)value.getValue());
        }
        else if (value instanceof FloatNode)
        {
            return new FloatTag(name, (Float)value.getValue());
        }
        else if (value instanceof IntNode)
        {
            return new IntTag(name, (Integer)value.getValue());
        }
        else if (value instanceof LongNode)
        {
            return new LongTag(name, (Long)value.getValue());
        }
        else if (value instanceof ShortNode)
        {
            return new ShortTag(name, (Short)value.getValue());
        }
        else if (value instanceof StringNode)
        {
            return new StringTag(name, (String)value.getValue());
        }
        else if (value instanceof NullNode)
        {
            return new EndTag();
        }
        throw new IllegalStateException("Unknown Node! "+ value.getClass().getName());
    }
}
