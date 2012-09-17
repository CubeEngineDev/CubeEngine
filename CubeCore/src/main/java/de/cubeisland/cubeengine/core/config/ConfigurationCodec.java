package de.cubeisland.cubeengine.core.config;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.config.annotations.Comment;
import de.cubeisland.cubeengine.core.config.annotations.MapComment;
import de.cubeisland.cubeengine.core.config.annotations.MapComments;
import de.cubeisland.cubeengine.core.config.annotations.Option;
import de.cubeisland.cubeengine.core.config.annotations.Revision;
import de.cubeisland.cubeengine.core.config.annotations.Updater;
import de.cubeisland.cubeengine.core.util.StringUtils;
import de.cubeisland.cubeengine.core.util.Validate;
import de.cubeisland.cubeengine.core.util.converter.ConversionException;
import de.cubeisland.cubeengine.core.util.converter.Convert;
import de.cubeisland.cubeengine.core.util.converter.Converter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Anselm Brehme
 */
public abstract class ConfigurationCodec
{
    public String COMMENT_PREFIX;
    public String SPACES;
    public String LINEBREAK;
    public String QUOTE;
    protected HashMap<String, String> comments;
    protected boolean first;
    protected Integer revision = null;
    private static final Logger logger = CubeEngine.getLogger();
    protected HashMap<String, String> loadedKeys;

    public ConfigurationCodec()
    {
        this.comments = new HashMap<String, String>();
    }

    /**
     * Converts the object to fit into the field
     *
     * @param field the field
     * @param object the object to deserialize
     * @return the deserialized object
     */
    public Object convertTo(Configuration config, Field field, Object object)
    {
        Converter converter = Convert.matchConverter(field.getType());
        try
        {
            if (converter == null)
            {
                Class<?> genericType = field.getAnnotation(Option.class).genericType();
                //Converts Collection / Map / Array  of genericType OR cast object into fieldClass
                return Convert.fromObject(field.getType(), field.get(config), object, genericType);
            }
            return converter.fromObject(object);
        }
        catch (ConversionException ex)
        {
            logger.log(Level.WARNING, "Error while converting", ex);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error while converting", e);
        }
        return object;
    }

    /**
     * Converts the field to fit into the object
     *
     * @param field the field to serialize
     * @param object the object
     * @return the serialized fieldvalue
     */
    public Object convertFrom(Field field, Object object)
    {
        try
        {
            if (Configuration.class.isAssignableFrom(field.getType()))
            {
                return this.saveIntoMap((Configuration)object, field.getAnnotation(Option.class).value());
            }
            if (object == null)
            {
                return null;
            }
            Converter converter = Convert.matchConverter(object.getClass());
            if (converter == null)
            {
                Class<?> genericType = field.getAnnotation(Option.class).genericType();
                //Converts Collection / Map / Array  of genericType OR returns object
                return Convert.toObject(object, genericType);
            }
            return converter.toObject(object);
        }
        catch (IllegalAccessException ex)
        {
            logger.log(Level.SEVERE, "Error while converting SubConfiguration", ex);
        }
        catch (ConversionException e)
        {
            logger.log(Level.WARNING, "Error while Converting", e);
        }
        return object;
    }

    /**
     * Loads the Configuration from a InputStream
     *
     * @param is the InputStream
     * @throws IOException
     */
    public void load(Configuration config, InputStream is) throws IOException
    {
        Map<String, Object> values = this.loadIntoMap(is);//load config into Codec
        values = this.updateConfig(values, config.getClass()); //update loaded config in Codec if needed
        this.loadIntoFields(config, values);//update Fields with loaded values
        this.clear(); //clear Codec
    }

