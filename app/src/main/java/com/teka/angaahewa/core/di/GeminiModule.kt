package com.teka.angaahewa.core.di


import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    @Named("text_model")
    fun provideGeminiTextModel(): GenerativeModel {
//        return GenerativeModel(
//            modelName = GeminiConfig.MODEL_NAME,
//            apiKey = GeminiConfig.GEMINI_API_KEY,
//            safetySettings = getSafetySettings()
//        )

        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        return model


    }

    @Provides
    @Singleton
    @Named("vision_model")
    fun provideGeminiVisionModel(): GenerativeModel {
//        return GenerativeModel(
//            modelName = GeminiConfig.VISION_MODEL_NAME,
//            apiKey = GeminiConfig.GEMINI_API_KEY,
//            safetySettings = getSafetySettings()
//        )

        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        return model
    }

    private fun getSafetySettings() = listOf(
        SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.NONE),
    )
}