package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.managers.AudioManager

class Speech : AudioSendHandler {
    val mary = LocalMaryInterface()

    override fun provide20MsAudio(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canProvide(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}