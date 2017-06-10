package net.resonious.talker

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.TextChannel

const val HELP_MESSAGE: String = """
===========================================
Type "#<channel-name> <message>" to send <message> to <channel-name> on all added guilds.
Type "${'$'}<channel-id> <message>" to send <message> to the channel with <channel-id>
===========================================
"""

fun chat(channel: TextChannel, message: String) {
    channel.sendMessage(message).queue {
        println("Sent \"${it.content}\" to ${channel.guild.name} #${channel.name}")
    }
}

fun announce(jda: JDA, message: String): Boolean {
    if (message.startsWith("#")) {
        val channelTag = message.takeWhile { !it.isWhitespace() }
        val content = message.removePrefix(channelTag).trimStart()
        val channelName = channelTag.removePrefix("#")

        for (guild in jda.guilds) {
            val channels = guild.getTextChannelsByName(channelName, true)
            for (channel in channels)
                chat(channel, content)
        }
        return true
    }
    else if (message.startsWith("$")) {
        val channelIdent = message.takeWhile { !it.isWhitespace() }
        val content      = message.removePrefix(channelIdent).trimStart()
        val channelId    = channelIdent.removePrefix("$")

        chat(jda.getTextChannelById(channelId), content)
        return true
    }
    return false
}

fun main(args: Array<String>) {
    val data = Data("./talker-data/")
    val bot = TalkerBot(data)
    val jda = JDABuilder(AccountType.BOT)
            .setToken(data.getToken())
            .addEventListener(bot)
            .buildAsync()

    var input = readLine()
    while (input != null) {
        if (!announce(jda, input))
            println(HELP_MESSAGE)
        input = readLine()
    }
}
