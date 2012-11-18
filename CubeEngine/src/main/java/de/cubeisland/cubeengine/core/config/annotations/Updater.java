package de.cubeisland.cubeengine.core.config.annotations;

import de.cubeisland.cubeengine.core.config.ConfigurationUpdater;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to declare a configuration-updater to use when the
 * configs revision is too old.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Updater
{
    Class<? extends ConfigurationUpdater> value();
}
