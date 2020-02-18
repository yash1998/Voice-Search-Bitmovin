package com.example.firetv

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log


class TvSpeechRecognitionListener : RecognitionListener {
    override fun onReadyForSpeech(p0: Bundle?) {
        Log.v("yash", "onReadyForSpeech")
    }

    override fun onRmsChanged(p0: Float) {
//        Log.v("yash", "onRmsChanged")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.v("yash", "onBufferReceived")
    }

    override fun onPartialResults(p0: Bundle?) {
//        Log.v("yash", "onPartialResults")
        val voiceResults = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        voiceResults?.let {
            for (match in voiceResults) {
                Log.v("yash", match)
            }
        }
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.v("yash", "onEvent")
    }

    override fun onBeginningOfSpeech() {
        Log.v("yash", "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.v("yash", "onEndOfSpeech")
    }

    override fun onError(p0: Int) {
        Log.v("yash", "onError $p0")
    }

    override fun onResults(p0: Bundle?) {
        val voiceResults = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (voiceResults == null) {
            Log.v("yash", "No voice results")
        } else {
            Log.v("yash", "Printing matches: ")
            for (match in voiceResults) {
                Log.v("yash", match)
            }
        }
    }
}