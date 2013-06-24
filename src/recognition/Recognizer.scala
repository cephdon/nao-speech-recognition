package recognition

import java.io._

import java.net.HttpURLConnection
import java.net.URL
import helper.Parser._
import javaFlacEncoder.FLAC_FileEncoder
import javaFlacEncoder.EncodingConfiguration
import scala.collection.mutable.StringBuilder
import helper.Audio

/**
 * Benutzt Googles Chrome Speech API um WAV-Files in Text zu konvertieren
 */
object Recognizer {

  val ADDRESS = "http://www.google.com/speech-api/v1/recognize?lang=de-de&client=chromium"
  val USER_AGENT = "Mozilla/5.0"
  val CONTENT_TYPE = "audio/x-flac; rate="

  /**
   * Der Name ist Programm
   * @param wavFile Pfad zu einer WAV, die erkannt werden soll
   * @return Info(Erkannter Text, Status der Erkennung, Zufriedenheit der Erkennung)
   */
  def recognize(wavFile: File): Info = {
    val filename = wavFile + ".flac"

    // WAV konvertieren    
    val flacEncoder = new FLAC_FileEncoder()
    val inputFile = wavFile //new File(wavFile)
    val sampleRate = Audio.sampleRate(inputFile)
    val outputFile = new File(filename)

    println(
      flacEncoder.encode(inputFile, outputFile) +
        "\n" + sampleRate)

    // Datei an Google uebertragen

    val url = new URL(ADDRESS)

    val connection: HttpURLConnection = url.openConnection.asInstanceOf[HttpURLConnection]

    connection.setRequestMethod("POST")

    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setUseCaches(false)

    val fileInputStream = new FileInputStream(outputFile)

    connection.setRequestProperty("User-Agent", USER_AGENT)
    connection.setRequestProperty("Content-Type", CONTENT_TYPE + sampleRate)
    connection.setRequestProperty("Accept-Charset", "UTF-8")
    //connection.setRequestProperty("Content-Length", "" + fileInputStream.available())

    val maxBufferSize: Int = 10000
    val writer: DataOutputStream = new DataOutputStream(connection.getOutputStream())

    while (fileInputStream.available() > 0) {
      val min = math.min(fileInputStream.available, maxBufferSize)
      val buffer = new Array[Byte](min)
      fileInputStream.read(buffer, 0, min)
      writer.write(buffer, 0, min)
    }

    // Ergebnis von Google verarbeiten
    try {
      val reader: BufferedReader = new BufferedReader(new InputStreamReader(
        connection.getInputStream, "UTF-8"))

      val buf = new StringBuilder

      for (line <- reader.readLine) {
        buf.append(line)
      }

      val text = extractUtterance(buf.toString)
      val status = extractStatus(buf.toString)
      val confidence = extractConfidence(buf.toString)

      writer.flush
      writer.close
      fileInputStream.close
      reader.close

      System.gc

      outputFile.delete
      Info(text, status, confidence)
    } catch {
      case _: Throwable => Info("", -1, 0)
    }
  }

  /**
   * Beispielaufruf
   */
  def main(args: Array[String]) = {
    println(recognize(new File("recordings/tmp.wav")))
  }
}

/**
 * Fasst das Erkennungsergebnis zusammen
 */
case class Info(val text: String, status: Int, confidence: Double)