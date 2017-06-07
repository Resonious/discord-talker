package net.resonious.talker

import marytts.LocalMaryInterface
import marytts.config.MaryConfig
import marytts.util.data.audio.MaryAudioUtils

fun main(args: Array<String>) {
    println("Ahoy ${args.size}")

    // TODO shit!!! how can we make this load from the jar?
    println("Uhhh " + MaryConfig.countConfigs())
    val mary = LocalMaryInterface()
    val audio = mary.generateAudio("one two three what the fuck?")
    val samples = MaryAudioUtils.getSamplesAsDoubleArray(audio)
    MaryAudioUtils.writeWavFile(samples, "out.wav", audio.format)

    println("Wrote to out.wav")
}
