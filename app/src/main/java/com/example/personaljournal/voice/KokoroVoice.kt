
package com.example.personaljournal.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class KokoroVoice(private val appContext: Context) {

    private var tts: TextToSpeech? = null
    private var initialized = false

    // INITIALIZE ANDROID TEXT TO SPEECH

    suspend fun initialize() = withContext(Dispatchers.Main) {

        if (initialized && tts != null) return@withContext

        suspendCancellableCoroutine<Unit> { continuation ->

            val completed = AtomicBoolean(false)

            lateinit var engine: TextToSpeech

            engine = TextToSpeech(appContext) { status ->

                if (!completed.compareAndSet(false, true)) return@TextToSpeech

                if (status == TextToSpeech.SUCCESS) {

                    val languageResult = engine.setLanguage(Locale.getDefault())

                    if (
                        languageResult != TextToSpeech.LANG_MISSING_DATA &&
                        languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                    ) {

                        engine.setSpeechRate(1.0f)

                        if (continuation.isActive) {
                            tts = engine
                            initialized = true
                            continuation.resume(Unit)
                        } else {
                            engine.shutdown()
                        }

                    } else {

                        engine.shutdown()

                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "Language unavailable. Install a voice in Android TTS settings."
                                )
                            )
                        }
                    }

                } else {

                    engine.shutdown()

                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException(
                                "Android TTS initialization failed."
                            )
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                completed.set(true)
                engine.shutdown()

                if (tts === engine) {
                    tts = null
                    initialized = false
                }
            }

            tts = engine
        }
    }

    // SPEAK TEXT

    suspend fun speak(text: String) = withContext(Dispatchers.Main) {

        val engine = tts
            ?: throw IllegalStateException(
                "Android TTS is not initialized"
            )

        check(initialized) {
            "Android TTS is not ready"
        }

        val cleanText = text.trim()
            .replace(Regex("\\s+"), " ")

        if (cleanText.isEmpty()) return@withContext

        suspendCancellableCoroutine<Unit> { continuation ->

            val completed = AtomicBoolean(false)
            val utteranceId = "journal_${System.nanoTime()}"

            fun finish(error: Throwable? = null) {

                if (completed.compareAndSet(false, true)) {

                    if (continuation.isActive) {

                        if (error == null) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(error)
                        }
                    }
                }
            }

            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(id: String?) = Unit

                    override fun onDone(id: String?) {
                        if (id == utteranceId) {
                            finish()
                        }
                    }

                    override fun onError(id: String?) {
                        if (id == utteranceId) {
                            finish(
                                IllegalStateException("Speech playback failed")
                            )
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId) {
                            finish(
                                IllegalStateException("Speech error: $errorCode")
                            )
                        }
                    }
                }
            )

            continuation.invokeOnCancellation {
                engine.stop()
            }

            val result = engine.speak(
                cleanText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )

            if (result == TextToSpeech.ERROR) {
                finish(
                    IllegalStateException("Unable to start speech")
                )
            }
        }
    }

    // RELEASE TEXT TO SPEECH

    fun release() {

        tts?.stop()
        tts?.shutdown()

        tts = null
        initialized = false
    }
}
