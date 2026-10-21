package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TopicEntity::class,
        FlashcardEntity::class,
        ResourceEntity::class,
        ReviewResultEntity::class,
        StudyPackEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class LoopiDatabase : RoomDatabase() {

    abstract fun topicDao(): TopicDao

    abstract fun flashcardDao(): FlashcardDao

    abstract fun resourceDao(): ResourceDao

    abstract fun reviewResultDao(): ReviewResultDao

    abstract fun studyPackDao(): StudyPackDao

    companion object {

        @Volatile
        private var INSTANCE: LoopiDatabase? = null

        /*
         * Migration:
         * Version 1 -> Version 2
         *
         * Adds Flashcards and Resources.
         */
        private val MIGRATION_1_2 =
            object : Migration(
                1,
                2
            ) {

                override fun migrate(
                    database:
                    SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `flashcards` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `topicId` INTEGER NOT NULL,
                            `question` TEXT NOT NULL,
                            `answer` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_flashcards_topicId`
                        ON `flashcards` (`topicId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `resources` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `topicId` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `url` TEXT NOT NULL,
                            `fileUri` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `tags` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_resources_topicId`
                        ON `resources` (`topicId`)
                        """.trimIndent()
                    )
                }
            }

        /*
         * Migration:
         * Version 2 -> Version 3
         *
         * Adds persistent quiz / review results.
         */
        private val MIGRATION_2_3 =
            object : Migration(
                2,
                3
            ) {

                override fun migrate(
                    database:
                    SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `review_results` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `topicId` INTEGER NOT NULL,
                            `flashcardId` INTEGER NOT NULL,
                            `question` TEXT NOT NULL,
                            `correctAnswer` TEXT NOT NULL,
                            `userAnswer` TEXT NOT NULL,
                            `isCorrect` INTEGER NOT NULL,
                            `confidence` TEXT NOT NULL,
                            `reviewedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_review_results_topicId`
                        ON `review_results` (`topicId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_review_results_flashcardId`
                        ON `review_results` (`flashcardId`)
                        """.trimIndent()
                    )
                }
            }

        /*
         * Migration:
         * Version 3 -> Version 4
         *
         * Adds user-created and imported study packs.
         */
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `study_packs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `topicNamesJson` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_study_packs_name` ON `study_packs` (`name`)"
                    )
                }
            }

        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE `review_results` ADD COLUMN `responseTimeMillis` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE `topics` ADD COLUMN `targetCardCount` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `flashcards` ADD COLUMN `explanation` TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE `review_results` ADD COLUMN `explanation` TEXT NOT NULL DEFAULT ''")
                }
            }

        fun getDatabase(
            context: Context
        ): LoopiDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            LoopiDatabase::class.java,
                            "loopi_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7
                            )
                            .build()

                    INSTANCE =
                        instance

                    instance
                }
        }
    }
}
