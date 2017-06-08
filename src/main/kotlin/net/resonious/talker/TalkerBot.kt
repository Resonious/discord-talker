package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent
import net.dv8tion.jda.core.audio.AudioReceiveHandler
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
    var audioManagers = HashMap<Long, AudioManager>()

    fun joinVoiceChannel(guild: Guild, channel: VoiceChannel) {
        val manager = audioManagers.getOrElse(guild.idLong, { guild.audioManager })

        // Talker really only works with one voice channel lol
        if (manager.connectedChannel?.id != channel.id) {
            // TODO set sending handler RIGHT HERE
            manager.openAudioConnection(channel)
        }
    }


    fun leaveVoiceChannel(guild: Guild) {
        val manager = audioManagers[guild.idLong] ?: return
        manager.closeAudioConnection()
        audioManagers.remove(guild.idLong)
    }


    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent?) {
        if (event == null) return
        if (event.author.isBot) return
        if (!event.channel.name.contains("voice-replies")) return // TODO maybe make voice-replies name configurable
        if (event.member.voiceState?.channel == null) return

        joinVoiceChannel(event.guild, event.member.voiceState.channel)
    }


    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent?) {
        if (event == null) return
        val channel = event.channelLeft ?: return
        val manager = audioManagers[channel.guild.idLong] ?: return

        val isMyChannel = channel.idLong == manager.connectedChannel.idLong

        if (isMyChannel && channel.members.size == 1)
            leaveVoiceChannel(event.guild)
    }


    private fun messageMentionsMe(message: Message) =
        message.mentionedUsers.any { u -> u.id == message.jda.selfUser.id }
}