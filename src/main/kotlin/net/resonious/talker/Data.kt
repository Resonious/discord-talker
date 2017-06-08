package net.resonious.talker
import com.fasterxml.jackson.module.kotlin.*
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class Data(val directory: String) {
    data class Profile(val userId: String)
    class NoTokenException(
            val path: Path,
            override val cause: Throwable
    ) : Exception("Failed to read token from $path", cause) { }

    val mapper = jacksonObjectMapper()

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