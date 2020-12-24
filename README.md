# qBittorrent Kotlin

![Bintray](https://img.shields.io/bintray/v/drewcarlson/qBittorrent-Kotlin/qBittorrent-Kotlin?color=blue)
![](https://github.com/DrewCarlson/qBittorrent-Kotlin/workflows/Jvm/badge.svg)
![](https://github.com/DrewCarlson/qBittorrent-Kotlin/workflows/Js/badge.svg)
![](https://github.com/DrewCarlson/qBittorrent-Kotlin/workflows/Native/badge.svg)

Kotlin wrapper for the [qBittorrent Web API](https://github.com/qbittorrent/qBittorrent/).

## About

qBittorrent-Kotlin is written in common Kotlin to support multiplatform development.  [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) provides json (de)serialization and [Ktor](https://ktor.io) the HTTP API.

Two modules are provided: `client` contains all the HTTP code, and `models` contains just the serializable data models.

## Usage

For a comprehensive list of available endpoints and to understand the returned data, see the [qBittorrent API Docs](https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)).

Kotlin
```kotlin
val client = QBittorrentClient("http://localhost:8888")

client.addTorrent {
  urls.add("magnet:?xt=urn:btih:c12fe1c06bba254a9dc9f519b335aa7c1367a88a")
  savePath = "/downloads"
}

client.torrentFlow("c12fe1c06bba254a9dc9f519b335aa7c1367a88a")
  .onEach { torrent ->
    println("${torrent.name} : ${torrent.state}")
  }
  .launchIn(GlobalScope)

```

## Download

![](https://img.shields.io/static/v1?label=&message=Platforms&color=grey)
![](https://img.shields.io/static/v1?label=&message=Js&color=blue)
![](https://img.shields.io/static/v1?label=&message=Jvm&color=blue)
![](https://img.shields.io/static/v1?label=&message=Linux&color=blue)
![](https://img.shields.io/static/v1?label=&message=macOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=Windows&color=blue)
![](https://img.shields.io/static/v1?label=&message=iOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=tvOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=watchOS&color=blue)

Artifacts are available on [Bintray](https://bintray.com/drewcarlson/qBittorrent-Kotlin).

```kotlin
repositories {
  jcenter()
}

dependencies {
  implementation "drewcarlson.qbittorrent:client:$qbittorrent_version"
  
  // Optional: Just the data models without the client/http lib.
  implementation "drewcarlson.qbittorrent:models:$qbittorrent_version"
}
```


Note: it is required to specify a Ktor client engine implementation.
([Documentation](https://ktor.io/clients/http-client/multiplatform.html))

```groovy
dependencies {
  // Jvm/Android
  implementation("io.ktor:ktor-client-okhttp:$ktor_version")
  implementation("io.ktor:ktor-client-android:$ktor_version")
  // iOS
  implementation("io.ktor:ktor-client-ios:$ktor_version")
  // macOS/Windows/Linux
  implementation("io.ktor:ktor-client-curl:$ktor_version")
  // Javascript/NodeJS
  implementation("io.ktor:ktor-client-js:$ktor_version")
}
``` 

## License
```
Copyright (c) 2020 Andrew Carlson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
