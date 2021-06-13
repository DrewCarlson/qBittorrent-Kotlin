rootProject.name = "qBittorrent"

include(":client", ":models", ":demo")

project(":models").name = "qbittorrent-models"
project(":client").name = "qbittorrent-client"
