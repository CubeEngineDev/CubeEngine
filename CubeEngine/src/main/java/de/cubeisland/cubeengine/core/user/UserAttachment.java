package de.cubeisland.cubeengine.core.user;

import de.cubeisland.cubeengine.core.attachment.Attachment;

public abstract class UserAttachment extends Attachment<User>
{
    public void onJoin(String joinMessage)
    {}

    public void onQuit(String quitMessage)
    {}

    public void onKick(String kickMessage)
    {}

    public void onChat(String format, String message)
    {}

    public void onCommand(String commandline)
    {}
}
