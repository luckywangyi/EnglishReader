package com.englishreader.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.englishreader.data.local.AppDatabase
import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.dao.SentenceDao
import com.englishreader.data.local.dao.VocabularyDao
import com.englishreader.data.remote.ai.AiService
import com.englishreader.data.remote.rss.RssService
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.SentenceRepository
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.data.repository.TranslationRepository
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.service.RecommendationService
import com.englishreader.notification.NotificationHelper
import com.englishreader.util.TtsManager
import com.englishreader.worker.ReminderScheduler
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
            .connectTimeout(10, TimeUnit.SECONDS)  // 缩短连接超时
            .readTimeout(15, TimeUnit.SECONDS)     // 缩短读取超时
            .writeTimeout(15, TimeUnit.SECONDS)    // 缩短写入超时
            .build()
    }

    @Provides
    @Singleton
    fun provideRssParser(): RssParser {
        return RssParser()
    }

    @Provides
    @Singleton
    fun provideRssService(rssParser: RssParser, htmlParser: com.englishreader.data.remote.rss.HtmlParser): RssService {
        return RssService(rssParser, htmlParser)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "english_reader_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()  // 备用：如果迁移失败则重建数据库
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)  // 更稳定的日志模式
            .build()
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
    fun provideAiService(settingsRepository: SettingsRepository): AiService {
        return AiService(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideArticleRepository(
        rssService: RssService,
        articleDao: ArticleDao,
        aiService: AiService,
        htmlParser: com.englishreader.data.remote.rss.HtmlParser,
        readingStatsDao: ReadingStatsDao,
        customRssSourceDao: com.englishreader.data.local.dao.CustomRssSourceDao
    ): ArticleRepository {
        return ArticleRepository(rssService, articleDao, aiService, htmlParser, readingStatsDao, customRssSourceDao)
    }

    @Provides
    @Singleton
    fun provideTranslationRepository(
        @ApplicationContext context: Context,
        aiService: AiService
    ): TranslationRepository {
        return TranslationRepository(context, aiService)
    }

    @Provides
    @Singleton
    fun provideVocabularyRepository(
        vocabularyDao: VocabularyDao
    ): VocabularyRepository {
        return VocabularyRepository(vocabularyDao)
    }

    @Provides
    @Singleton
    fun provideSentenceDao(database: AppDatabase): SentenceDao {
        return database.sentenceDao()
    }
    
    @Provides
    @Singleton
    fun provideCustomRssSourceDao(database: AppDatabase): com.englishreader.data.local.dao.CustomRssSourceDao {
        return database.customRssSourceDao()
    }

    @Provides
    @Singleton
    fun provideSentenceRepository(
        sentenceDao: SentenceDao
    ): SentenceRepository {
        return SentenceRepository(sentenceDao)
    }

    @Provides
    @Singleton
    fun provideRecommendationService(
        articleDao: ArticleDao,
        settingsRepository: SettingsRepository
    ): RecommendationService {
        return RecommendationService(articleDao, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): ReminderScheduler {
        return ReminderScheduler(context)
    }
    
    @Provides
    @Singleton
    fun provideTtsManager(
        @ApplicationContext context: Context
    ): TtsManager {
        return TtsManager(context)
    }
}
