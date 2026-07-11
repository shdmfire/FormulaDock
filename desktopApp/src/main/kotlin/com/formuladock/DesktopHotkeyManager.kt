package com.formuladock

import com.formuladock.core.preferences.DesktopHotkeySetting
import com.formuladock.core.preferences.FormulaDockPreferences
import io.github.kdroidfilter.nucleus.globalhotkey.GlobalHotKeyManager
import io.github.kdroidfilter.nucleus.globalhotkey.HotKeyModifier
import java.awt.EventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.kdroidfilter.nucleus.globalhotkey.plus

class DesktopHotkeyManager(
    private val onToggle: () -> Unit,
    private val preferences: FormulaDockPreferences = FormulaDockPreferences(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var observerJob: Job? = null
    private var initialized = false
    private var currentHandle = INVALID_HANDLE
    private var currentHotkey: DesktopHotkeySetting? = null

    /** Starts observing the persisted JVM hotkey setting and applies every change. */
    fun register() {
        if (observerJob != null) return
        observerJob = scope.launch {
            preferences.desktopHotkey
                .distinctUntilChanged()
                .collect { replace(it).onFailure(Throwable::printStackTrace) }
        }
    }

    fun unregister() {
        observerJob?.cancel()
        observerJob = null
        runBlocking(Dispatchers.IO) {
            mutex.withLock {
                GlobalHotKeyManager.shutdown()
                currentHandle = INVALID_HANDLE
                currentHotkey = null
                initialized = false
            }
        }
        scope.cancel()
    }

    private suspend fun replace(hotkey: DesktopHotkeySetting): Result<Unit> = mutex.withLock {
        if (hotkey == currentHotkey) return Result.success(Unit)

        ensureInitialized().getOrElse { return Result.failure(it) }

        // Register first: if the candidate is occupied, the previous hotkey remains active.
        val newHandle = GlobalHotKeyManager.register(
            keyCode = hotkey.keyCode,
            modifiers = hotkey.toNucleusModifiers(),
        ) { _, _ -> EventQueue.invokeLater { onToggle() } }

        if (newHandle < 0) return Result.failure(hotkeyException("热键注册失败，可能已被其他程序占用"))

        val previousHandle = currentHandle
        if (previousHandle >= 0 && !GlobalHotKeyManager.unregister(previousHandle)) {
            GlobalHotKeyManager.unregister(newHandle)
            return Result.failure(hotkeyException("无法替换原有热键"))
        }

        currentHandle = newHandle
        currentHotkey = hotkey
        Result.success(Unit)
    }

    private fun ensureInitialized(): Result<Unit> {
        if (initialized) return Result.success(Unit)
        if (!GlobalHotKeyManager.isAvailable) {
            return Result.failure(IllegalStateException("当前平台不支持全局热键"))
        }
        if (!GlobalHotKeyManager.initialize()) {
            return Result.failure(hotkeyException("全局热键初始化失败"))
        }
        initialized = true
        return Result.success(Unit)
    }

    private fun DesktopHotkeySetting.toNucleusModifiers(): Int {
        var result = 0
        if (ctrl) result += HotKeyModifier.CONTROL
        if (alt) result += HotKeyModifier.ALT
        if (shift) result += HotKeyModifier.SHIFT
        if (meta) result += HotKeyModifier.META
        return result
    }

    private fun hotkeyException(fallback: String) =
        IllegalStateException(GlobalHotKeyManager.lastError ?: fallback)

    private companion object {
        const val INVALID_HANDLE = -1L
    }
}
