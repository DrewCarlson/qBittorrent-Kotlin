package qbittorrent

import app.cash.turbine.test
import app.cash.turbine.testIn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

const val TEST_MAGNET_URL =
    "magnet:?xt=urn:btih:P42GCLQPVRPHWBI3PC67CBQBCM2Q5P7A&dn=big_buck_bunny_1080p_h264.mov&xl=725106140&tr=http%3A%2F%2Fblender.waag.org%3A6969%2Fannounce"
const val TEST_HASH = "7f34612e0fac5e7b051b78bdf1060113350ebfe0"

@OptIn(ExperimentalCoroutinesApi::class)
class QBittorrentClientTests {

    private lateinit var client: QBittorrentClient

    @BeforeTest
    fun setup() {
        client = QBittorrentClient(
            baseUrl = "http://localhost:9090",
            mainDataSyncMs = 1000,
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

        assertEquals(200, error.response?.status?.value)
        assertEquals("Fails.", error.message)
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
                torrents.add("~/bbb.torrent")
                torrents.add("%USERPROFILE%/bbb.torrent")
            }
        } catch (_: NotImplementedError) {
            return@runTest // Unsupported on JS targets
        }

        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals(TEST_HASH, torrent.hash)

        deleteTorrents()
    }

    @Test
    fun testAddTorrentMagnetUrl() = runTest {
        client.addTorrent { urls.add(TEST_MAGNET_URL) }
        val torrents = client.getTorrents()
        val torrent = assertNotNull(torrents.singleOrNull())

        assertEquals(TEST_HASH, torrent.hash)

        deleteTorrents()
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
        val mainDataFlow = client.observeMainData().testIn(this)
        mainDataFlow.awaitItem()
        assertTrue(client.isSyncing)
        mainDataFlow.cancelAndIgnoreRemainingEvents()
        withContext(Dispatchers.Default) { delay(10) }
        assertFalse(client.isSyncing)
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
            deleteTorrents()
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
        val torrentFlow = client.observeTorrent(TEST_HASH, waitIfMissing = true).testIn(this)
        client.addTorrent { urls.add(TEST_MAGNET_URL) }

        val torrent = torrentFlow.awaitItem()
        assertEquals(TEST_HASH, torrent.hash)

        torrentFlow.cancelAndIgnoreRemainingEvents()
        deleteTorrents()
    }

    private suspend fun deleteTorrents() {
        runCatching { client.deleteTorrents(listOf(TEST_HASH), deleteFiles = true) }
    }
}
