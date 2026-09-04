package com.mastercompanion.data.battery

import com.mastercompanion.data.prefs.PreferencesRepository
import com.mastercompanion.di.DefaultDispatcher
import com.mastercompanion.domain.model.BatteryData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepository @Inject constructor(
    private val rootDataSource: RootBatteryDataSource,
    private val standardDataSource: StandardBatteryDataSource,
    private val preferencesRepository: PreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(defaultDispatcher)

    private val _batteryData = MutableStateFlow(BatteryData())
    val batteryData: StateFlow<BatteryData> = _batteryData.asStateFlow()

    private var activeDataSource: BatteryDataSource = standardDataSource

    init {
        scope.launch {
            initDataSourceAndListen()
        }
    }

    private suspend fun initDataSourceAndListen() {
        activeDataSource = if (rootDataSource.isOperational()) {
            Timber.i("BatteryRepository: Root access detected. Using RootBatteryDataSource.")
            rootDataSource
        } else {
            Timber.w("BatteryRepository: Root access unavailable. Falling back to StandardBatteryDataSource.")
            standardDataSource
        }

        activeDataSource.getBatteryDataFlow().collect { data ->
            _batteryData.value = data
            evaluateChargeThresholds(data)
        }
    }

    /**
     * Automated charge protection:
     * When phone is docked at >= 80%, stop charging to run on AC bypass.
     * When battery level drops to <= 75%, resume charging.
     */
    private suspend fun evaluateChargeThresholds(data: BatteryData) {
        val limitEnabled = preferencesRepository.chargeLimitEnabledFlow.first()
        if (!limitEnabled || !data.isRootControlled) return

        val stopThreshold = preferencesRepository.chargeStopThresholdFlow.first()
        val resumeThreshold = preferencesRepository.chargeResumeThresholdFlow.first()

        if (data.percentage >= stopThreshold && !data.isChargeLimitActive) {
            Timber.i("Battery reached ${data.percentage}% >= $stopThreshold%. Halting charge to engage AC bypass.")
            activeDataSource.setChargingEnabled(false)
        } else if (data.percentage <= resumeThreshold && data.isChargeLimitActive) {
            Timber.i("Battery dropped to ${data.percentage}% <= $resumeThreshold%. Resuming charging.")
            activeDataSource.setChargingEnabled(true)
        }
    }

    suspend fun setChargeLimitManual(enabled: Boolean): Result<Unit> {
        preferencesRepository.setChargeLimitEnabled(enabled)
        return if (enabled) {
            val currentLevel = _batteryData.value.percentage
            val stopThreshold = preferencesRepository.chargeStopThresholdFlow.first()
            if (currentLevel >= stopThreshold) {
                activeDataSource.setChargingEnabled(false)
            } else {
                Result.success(Unit)
            }
        } else {
            activeDataSource.setChargingEnabled(true)
        }
    }

    suspend fun toggleChargeLimit(): Boolean {
        val current = preferencesRepository.chargeLimitEnabledFlow.first()
        val newState = !current
        setChargeLimitManual(newState)
        return newState
    }
}
