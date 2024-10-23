rootProject.name = "qBittorrent"

include(":client", ":models", ":demo", ":codegen")

project(":models").name = "qbittorrent-models"
project(":client").name = "qbittorrent-client"
