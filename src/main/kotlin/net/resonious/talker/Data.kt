package net.resonious.talker
import com.fasterxml.jackson.module.kotlin.*
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths

class Data(val directory: String) {
    data class Profile(val userId: String)

    val mapper = jacksonObjectMapper()

    fun getProfile(id: String): Profile {
        val path = Paths.get(directory, "profile-$id.json")
        val file = path.toFile()

        try {
            return mapper.readValue(file)
        }
        catch (notFound: FileNotFoundException) {
            val newProfile = Profile(id)
            mapper.writeValue(file, newProfile)
            return newProfile
        }
    }
}