package com.example.acusen.alert

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

/**
 * Služba pro odesílání e-mailových upozornění
 */
class EmailAlertService(private val context: Context) {

    data class AlertConfig(
        val recipientEmail: String,
        val senderEmail: String,
        val senderPassword: String,
        val smtpHost: String = "smtp.gmail.com",
        val smtpPort: Int = 587,
        val subjectPrefix: String = "[ACOUSTIC SENTINEL ALERT]"
    )

    /**
     * Odešle alert e-mail s audio přílohou a GPS lokací
     */
    suspend fun sendAlert(
        config: AlertConfig,
        patternName: String,
        confidence: Double,
        audioData: FloatArray,
        location: Location?
    ): Boolean = withContext(Dispatchers.IO) {

        try {
            val session = createEmailSession(config)
            val message = createEmailMessage(session, config, patternName, confidence, location)

            // Vytvoření audio přílohy
            val audioFile = createAudioFile(audioData)
            if (audioFile != null) {
                addAudioAttachment(message, audioFile)
            }

            // Odeslání e-mailu
            Transport.send(message)

            // Vymazání dočasného souboru
            audioFile?.delete()

            true

        } catch (e: Exception) {
            // TODO: Logovat chybu
            false
        }
    }

    private fun createEmailSession(config: AlertConfig): Session {
        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.smtpHost)
            put("mail.smtp.port", config.smtpPort.toString())
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        return Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.senderEmail, config.senderPassword)
            }
        })
    }

    private fun createEmailMessage(
        session: Session,
        config: AlertConfig,
        patternName: String,
        confidence: Double,
        location: Location?
    ): MimeMessage {

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.senderEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.recipientEmail))
            subject = "${config.subjectPrefix} Detekce: $patternName"
        }

        // Vytvoření HTML obsahu
        val htmlContent = buildEmailContent(patternName, confidence, timestamp, location)

        val multipart = MimeMultipart()

        // Text část
        val textPart = MimeBodyPart().apply {
            setContent(htmlContent, "text/html; charset=utf-8")
        }
        multipart.addBodyPart(textPart)

        message.setContent(multipart)
        return message
    }

    private fun buildEmailContent(
        patternName: String,
        confidence: Double,
        timestamp: String,
        location: Location?
    ): String {
        val locationText = location?.let {
            val googleMapsLink = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
            """
            <p><strong>GPS Lokace:</strong><br>
            Zeměpisná šířka: ${it.latitude}<br>
            Zeměpisná délka: ${it.longitude}<br>
            <a href="$googleMapsLink">Zobrazit na Google Maps</a></p>
            """
        } ?: "<p><strong>GPS Lokace:</strong> Nedostupná</p>"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: Arial, sans-serif; }
                    .alert { background-color: #ffebee; padding: 15px; border-left: 4px solid #f44336; }
                    .info { margin: 10px 0; }
                    .confidence { color: #4caf50; font-weight: bold; }
                    .timestamp { color: #666; }
                </style>
            </head>
            <body>
                <div class="alert">
                    <h2>🚨 ACOUSTIC SENTINEL ALERT</h2>
                    
                    <div class="info">
                        <p><strong>Detekovaný vzor:</strong> $patternName</p>
                        <p><strong>Přesnost detekce:</strong> <span class="confidence">${(confidence * 100).toInt()}%</span></p>
                        <p><strong>Čas detekce:</strong> <span class="timestamp">$timestamp</span></p>
                    </div>
                    
                    $locationText
                    
                    <p><strong>Audio záznam:</strong> V příloze najdete 15sekundový audio záznam zachycující moment detekce.</p>
                    
                    <hr>
                    <p style="font-size: 12px; color: #888;">
                        Tento e-mail byl automaticky vygenerován aplikací Acoustic Sentinel.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun addAudioAttachment(message: MimeMessage, audioFile: File) {
        val multipart = message.content as MimeMultipart

        val attachmentPart = MimeBodyPart().apply {
            attachFile(audioFile)
            fileName = "acoustic_detection_${System.currentTimeMillis()}.wav"
            disposition = Part.ATTACHMENT
        }

        multipart.addBodyPart(attachmentPart)
    }

    private fun createAudioFile(audioData: FloatArray): File? {
        try {
            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")

            // Zjednodušená WAV konverze (bez hlavičky)
            // V produkci použijte správnou WAV knihovnu
            FileOutputStream(audioFile).use { fos ->
                // WAV header (44 bytes)
                writeWavHeader(fos, audioData.size)

                // Audio data (16-bit PCM)
                for (sample in audioData) {
                    val intSample = (sample * 32767).toInt().coerceIn(-32768, 32767)
                    fos.write(intSample and 0xFF)
                    fos.write((intSample shr 8) and 0xFF)
                }
            }

            return audioFile

        } catch (e: Exception) {
            return null
        }
    }

    private fun writeWavHeader(fos: FileOutputStream, audioDataSize: Int) {
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = audioDataSize * 2 // 16-bit samples

        // RIFF header
        fos.write("RIFF".toByteArray())
        writeInt(fos, 36 + dataSize)
        fos.write("WAVE".toByteArray())

        // fmt chunk
        fos.write("fmt ".toByteArray())
        writeInt(fos, 16) // chunk size
        writeShort(fos, 1) // PCM format
        writeShort(fos, channels)
        writeInt(fos, sampleRate)
        writeInt(fos, byteRate)
        writeShort(fos, blockAlign)
        writeShort(fos, bitsPerSample)

        // data chunk
        fos.write("data".toByteArray())
        writeInt(fos, dataSize)
    }

    private fun writeInt(fos: FileOutputStream, value: Int) {
        fos.write(value and 0xFF)
        fos.write((value shr 8) and 0xFF)
        fos.write((value shr 16) and 0xFF)
        fos.write((value shr 24) and 0xFF)
    }

    private fun writeShort(fos: FileOutputStream, value: Int) {
        fos.write(value and 0xFF)
        fos.write((value shr 8) and 0xFF)
    }
}
