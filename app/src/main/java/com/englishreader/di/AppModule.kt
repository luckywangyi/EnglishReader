package com.englishreader.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.englishreader.data.local.AppDatabase
import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.dao.VocabularyDao
import com.englishreader.data.remote.gemini.GeminiService
import com.englishreader.data.remote.rss.RssService
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.data.repository.TranslationRepository
import com.englishreader.data.repository.VocabularyRepository
import com.prof18.rssparser.RssParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRssParser(okHttpClient: OkHttpClient): RssParser {
        return RssParser(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideRssService(rssParser: RssParser): RssService {
        return RssService(rssParser)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "english_reader_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideArticleDao(database: AppDatabase): ArticleDao {
        return database.articleDao()
    }

    @Provides
    @Singleton
    fun provideVocabularyDao(database: AppDatabase): VocabularyDao {
        return database.vocabularyDao()
    }

    @Provides
    @Singleton
    fun provideReadingStatsDao(database: AppDatabase): ReadingStatsDao {
        return database.readingStatsDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideGeminiService(settingsRepository: SettingsRepository): GeminiService {
        return GeminiService(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideArticleRepository(
        rssService: RssService,
        articleDao: ArticleDao,
        geminiService: GeminiService
    ): ArticleRepository {
        return ArticleRepository(rssService, articleDao, geminiService)
    }

    @Provides
    @Singleton
    fun provideTranslationRepository(
        @ApplicationContext context: Context,
        geminiService: GeminiService
    ): TranslationRepository {
        return TranslationRepository(context, geminiService)
    }

    @Provides
    @Singleton
    fun provideVocabularyRepository(
        vocabularyDao: VocabularyDao
    ): VocabularyRepository {
        return VocabularyRepository(vocabularyDao)
    }
}
