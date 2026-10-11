package com.nexient.jazzy.baselineprofile

import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * UiAutomator journeys shared by the Baseline Profile generator and the macrobenchmarks.
 *
 * Everything here is tolerant of where the app happens to start (first run, profile picker, Home)
 * and of slow or missing network data: a step that can't find its target is skipped rather than
 * failing the run, so profile generation still captures what it could reach. Nothing here presses
 * Play — no torrent traffic during profiling.
 */
const val PACKAGE_NAME = "com.nexient.jazzy"
const val TAG = "JazzyBench"

private const val UI_WAIT = 10_000L
private const val NETWORK_WAIT = 20_000L

/** Name of the profile the journeys create on a fresh install. */
const val BENCH_PROFILE = "Bench"

private val DISCLAIMER = By.text("I understand")
private val CREATE_PROFILE = By.text("Create your profile")
private val GENRE_STEP = By.text(Pattern.compile("Pick 3 or more genres.*"))
private val PICKER = By.text("Who's watching?")
private val REMEMBER = By.text("Remember my profile on this device")

/** Home's tab chips only exist on phone Home ("All · Movies · TV Shows"). */
private val HOME_PHONE = By.text("TV Shows")
private val RAIL_TITLE = By.text(Pattern.compile("Trending Now|Popular|Continue Watching|Top 10.*"))

/** A SourceRow's content description: "1080p, 2.1 GB, 1.2K seeders". */
val SOURCE_ROW: BySelector = By.desc(Pattern.compile(".*\\bseeders\\b.*"))

private val ANY_START_SCREEN = By.text(
    Pattern.compile("I understand|Create your profile|Who's watching\\?|TV Shows|Pick 3 or more genres.*"),
)

val MacrobenchmarkScope.isTv: Boolean
    get() = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        .hasSystemFeature(PackageManager.FEATURE_LEANBACK)

/**
 * From a launched app to Home: accepts the disclaimer, creates [BENCH_PROFILE] on a first run (genre
 * step skipped), or picks a profile on the picker. With [rememberProfile], the picker's "Remember my
 * profile" switch is flipped so the next cold start opens straight to Home — only ever done when the
 * picker is showing at launch, which means the setting is currently off.
 */
fun MacrobenchmarkScope.goToHome(rememberProfile: Boolean = false): Boolean {
    repeat(8) {
        device.wait(Until.hasObject(ANY_START_SCREEN), UI_WAIT)
        dismissAnrDialog()
        when {
            device.hasObject(DISCLAIMER) -> device.findObject(DISCLAIMER)?.click()
            device.hasObject(GENRE_STEP) -> device.findObject(By.text("Skip"))?.click()
            device.hasObject(CREATE_PROFILE) -> createProfile()
            device.hasObject(PICKER) -> pickProfile(rememberProfile)
            isHome() -> return true
            isTv -> return true // TV Home has no phone chips; the drawer is there instead.
        }
        device.waitForIdle()
    }
    return isHome()
}

fun MacrobenchmarkScope.isHome(): Boolean = device.hasObject(HOME_PHONE) && !device.hasObject(DISCLAIMER)

private fun MacrobenchmarkScope.createProfile() {
    val field = device.findObject(By.clazz("android.widget.EditText")) ?: return
    field.click()
    field.text = BENCH_PROFILE
    device.pressBack() // hide the keyboard so "Next" isn't covered
    device.findObject(By.text("Next"))?.click() ?: device.findObject(By.text("Save"))?.click()
    device.wait(Until.hasObject(GENRE_STEP), UI_WAIT)
    device.findObject(By.text("Skip"))?.click()
}

private fun MacrobenchmarkScope.pickProfile(rememberProfile: Boolean) {
    if (rememberProfile) device.findObject(REMEMBER)?.click()
    // Prefer our own profile; otherwise the first unlocked tile (tiles describe themselves by name).
    val tile = device.findObject(By.desc(BENCH_PROFILE))
        ?: device.findObjects(By.desc(Pattern.compile(".+"))).firstOrNull {
            val d = it.contentDescription.orEmpty()
            d != "Add profile" && !d.contains("PIN") && !d.endsWith(", edit")
        }
    tile?.click()
}

