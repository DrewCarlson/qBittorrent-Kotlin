package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class SchedulerDays(val value: Int) {
    EVERY_DAY(0),
    EVERY_WEEKDAY(1),
    EVERY_WEEKEND(2),
    MONDAY(3),
    TUESDAY(4),
    WEDNESDAY(5),
    THURSDAY(6),
    FRIDAY(7),
    SATURDAY(8),
    SUNDAY(9)
}
