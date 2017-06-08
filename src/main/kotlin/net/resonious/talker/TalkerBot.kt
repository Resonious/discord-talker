package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.managers.AudioManager

/**
 * Created by metre on 6/7/2017.
 */
class TalkerBot(val dataSource: Data) : ListenerAdapter() {
    val mary = LocalMaryInterface()
    var audioManagers = HashMap<Long, AudioManager>()


    fun joinVoiceChannel(guild: Guild, channel: VoiceChannel) {
        val manager = audioManagers.getOrElse(guild.idLong, { guild.audioManager })

        // Talker really only works with one voice channel lol
        if (manager.connectedChannel?.id != channel.id)
            manager.openAudioConnection(channel)
    }


    fun leaveVoiceChannel(guild: Guild) {
        val manager = audioManagers[guild.idLong] ?: return
        manager.closeAudioConnection()
    }


    // TODO anyone typing into #voice-replies while in voice should trip
    // the joinVoiceChannel -- this is just for stupid I guess
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent?) {
        if (event == null) return
        if (!messageMentionsMe(event.message)) return

        val content = event.message.content
        if (content.contains("join")) {
            try {
                joinVoiceChannel(event.guild, event.member.voiceState.channel)
            }
            catch (e: Exception) {
                println(e)
                // Don't really care right now if it doesn't work
            }
        }
    }


    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent?) {
        if (event == null) return
        val channel = event.channelLeft ?: return
        val manager = audioManagers[channel.guild.idLong] ?: return

        // TODO this does not work and should depend on how many people are left in there
        leaveVoiceChannel(event.guild)
    }


    private fun messageMentionsMe(message: Message) =
        message.mentionedUsers.any { u -> u.id ==  message.jda.selfUser.id }
}