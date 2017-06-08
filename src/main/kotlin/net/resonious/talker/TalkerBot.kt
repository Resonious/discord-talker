package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Created by metre on 6/7/2017.
 */
class TalkerBot(val dataSource: Data) : ListenerAdapter() {
    val mary = LocalMaryInterface()

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event == null) return

        val content = event.message.content
        println("Got this: '$content'")
    }
}