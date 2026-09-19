package app.noor.prayer.di

import app.noor.prayer.core.media.RadioRepository
import app.noor.prayer.data.PrayerScheduleRepository
import app.noor.prayer.data.SettingsRepository
import app.noor.prayer.domain.PrayerEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun schedules(): PrayerScheduleRepository
    fun settings(): SettingsRepository
    fun engine(): PrayerEngine
    fun radio(): RadioRepository
}
