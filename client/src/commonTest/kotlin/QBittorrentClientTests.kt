package qbittorrent

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import qbittorrent.models.*
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
        runTest {
            val auth = client.http.plugin(QBittorrentAuth)
            if (auth.lastAuthResponseState.value?.isValidForAuth() == true) {
                client.deleteTorrents(client.getTorrents().map(Torrent::hash))
            }
        }
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

    @Test
    fun testAddTorrentLocalFile() = runTest {
        try {
            client.addTorrent {
                torrents.add("~/bbb.torrent")
                torrents.add("%USERPROFILE%/bbb.torrent")
            }
        } catch (_: NotImplementedError) {
            return@runTest // Unsupported on JS targets
        }

        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals("7f34612e0fac5e7b051b78bdf1060113350ebfe0", torrent.hash)
    }

    @Test
    fun testAddTorrentMagnetUrl() = runTest {
        client.addTorrent {
            urls.add("magnet:?xt=urn:btih:P42GCLQPVRPHWBI3PC67CBQBCM2Q5P7A&dn=big_buck_bunny_1080p_h264.mov&xl=725106140&tr=http%3A%2F%2Fblender.waag.org%3A6969%2Fannounce")
        }

        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals("7f34612e0fac5e7b051b78bdf1060113350ebfe0", torrent.hash)
    }
}
