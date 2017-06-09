package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.client.managers.EmoteManager
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class TalkerBot(val dataSource: Data) : ListenerAdapter() {
    val speeches = mutableListOf<Speech>()
    val mary = LocalMaryInterface()


    fun joinVoiceChannel(guild: Guild, channel: VoiceChannel) {
        val manager = guild.audioManager

        // Talker really only works with one voice channel I guess
        if (manager.connectedChannel?.id != channel.id)
            manager.openAudioConnection(channel)
    }


    fun leaveVoiceChannel(guild: Guild) {
        val manager = guild.audioManager
        speeches.clear()
        manager.closeAudioConnection()
    }


    fun endCurrentSpeech(guild: Guild) {
        val manager = guild.audioManager.sendingHandler
        if (manager is Speech)
            manager.done = true
    }


    fun playNextSpeech() {
        val speech       = speeches.firstOrNull() ?: return
        val guild        = speech.message.guild
        val voiceChannel = speech.message.member.voiceState?.channel ?: return

        joinVoiceChannel(guild, voiceChannel)
        guild.audioManager.sendingHandler = speech
    }


    fun speechDone(speech: Speech) {
        println("The speech \"${speech.text}\" has ended")

        speeches.remove(speech)
        playNextSpeech()
    }


    fun listVoices(message: Message) {
        val guild = message.guild
        message.textChannel.sendMessage("Click a reaction to select a voice").queue { sent ->
            var foundAny = false

            dataSource.allVoices().forEach { voice ->
                foundAny = true

                if (voice.emoji.toUpperCase() != voice.emoji.toLowerCase()) {
                    val candidates = guild.getEmotesByName(voice.emoji, true)
                    if (candidates.isEmpty())
                        println("Failed to find emote for \"${voice.emoji}\" for voice \"${voice.name}\"")
                    else
                        sent.addReaction(candidates.first()).queue()
                }
                else
                    sent.addReaction(voice.emoji).queue()
            }

            if (!foundAny) {
                message.textChannel.sendMessage("Oops nevermind there aren't any options. Sorry!").queue()
            }
        }
    }


    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent?) {
        if (event == null) return
        val user     = event.member.user
        val reaction = event.reaction
        if (user.isBot) return

        println("Reaction \"${reaction.emote.name}\" added to some message")

        // When a reaction is added to one of our messages, we set the user's voice according to the emote
        event.channel.getMessageById(event.messageIdLong).queue { message ->
            val theMessageWasMine = message.author.idLong == event.jda.selfUser.idLong
            if (theMessageWasMine) {
                val profile = dataSource.getProfile(user.id)
                val voice = dataSource.getVoiceByEmoji(reaction.emote.name)

                if (voice == null)
                    message.channel.sendMessage(
                            "${user.asMention} Oops, failed to find the voice for ${reaction.emote.name}, sorry"
                    ).queue()

                else {
                    profile.voiceName = voice.name
                    dataSource.saveProfile(profile)
                    message.channel.sendMessage(
                            "${user.asMention} Voice set to ${reaction.emote.name} (${voice.name})!"
                    ).queue()
                }

                reaction.removeReaction(user).queue()
            }
        }
    }


    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent?) {
        if (event == null) return

        if (event.message.strippedContent.contains("!voices")) return listVoices(event.message)

        if (event.member.voiceState?.channel == null) return
        if (event.author.isBot) return
        if (!event.channel.name.contains("voice-replies")) return // TODO maybe make voice-replies name configurable

        val user    = event.member.user
        val profile = dataSource.getProfile(user.id)
        val voice   = dataSource.getVoice(profile.voiceName) ?: dataSource.getVoice("default")
        ?: throw RuntimeException("No default voice")

        speeches.add(Speech(mary, event.message, voice, this::speechDone))

        if (speeches.size == 1)
            playNextSpeech()
    }


    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent?) {
        if (event == null) return
        if (event.member.user.idLong == event.jda.selfUser.idLong) return
        val channel = event.channelLeft ?: return
        val manager = event.guild.audioManager ?: return

        val isMyChannel = channel.idLong == manager.connectedChannel?.idLong

        if (isMyChannel && channel.members.size == 1) {
            // If we're the only one left, leave!
            leaveVoiceChannel(event.guild)
        }
        else {
            val current = manager.sendingHandler
            // If the person who left was talking, we want to remove all their speeches
            // from the queue.
            speeches.removeAll {
                if (it == current) it.done = true
                it.message.author.idLong == event.member.user.idLong
            }

            // In case we interrupted the current speech, start playing the next one.
            if (current is Speech && current.done && speeches.size > 0)
                playNextSpeech()
        }
    }
}