/**
 * Makes sure a cold start lands on Home (profile remembered), so startup benchmarks measure
 * launch → Home rather than the picker. Leaves the app running on Home.
 */
fun MacrobenchmarkScope.ensureStartsAtHome() {
    repeat(3) { attempt ->
        killProcess()
        startActivityAndWait()
        device.wait(Until.hasObject(ANY_START_SCREEN), UI_WAIT)
        if (isHome() || (isTv && !device.hasObject(PICKER) && !device.hasObject(CREATE_PROFILE))) {
            waitForHomeContent()
            return
        }
        Log.i(TAG, "Launch didn't open Home (attempt $attempt); onboarding")
        goToHome(rememberProfile = true)
        waitForHomeContent()
    }
}

/** Waits for Home's first rail (cache or network). */
fun MacrobenchmarkScope.waitForHomeContent(): Boolean {
    dismissAnrDialog()
    return device.wait(Until.hasObject(RAIL_TITLE), NETWORK_WAIT).also { if (!it) Log.w(TAG, "No Home rail appeared") }
}

/** Slow emulators sometimes show "<app> isn't responding" for the launcher; it blocks every tap. */
fun MacrobenchmarkScope.dismissAnrDialog() {
    if (device.hasObject(By.textContains("isn't responding"))) {
        Log.w(TAG, "Dismissing an ANR dialog")
        device.findObject(By.text("Wait"))?.click()
        device.waitForIdle()
    }
}

/** The tallest scrollable on screen: Home's / Details' vertical list. */
fun UiDevice.mainList(): UiObject2? =
    runCatching { findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() } }.getOrNull()

/** Horizontal rails: scrollables that are much shorter than the screen. */
fun UiDevice.rails(): List<UiObject2> = runCatching {
    findObjects(By.scrollable(true)).filter { it.visibleBounds.height() < displayHeight / 2 && it.visibleBounds.width() > displayWidth / 2 }
}.getOrDefault(emptyList())

private fun UiObject2.safeFling(direction: Direction, device: UiDevice) = safeGesture(device) { fling(direction) }

/**
 * A bounded horizontal scroll (about [screens] rail widths). Rails use bounded scrolls rather than
 * flings so every iteration covers the same items and loads the same number of pages.
 */
private fun UiObject2.safeScroll(direction: Direction, device: UiDevice, screens: Float = 1.2f) =
    safeGesture(device) { scroll(direction, screens) }

private inline fun UiObject2.safeGesture(device: UiDevice, gesture: UiObject2.() -> Unit) {
    try {
        // Keep gestures away from the system gesture areas at the screen edges.
        setGestureMargins(device.displayWidth / 10, device.displayHeight / 8, device.displayWidth / 10, device.displayHeight / 8)
        gesture()
    } catch (_: StaleObjectException) {
    } catch (_: IllegalArgumentException) {
    }
    device.waitForIdle()
}

/** Vertical flings through Home with a horizontal fling on each rail that's on screen. */
fun MacrobenchmarkScope.scrollHomeRails(verticalFlings: Int = 3) {
    if (isTv) return dpadBrowse()
    repeat(verticalFlings) {
        device.rails().take(2).forEach { rail ->
            rail.safeScroll(Direction.RIGHT, device)
            rail.safeScroll(Direction.LEFT, device)
        }
        device.mainList()?.safeFling(Direction.DOWN, device)
    }
    repeat(verticalFlings) { device.mainList()?.safeFling(Direction.UP, device) }
}

/** Vertical-only flings, for the jank benchmark. */
fun MacrobenchmarkScope.flingHomeVertically(times: Int = 3) {
    if (isTv) return dpadBrowse()
    repeat(times) { device.mainList()?.safeFling(Direction.DOWN, device) }
    repeat(times) { device.mainList()?.safeFling(Direction.UP, device) }
}

