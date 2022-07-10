package qbittorrent

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class QBittorrentClientTests {

    private lateinit var client: QBittorrentClient

    @BeforeTest
    fun setup() {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            httpClient = HttpClient {
                Logging {
                    logger = Logger.SIMPLE
                    level = LogLevel.ALL
                }
            }
        )
    }

    @AfterTest
    fun cleanup() {
        client.http.close()
    }

    @Test
    fun testLoginError() = runTest {
        val error = assertFailsWith<QBittorrentException> {
            client.login("aa", "aaa")
        }

        assertEquals(200, error.response.status.value)
        assertEquals("Fails.", error.body)
    }

    @Test
    fun testLoginSuccess() = runTest {
        client.login("admin", "adminadmin")
        assertTrue(client.getApiVersion().isNotBlank())
    }

    @Test
    fun testAutoLoginError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
        )
        val error = assertFailsWith<QBittorrentException> {
            client.getApiVersion()
        }

        assertEquals(403, error.response.status.value)
        assertEquals("Forbidden", error.body)
    }

    @Test
    fun testAutoLogin() = runTest {
        assertTrue(client.getApiVersion().isNotBlank())
    }

    @Test
    fun testAutoLoginFromMainData() = runTest {
        val result = client.syncMainData().firstOrNull()

        assertNotNull(result)
    }

    @Test
    fun testAutoLoginFromMainDataError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
        )
        val error = assertFailsWith<QBittorrentException> {
            client.syncMainData().firstOrNull()
        }

        assertEquals(403, error.response.status.value)
        assertEquals("Forbidden", error.body)
    }
}