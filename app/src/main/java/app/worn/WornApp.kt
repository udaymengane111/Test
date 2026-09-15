package app.worn

import android.app.Application
import androidx.room.Room
import app.worn.data.TrackingRepository
import app.worn.data.local.WornDatabase
import app.worn.notifications.WornNotifications

class WornApp : Application() {
    lateinit var database: WornDatabase
        private set
    lateinit var repository: TrackingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, WornDatabase::class.java, "worn.db")
            .fallbackToDestructiveMigration()
            .build()
        repository = TrackingRepository(database)
        WornNotifications.ensureChannels(this)
    }

    companion object {
        lateinit var instance: WornApp
            private set
    }
}
