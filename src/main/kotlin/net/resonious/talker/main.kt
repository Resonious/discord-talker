package net.resonious.talker

import marytts.LocalMaryInterface
import marytts.config.MaryConfig
import marytts.util.data.audio.MaryAudioUtils

fun main(args: Array<String>) {
    println("Uhhh " + MaryConfig.countConfigs() + " configs exist\n")
    val mary = LocalMaryInterface()

    var voices = mary.availableVoices
    voices.forEach { v -> println("Voice: $v") }
    mary.voice = "cmu-rms-hsmm"

    val audio = mary.generateAudio("Here's the voice. I hope this works any amount at all. How do I sound?")

    println(
        "\nSome info:\n"+
        "Channels: ${audio.format.channels}\n"+
        "Encoding: ${audio.format.encoding.toString()}\n"+
        "Big Endian: ${audio.format.isBigEndian}\n"+
        "Sample rate: ${audio.format.sampleRate}"+
        "Sample bits: ${audio.format.sampleSizeInBits}"
    )

    val samples = MaryAudioUtils.getSamplesAsDoubleArray(audio)
    MaryAudioUtils.writeWavFile(samples, "out.wav", audio.format)


    println("\nWrote to out.wav")
}
