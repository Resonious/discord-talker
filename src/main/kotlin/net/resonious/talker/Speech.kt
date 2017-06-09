package net.resonious.talker

import marytts.LocalMaryInterface
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Message

class Speech(
        mary: LocalMaryInterface,
        val message: Message,
        val onDone: (Speech) -> Unit
) : AudioSendHandler {
    val text = message.strippedContent
    var inputStream = mary.generateAudio(text)
    var done = false
    var ranCallback = false

    init {
        println("Received message: \"$text\"")

        // Do some quick validations here
        val myFormat = inputStream.format
        val expectedFormat = AudioSendHandler.INPUT_FORMAT

        try {
            if (myFormat.sampleRate != expectedFormat.sampleRate)
                throw RuntimeException(
                    "Sample rates don't match (generated ${myFormat.sampleRate}," +
                    "expected ${expectedFormat.sampleRate})"
                )

            if (myFormat.frameSize * 2 != expectedFormat.frameSize)
                throw RuntimeException(
                    "Frame sizes isn't right (generated ${myFormat.frameSize}, " +
                    "expected ${expectedFormat.frameSize / 2} * 2)"
                )

            if (myFormat.isBigEndian == expectedFormat.isBigEndian)
                throw RuntimeException(
                    "Expected endianness to be swapped. This is an easy fix."
                )
        }
        catch (e: Exception) {
            done = true
            throw e
        }
    }

    override fun provide20MsAudio(): ByteArray {
        val result = ByteArray(3840)
        val sample = ByteArray(2)

        for (i in result.indices step 4) {
            val amountRead = inputStream.read(sample)
            if (amountRead < 2) {
                done = true
                break
            }

            result[i]   = sample[1]
            result[i+1] = sample[0]

            result[i+2] = sample[1]
            result[i+3] = sample[0]
        }

        return result
    }

    override fun canProvide(): Boolean {
        if (!done) return true
        else if (!ranCallback) { ranCallback = true; onDone(this); return false }
        else return false
    }
}