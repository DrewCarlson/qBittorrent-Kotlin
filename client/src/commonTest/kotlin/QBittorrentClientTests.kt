package qbittorrent

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

const val TEST_MAGNET_URL =
    "magnet:?xt=urn:btih:3WBFL3G4PSSV7MF37AJSHWDQMLNR63I4&dn=Big%20Buck%20Bunny&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969"
const val TEST_HASH = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c"

expect val isWindows: Boolean

class QBittorrentClientTests {

    private lateinit var httpClient: HttpClient
    private lateinit var client: QBittorrentClient

    @BeforeTest
    fun setup() {
        httpClient = HttpClient {
            Logging {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
        }
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            syncInterval = 1.seconds,
            httpClient = httpClient
        )
    }

    @AfterTest
    fun cleanup() = runTest {
        deleteTorrents()
        client.http.close()
    }

    @Test
    fun testLoginError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
            httpClient = httpClient
        )
        val error = assertFailsWith<QBittorrentException> {
            client.login()
        }

        assertEquals(200, error.response?.status?.value)
        assertEquals("Fails.", error.message)
    }

    @Test
    fun testLoginSuccess() = runTest {
        client.login()
        assertTrue(client.getApiVersion().isNotBlank())
    }

    @Test
    fun testAutoLoginError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
            httpClient = httpClient
        )
        val error = assertFailsWith<QBittorrentException> {
            client.getApiVersion()
        }

        assertEquals(403, error.response?.status?.value)
        assertEquals("Forbidden", error.message)
    }

    @Test
    fun testAutoLogin() = runTest {
        assertTrue(client.getApiVersion().isNotBlank())
    }

    @Test
    fun testAutoLoginFromMainData() = runTest {
        val result = client.observeMainData().firstOrNull()

        assertNotNull(result)
    }

    @Test
    fun testAutoLoginFromMainDataError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
            httpClient = httpClient
        )
        val error = assertFailsWith<QBittorrentException> {
            client.observeMainData().firstOrNull()
        }

        assertEquals(403, error.response?.status?.value)
        assertEquals("Forbidden", error.message)
    }

    @Test
    fun testAddTorrentLocalFile() = runTest {
        try {
            client.addTorrent {
                if (isWindows) {
                    torrents.add("%USERPROFILE%/bbb.torrent")
                } else {
                    torrents.add("~/bbb.torrent")
                }
            }
        } catch (_: NotImplementedError) {
            return@runTest // Unsupported on JS targets
        }

        Default { delay(2. seconds) }

        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals(TEST_HASH, torrent.hash)
    }

    @Test
    fun testAddTorrentMagnetUrl() = runTest {
        client.addTorrent { urls.add(TEST_MAGNET_URL) }
        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals(TEST_HASH, torrent.hash)
    }

    @Test
    fun testMainDataSyncingIsStoppedByDefault() = runTest {
        assertFalse(client.isSyncing)
    }

    @Test
    fun testMainDataSyncingIsStartedWithSubscribers() = runTest {
        client.observeMainData().test {
            awaitItem()
            assertTrue(client.isSyncing)
        }
    }

    @Test
    fun testMainDataSyncingIsStoppedWithoutSubscribers() = runTest {
        turbineScope {
            val mainDataFlow = client.observeMainData().testIn(this)
            mainDataFlow.awaitItem()
            assertTrue(client.isSyncing)
            mainDataFlow.cancelAndIgnoreRemainingEvents()
            Default { delay(10) }
            assertFalse(client.isSyncing)
        }
    }

    @Test
    fun testMainDataEmitsFullUpdate() = runTest {
        client.observeMainData().test {
            val mainData = awaitItem()
            assertTrue(mainData.fullUpdate)
            assertEquals(1, mainData.rid)
        }
    }

    @Test
    fun testMainDataThrowsAfterError() = runTest {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            username = "aaa",
            password = "aaa",
            httpClient = httpClient
        )

        client.observeMainData().test {
            val error = assertIs<QBittorrentException>(awaitError())
            assertEquals("Forbidden", error.message)
        }
    }

    @Test
    fun testTorrentFlowEmitsIfExisting() = runTest {
        client.addTorrent {
            urls.add(TEST_MAGNET_URL)
            skipChecking = true
            paused = true
        }
        client.observeTorrent(TEST_HASH, waitIfMissing = false).test {
            val torrent = awaitItem()
            assertEquals(TEST_HASH, torrent.hash)
        }
    }

    @Test
    fun testTorrentFlowCompletesIfMissing() = runTest {
        client.observeTorrent(TEST_HASH, waitIfMissing = false).test {
            awaitComplete()
        }
    }

    @Test
    fun testTorrentFlowWaitsIfMissing() = runTest {
        turbineScope {
            val torrentFlow = client.observeTorrent(TEST_HASH, waitIfMissing = true)
                .testIn(this, timeout = 3.seconds)
            client.addTorrent { urls.add(TEST_MAGNET_URL) }

            val torrent = torrentFlow.awaitItem()
            assertEquals(TEST_HASH, torrent.hash)

            torrentFlow.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testTorrentPeersFlow() = runTest {
        client.addTorrent {
            urls.add(TEST_MAGNET_URL)
            skipChecking = true
            dlLimit = 1
            upLimit = 1
        }

        Default { delay(2.seconds) }

        client.observeTorrentPeers(TEST_HASH).test {
            val torrentPeers = awaitItem()

            assertEquals(1, torrentPeers.rid)
            assertTrue(torrentPeers.peers.isNotEmpty(), "Expected peers list to have at least one value.")
        }
    }

    private suspend fun deleteTorrents() {
        runCatching { client.deleteTorrents(listOf(TEST_HASH), deleteFiles = true) }
    }
}
