package com.nuevoso.launcher

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.nuevoso.launcher.ai.ProviderFactory
import com.nuevoso.launcher.agent.security.ApprovalStore
import com.nuevoso.launcher.agent.security.InMemoryApprovalStore
import com.nuevoso.launcher.data.apps.AppRepository
import com.nuevoso.launcher.data.memory.MemoryDb
import com.nuevoso.launcher.data.memory.MemoryMigrations
import com.nuevoso.launcher.data.memory.MemoryRepository
import com.nuevoso.launcher.data.settings.SettingsRepository

class App : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var memoryRepository: MemoryRepository
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var providerFactory: ProviderFactory
        private set
    lateinit var approvalStore: ApprovalStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository(this)

        val db = Room.databaseBuilder(this, MemoryDb::class.java, "memory.db")
            .addMigrations(MemoryMigrations.MIGRATION_1_2, MemoryMigrations.MIGRATION_2_3)
            .build()
        memoryRepository = MemoryRepository(db.memoryDao())

        appRepository = AppRepository(packageManager)
        providerFactory = ProviderFactory(settingsRepository)
        approvalStore = InMemoryApprovalStore()
    }

    companion object {
        lateinit var instance: App
            private set

        fun get(context: Context): App = context.applicationContext as App
    }
}
