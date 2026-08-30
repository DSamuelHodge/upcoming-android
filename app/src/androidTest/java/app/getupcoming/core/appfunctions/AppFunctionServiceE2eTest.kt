package app.getupcoming.core.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionPermissionRequiredException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E verification of Upcoming's AppFunctions surface on an Android 16+
 * system image, exercised through the platform's [AppFunctionManager] — the
 * same framework path a real assistant takes.
 *
 * **Opt-in**: run with
 * `-Pandroid.testInstrumentationRunnerArguments.appFunctionsE2e=true`.
 * Requires an emulator image whose AppSearch mainline module supports the
 * v2 dynamic-schema format (`system-images;android-36.1;google_apis;x86_64`
 * or newer). The API 36.0 image from May 2025 cannot index the library:
 * its indexer cannot parse the XSD and falls back to the v1 XML property
 * that the compiler cannot emit (verified in the 1.0.0-alpha11 artifact),
 * so discovery returns empty there.
 *
 * Covers two of the three Phase 3 E2E gates (roadmap decision #1: emulator
 * path, no physical 16+ device):
 *  1. **Discovery** — all 6 shipped functions are indexed by the framework
 *     from the KSP-generated schema.
 *  2. **Auth gate (signed out)** — invoking a function without a signed-in
 *     session must NOT succeed: `requireSignedIn()` rejects with the
 *     permission-required error, so an agent can never operate on data
 *     without the user's session.
 *
 * On 36.1+ images the deterministic quick check is
 * `adb shell cmd app_function list-app-functions | grep app.getupcoming`.
 *
 * The signed-in happy path (real account against the deployed API) remains
 * a manual gate — see roadmap, "Before AppFunctions public beta".
 */
@RunWith(AndroidJUnit4::class)
class AppFunctionServiceE2eTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // Null on images without the AppFunctions service (pre-Android 16); each test
    // assumes non-null so unsupported devices report as skipped, not failed.
    private val manager: AppFunctionManager? = AppFunctionManager.getInstance(context)

    @Before
    fun requireOptIn() {
        Assume.assumeTrue(
            "Opt-in suite: run with " +
                "-Pandroid.testInstrumentationRunnerArguments.appFunctionsE2e=true " +
                "on an android-36.1+ image; skipping",
            InstrumentationRegistry.getArguments().getBoolean("appFunctionsE2e", false),
        )
    }

    private val expectedFunctions =
        setOf(
            "listEventTypes",
            "getUpcomingBookings",
            "getBooking",
            "checkAvailability",
            "createSingleUseBookingLink",
            "getPersonalShareLink",
        )

    @Test
    fun discovery_allSixFunctionsIndexedByFramework() = runBlocking {
        val m = this@AppFunctionServiceE2eTest.manager
        Assume.assumeTrue(
            "AppFunctionManager unavailable on this image (requires Android 16+); skipping",
            m != null,
        )
        val manager = m!!
        // Indexing after install is asynchronous; poll briefly.
        var ids: Set<String> = emptySet()
        repeat(6) { attempt ->
            val found = manager.searchAppFunctions(
                AppFunctionSearchSpec(packageNames = setOf("app.getupcoming")),
            )
            ids = found.map { it.id }.toSet()
            if (expectedFunctions.all { expected -> ids.any { it.endsWith(expected) } }) return@runBlocking
            Thread.sleep(5_000L * (attempt + 1))
        }
        for (expected in expectedFunctions) {
            assertTrue(
                "Expected function '$expected' to be indexed; framework returned: $ids",
                ids.any { it.endsWith(expected) },
            )
        }
    }

    @Test
    fun signedOut_invocationMustNotSucceed() = runBlocking {
        val m = this@AppFunctionServiceE2eTest.manager
        Assume.assumeTrue(
            "AppFunctionManager unavailable on this image (requires Android 16+); skipping",
            m != null,
        )
        val manager = m!!
        val response = manager.executeAppFunction(
            ExecuteAppFunctionRequest(
                targetPackageName = "app.getupcoming",
                functionIdentifier = "listEventTypes",
                functionParameters = AppFunctionData.EMPTY,
            ),
        )
        val error = response as? ExecuteAppFunctionResponse.Error
        assertNotNull(
            "Signed-out invocation must not return a success response (got: $response)",
            error,
        )
        assertTrue(
            "Expected a permission-related rejection, got: ${error!!.error}",
            error.error is AppFunctionPermissionRequiredException,
        )
    }
}
