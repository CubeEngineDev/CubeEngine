package de.cubeisland.cubeengine.guests;

import de.cubeisland.cubeengine.core.config.Configuration;
import de.cubeisland.cubeengine.core.config.annotations.Codec;
import de.cubeisland.cubeengine.core.config.annotations.Revision;

@Codec("yml")
@Revision(1)
public class GuestsConfig extends Configuration
{
    public boolean punishments = false;
}
