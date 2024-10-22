package qbittorrent.models

import kotlin.test.Test
import kotlin.test.assertEquals

class SerialNameMappingTest {

    @Test
    fun testTorrentFieldNameMapping() {
        assertEquals("amount_left", Torrent::amountLeft.serialName)
        assertEquals("category", Torrent::category.serialName)
        assertEquals("f_l_piece_prio", Torrent::firstLastPiecePriority.serialName)
        assertEquals("last_activity", Torrent::lastActivity.serialName)
        assertEquals("name", Torrent::name.serialName)
    }
}
