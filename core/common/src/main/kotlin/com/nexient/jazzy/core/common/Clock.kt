package com.nexient.jazzy.core.common

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Wall-clock access behind an interface so TTLs, decay and grouping are testable. */
interface Clock {
    fun nowMillis(): Long
    fun today(): LocalDate = java.time.Instant.ofEpochMilli(nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {
    @Binds
    abstract fun clock(impl: SystemClock): Clock
}

const val MINUTE_MS = 60_000L
const val HOUR_MS = 60 * MINUTE_MS
const val DAY_MS = 24 * HOUR_MS