    /**
     * Reads the InputStream to load the config into this Codec
     *
     * @param is the InputStream
     * @throws IOException
     */
    private Map<String, Object> loadIntoMap(InputStream is) throws IOException
    {
        if (is == null)
        {
            return new HashMap<String, Object>();
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader input = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        try
        {
            String line;
            line = input.readLine();
            if (line != null)
            {
                if (!line.startsWith("#Revision:"))//Detect Revision
                {
                    builder.append(line).append(LINEBREAK);
                }
                else
                {
                    int rev = Integer.parseInt(line.substring(line.indexOf(' ')));
                    this.revision = rev;
                }
                while ((line = input.readLine()) != null)
                {
                    builder.append(line);
                    builder.append(LINEBREAK);
                }
            }
        }
        catch (NumberFormatException ex)
        {
            logger.warning("Invalid RevisionNumber");
        }
        finally
        {
            input.close();
        }
        return loadFromString(builder.toString());
    }

    /**
     * Converts the inputString into ConfigurationMaps
     *
     * @param contents
     */
    public abstract Map<String, Object> loadFromString(String contents);

    private Map<String, Object> updateConfig(Map<String, Object> values, Class<? extends Configuration> clazz)
    {
        Revision revis = clazz.getAnnotation(Revision.class);
        if (revis != null && this.revision != null)
        {
            if (revis.value() > this.revision)
            {
                logger.log(Level.INFO, "Updating Configuration from Revision {0}", this.revision);
                Updater annotation = clazz.getClass().getAnnotation(Updater.class);
                if (annotation != null)
                {
                    Class<? extends ConfigurationUpdater> updaterClass = annotation.value();
                    ConfigurationUpdater updater;
                    try
                    {
                        updater = updaterClass.newInstance();
                        values = updater.update(values, this.revision);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        }
        return values;
    }

    /**
     * Writes loaded values from the Codec into the fields of the Configuration
     *
     * @param config the Configuration
     */
    private void loadIntoFields(Configuration config, Map<String, Object> values)
    {
        for (Field field : config.getClass().getFields())
        {
            try
            {
                if (field.isAnnotationPresent(Option.class))
                {
                    int mask = field.getModifiers();
                    if ((mask & Modifier.STATIC) == Modifier.STATIC)
                    {
                        continue;
                    }
                    Object configElem = this.get(field.getAnnotation(Option.class).value().toLowerCase(Locale.ENGLISH), values);//Get savedValue or default
                    if (configElem != null)
                    {
                        if (Configuration.class.isAssignableFrom(field.getType()))
                        {
                            Configuration subConfig = (Configuration)field.get(config);
                            subConfig.setCodec(config.codec);
                            subConfig.codec.loadIntoFields(subConfig, (Map)configElem);
                            field.set(config, subConfig);//Set loaded Configuration into Field
                        }
                        else
                        {
                            field.set(config, convertTo(config, field, configElem));//Set loaded Value into Field
                        }
                    }
                }
            }
            catch (IllegalAccessException ex)
            {
                throw new IllegalStateException("Error while loading a Configuration-Element!", ex);
            }
        }
    }

    /**
     * Converts the Configutaion with this Codec and saves into given File
     *
     * @param config the Configutaion
     * @param file the File
     */
    public void save(Configuration config, File file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            Map<String, Object> values = this.saveIntoMap(config, "");//Get Map & Comments
            Revision a_revision = config.getClass().getAnnotation(Revision.class);
            if (a_revision != null)
            {
                this.revision = a_revision.value();
            }
            this.save(config, file, values);
            this.clear();
        }
        catch (IOException e)
        {
            logger.warning("Error while saving a Configuration-File!");
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Error while saving a Configuration-Element!", e);
        }
    }

    /**
     * Converts the Configuration into Map String->Object
     *
     * @param config the Configuration
     * @throws IllegalAccessException
     */
    private Map<String, Object> saveIntoMap(Configuration config, String basePath) throws IllegalAccessException
    {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        Class<? extends Configuration> clazz = config.getClass();
        if (clazz.isAnnotationPresent(MapComments.class))
        {
            MapComment[] mapcomments = clazz.getAnnotation(MapComments.class).value();
            for (MapComment comment : mapcomments)
            {
                if ("".equals(basePath))
                {
                    this.addComment(comment.path(), comment.text());
                }
                else
                {
                    this.addComment(basePath + "." + comment.path(), comment.text());
                }
            }
        }
        for (Field field : clazz.getFields())
        {
            if (field.isAnnotationPresent(Option.class))
            {
                int mask = field.getModifiers();
                if (((mask & Modifier.FINAL) == Modifier.FINAL) || (((mask & Modifier.STATIC) == Modifier.STATIC)))
                {
                    continue;
                }
                String path = field.getAnnotation(Option.class).value();
                if (field.isAnnotationPresent(Comment.class))
                {
                    Comment comment = field.getAnnotation(Comment.class);
                    if ("".equals(basePath))
                    {
                        this.addComment(path, comment.value());
                    }
                    else
                    {
                        this.addComment(basePath + "." + path, comment.value());
                    }
                }
                this.set(path.toLowerCase(Locale.ENGLISH), convertFrom(field, field.get(config)), values);
            }
        }
        return values;
    }

    /**
     * Saves the configuration to a File
     *
     * @param file the File
     * @throws IOException
     */
    public void save(Configuration config, File file, Map<String, Object> values) throws IOException
    {
        Validate.notNull(file, "File for saving is null");
        String data = this.convertMapToString(config, values);
        FileWriter writer = new FileWriter(file);
        try
        {
            writer.write(data);
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * Converts the Configuration into a String for saving
     *
     * @return the config as String
     */
    public String convertMapToString(Configuration config, Map<String, Object> values)
    {
        StringBuilder sb = new StringBuilder();
        first = true;
        sb.append(this.revision());
        sb.append(StringUtils.implode("\n", config.head()));
        sb.append(this.convertMap("", values, 0));
        sb.append(StringUtils.implode("\n", config.tail()));
        return sb.toString();
    }

    /**
     * Converts a whole Section/Map into String for saving
     *
     * @param path the current path
     * @param values the values saved in the Section/Map
     * @param off the offset
     * @return the Section/Map as String
     */
    public abstract String convertMap(String path, Map<String, Object> values, int off);

    /**
     * Converts a Value into String for saving
     *
     * @param path the current path
     * @param value the value saved at given path
     * @param off the offset
     * @return the Value as String
     */
    public abstract String convertValue(String path, Object value, int off);

    /**
     * Builds a Comment for the given path
     *
     * @param path the path
     * @return the comment for path
     */
    public abstract String buildComment(String path, int off);

    /**
     * Gets the offset as String
     *
     * @param offset the offset
     * @return the offset as String
     */
    protected String offset(int offset)
    {
        StringBuilder off = new StringBuilder("");
        for (int i = 0; i < offset; ++i)
        {
            off.append(SPACES);
        }
        return off.toString();
    }

    /**
     * Gets the value saved under this path in given section
     *
     * @param path the path
     * @param section the section
     * @return the value saved under path in section
     */
    private Object get(String path, Map<String, Object> section)
    {
        if (section == null)
        {
            return null;
        }
        if (path.contains("."))
        {
            return this.get(this.getSubPath(path), (Map<String, Object>)section.get(this.findKey(this.getBasePath(path))));
        }
        return section.get(this.findKey(path));
    }

    /**
     * Gets the loaded Key for given lowercase Key
     *
     * @param key the key
     * @return the loadedKey
     */
    private String findKey(String key)
    {
        return this.loadedKeys.get(key);
    }

    /**
     * Fills LowerCaseKey -> LoadedKey Map
     */
    protected abstract void loadedKeys(Map<String, Object> values);

    /**
     * Sets a value at a specified path
     *
     * @param path the path
     * @param value the value to set
     */
    public Map<String, Object> set(String path, Object value, Map<String, Object> values)
    {
        if (path.contains("."))
        {
            Map<String, Object> subsection = this.createSection(values, this.getBasePath(path));
            this.set(subsection, this.getSubPath(path), value);
        }
        else
        {
            values.put(path, value);
        }
        return values;
    }

    /**
     * Sets the value at the path in the specified section
     *
     * @param section the section
     * @param path the path
     * @param value the value to set
     */
    private void set(Map<String, Object> section, String path, Object value)
    {
        if (path.contains("."))
        {
            Map<String, Object> subsection = this.createSection(section, this.getBasePath(path));
            this.set(subsection, this.getSubPath(path), value);
        }
        else
        {
            section.put(path, value);
        }
    }

    /**
     * Gets or create the section with the path in the basesection
     *
     * @param basesection the basesection
     * @param path the path of the section
     * @return the section
     */
    private Map<String, Object> createSection(Map<String, Object> basesection, String path)
    {
        Map<String, Object> subsection = (Map<String, Object>)basesection.get(path);
        if (subsection == null)
        {
            subsection = new LinkedHashMap<String, Object>();
            basesection.put(path, subsection);
        }
        return subsection;
    }

    /**
     * Splits up the path and returns the basepath
     *
     * @param path the path
     * @return the basepath
     */
    public String getBasePath(String path)
    {
        return path.substring(0, path.indexOf('.'));
    }

    /**
     * Splits up the path and returns the subpath
     *
     * @param path the path
     * @return the subpath
     */
    public String getSubPath(String path)
    {
        return path.substring(path.indexOf('.') + 1);
    }

    /**
     * Splits up the path and returns the key
     *
     * @param path the path
     * @return the key
     */
    public String getSubKey(String path)
    {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    /**
     * Adds a Comment to the specified path
     *
     * @param path the path
     * @param comment the comment
     */
    public void addComment(String path, String comment)
    {
        this.comments.put(path, comment);
    }

    /**
     * Resets the value & comments Map
     */
    public void clear()
    {
        this.comments.clear();
    }

    /**
     * Gets the revision as String to put infront of the File
     *
     * @return
     */
    public String revision()
    {
        if (revision != null)
        {
            return new StringBuilder("#Revision: ").append(this.revision).append(LINEBREAK).toString();
        }
        return "";
    }
}