/** Horizontal flings on the first rails on screen, for the jank benchmark. */
fun MacrobenchmarkScope.flingRailsHorizontally() {
    if (isTv) return
    device.rails().take(3).forEach { rail ->
        rail.safeScroll(Direction.RIGHT, device)
        rail.safeScroll(Direction.LEFT, device)
    }
}

private fun MacrobenchmarkScope.dpadBrowse() {
    repeat(4) {
        repeat(6) { device.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT); device.waitForIdle() }
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)
        device.waitForIdle()
    }
    repeat(4) { device.pressKeyCode(KeyEvent.KEYCODE_DPAD_UP); device.waitForIdle() }
}

/** Switches Home to the Movies tab so a tapped title has a Sources list right on Details. */
fun MacrobenchmarkScope.selectMoviesTab() {
    if (isTv) return
    device.findObject(By.text("Movies"))?.click()
    device.waitForIdle()
    waitForHomeContent()
}

/** A poster in the first rail on screen (a clickable child of the rail), or null. */
fun MacrobenchmarkScope.firstPoster(): UiObject2? {
    val rail = device.rails().firstOrNull() ?: return null
    return runCatching { rail.findObjects(By.clickable(true)).firstOrNull { visibleArea(it.visibleBounds) > 0 } }.getOrNull()
}

private fun visibleArea(r: Rect) = r.width() * r.height()

/** Taps the first poster on Home. Returns false when nothing tappable was found. */
fun MacrobenchmarkScope.openFirstTitle(): Boolean {
    if (isTv) {
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)
        device.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)
        return true
    }
    val poster = firstPoster() ?: return false
    poster.click()
    return true
}

/**
 * Waits for the first SourceRow on Details. The Sources list starts below the fold on phones and
 * off-screen LazyColumn items aren't in the accessibility tree, so once the "Sources" header exists the
 * list is scrolled (once) to bring the rows on screen.
 */
fun MacrobenchmarkScope.waitForFirstSourceRow(timeoutMs: Long = NETWORK_WAIT): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    var scrolled = isTv
    while (System.currentTimeMillis() < deadline) {
        if (device.hasObject(SOURCE_ROW)) return true
        if (!scrolled && device.hasObject(By.text("Sources"))) {
            device.mainList()?.safeScroll(Direction.DOWN, device, screens = 0.6f)
            scrolled = true
        }
        device.wait(Until.hasObject(SOURCE_ROW), 250)
    }
    return device.hasObject(SOURCE_ROW)
}

/** Scrolls Details down through the Sources list and back. */
fun MacrobenchmarkScope.scrollDetails() {
    if (isTv) {
        repeat(6) { device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN); device.waitForIdle() }
        return
    }
    repeat(3) { device.mainList()?.safeFling(Direction.DOWN, device) }
    repeat(3) { device.mainList()?.safeFling(Direction.UP, device) }
}

/** Opens Search from Home and types a query one letter at a time, then scrolls the results. */
fun MacrobenchmarkScope.searchAndType(query: String = "star") {
    if (isTv) return
    (device.findObject(By.text("Search")) ?: device.findObject(By.desc("Search")))?.click() ?: return
    val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), UI_WAIT) ?: return
    field.click()
    for (i in 1..query.length) {
        runCatching { device.findObject(By.clazz("android.widget.EditText"))?.text = query.take(i) }
        device.waitForIdle()
    }
    device.pressBack() // keyboard
    // Results are title-cased ("Star Wars"); the field itself holds the lower-case query.
    device.wait(Until.hasObject(By.textContains(query.replaceFirstChar { it.uppercase() })), NETWORK_WAIT)
    device.mainList()?.safeFling(Direction.DOWN, device)
}

/** Back to Home from wherever the journey is (bounded). */
fun MacrobenchmarkScope.backToHome() {
    repeat(4) {
        if (isHome()) return
        device.pressBack()
        device.waitForIdle()
    }
}
