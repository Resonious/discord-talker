package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Message
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

const val SEGMENT_SIZE = 3840

class Speech(
        val mary: LocalMaryInterface,
        val message: Message,
        val voice:   Data.Voice,
        val onDone: (Speech) -> Unit
) : AudioSendHandler {
    val text: String = message.contentStripped
    var originalInputStream = generateAudio()
    var inputStream = originalInputStream
    var done = false
    val guild = message.guild
    private var ranCallback = false
    private var nextSegment = ByteArray(SEGMENT_SIZE)
    private val myFormat    get() = inputStream.format
    private val inputFormat get() = AudioSendHandler.INPUT_FORMAT


    init {
        println("Received message: \"$text\"")

        // Convert input
        try {
            if (myFormat != inputFormat) {
                inputStream = AudioSystem.getAudioInputStream(inputFormat, inputStream)
            }
        }
        catch (e: Exception) {
            done = true
            throw e
        }
    }


    fun generateAudio(): AudioInputStream {
        mary.voice        = voice.maryVoice
        mary.audioEffects = voice.maryEffects

        var inputText = text
        if (!inputText.endsWith('.')) inputText += '.'
        // TODO do more processing, maybe generate ssml doc

        return mary.generateAudio(inputText)
    }


    fun advanceSegment(): Boolean {
        if (done) return false

        nextSegment.fill(0)
        val bytesRead = inputStream.read(nextSegment)

        if (bytesRead < nextSegment.size) {
            done = true
            return bytesRead > 0
        }
        else
            return true
    }


    override fun canProvide(): Boolean {
        if (!done) return advanceSegment()
        else if (!ranCallback) { ranCallback = true; onDone(this); return false }
        else return false
    }


    override fun provide20MsAudio(): ByteArray? {
        return nextSegment
    }
}