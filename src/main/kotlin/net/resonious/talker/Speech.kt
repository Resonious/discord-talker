package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Message
import javax.sound.sampled.AudioInputStream

const val SEGMENT_SIZE = 3840

class Speech(
        val mary: LocalMaryInterface,
        val message: Message,
        val voice:   Data.Voice,
        val onDone: (Speech) -> Unit
) : AudioSendHandler {
    val text = message.strippedContent
    var inputStream = generateAudio()
    var done = false
    var ranCallback = false
    var nextSegment = ByteArray(SEGMENT_SIZE)
    val myFormat    get() = inputStream.format
    val inputFormat get() = AudioSendHandler.INPUT_FORMAT
    val inputSamplesPerMySample = (inputFormat.sampleRate / myFormat.sampleRate).toInt()

    init {
        println("Received message: \"$text\"")

        // Do some quick validations here
        try {
            if (myFormat.sampleRate > inputFormat.sampleRate)
                throw RuntimeException(
                    "Sample rates aren't right (generated ${myFormat.sampleRate}," +
                    "expected < ${inputFormat.sampleRate})"
                )

            if (myFormat.frameSize * 2 != inputFormat.frameSize)
                throw RuntimeException(
                    "Frame size isn't right (generated ${myFormat.frameSize}, " +
                    "expected ${inputFormat.frameSize / 2} * 2)"
                )

            if (myFormat.isBigEndian == inputFormat.isBigEndian)
                throw RuntimeException(
                    "Expected endianness to be swapped. This is an easy fix."
                )
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


    override fun canProvide(): Boolean {
        if (!done) return advanceSegment()
        else if (!ranCallback) { ranCallback = true; onDone(this); return false }
        else return false
    }


    fun advanceSegment(): Boolean {
        if (done) return false

        val sample = ByteArray(2)
        nextSegment.fill(0)

        // The 4 here I think is (2 bytes/sample) * (2 mono/stereo)
        for (i in nextSegment.indices step 4 * inputSamplesPerMySample) {
            val amountRead = inputStream.read(sample)
            if (amountRead < 2) {
                done = true
                return i > 0
            }

            for (j in 0..(inputSamplesPerMySample-1)) {
                nextSegment[i+j]   = sample[1]
                nextSegment[i+j+1] = sample[0]
                nextSegment[i+j+2] = sample[1]
                nextSegment[i+j+3] = sample[0]
            }
        }

        return true
    }


    override fun provide20MsAudio(): ByteArray? {
        return nextSegment
    }
}