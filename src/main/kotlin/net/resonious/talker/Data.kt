package net.resonious.talker
import com.fasterxml.jackson.module.kotlin.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class Data(private val directory: String) {
    data class Voice(val name: String, val maryVoice: String, val maryEffects: String, val emoji: String)
    data class Profile(val userId: String, var voiceName: String)


    class NoTokenException(
            val path: Path,
            override val cause: Throwable
    ) : Exception("Failed to read token from $path", cause)


    private val mapper = jacksonObjectMapper()
    private val voiceCache   = hashMapOf<String, Voice>()
    private val profileCache = hashMapOf<String, Profile>()


    fun getToken(): String {
        val path = Paths.get(directory, "token.txt")
        val file = path.toFile()

        try {
            val lines = file.readLines()
            return lines.first()
        }
        catch (notFound: FileNotFoundException) {
            throw NoTokenException(path, notFound)
        }
    }


    fun getProfile(id: String): Profile = profileCache.getOrPut(id, {
        val path = Paths.get(directory, "profile-$id.json")
        val file = path.toFile()

        try {
            return mapper.readValue(file)
        }
        catch (notFound: FileNotFoundException) {
            val newProfile = Profile(id, "default")
            mapper.writeValue(file, newProfile)
            return newProfile
        }
    })


    fun getVoice(name: String): Voice? = voiceCache.getOrPut(name, {
        val path = Paths.get(directory, "voice-$name.json")
        val file = path.toFile()

        return try {
            mapper.readValue(file)
        }
        catch (notFound: FileNotFoundException) {
            null
        }
    })


    fun allVoices(): List<Voice> {
        return File(directory)
                .listFiles({ file -> file.name.contains("voice-") })
                .map { mapper.readValue<Voice>(it) }
    }


    fun getVoiceByEmoji(emoji: String): Voice? {
        for (voice in allVoices())
            if (voice.emoji == emoji)
                return voice
        return null
    }


    fun saveProfile(profile: Profile) {
        val path = Paths.get(directory, "profile-${profile.userId}.json")
        val file = path.toFile()
        mapper.writeValue(file, profile)
        profileCache.put(profile.userId, profile)
    }
}