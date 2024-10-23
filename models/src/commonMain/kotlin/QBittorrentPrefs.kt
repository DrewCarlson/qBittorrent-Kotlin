package qbittorrent.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import qbittorrent.models.preferences.*
import qbittorrent.models.serialization.*
import qbittorrent.models.serialization.GeneratePropertySerializerMapper
import qbittorrent.models.serialization.GenerateSerialNameMapper
import qbittorrent.models.serialization.NewLineListSerializer
import qbittorrent.models.serialization.SemiColonListSerializer

@GeneratePropertySerializerMapper
@GenerateSerialNameMapper
@Serializable
data class QBittorrentPrefs(
    /** Currently selected language (e.g. en_GB for English) */
    val locale: String,
    /** True if a subfolder should be created when adding a torrent */
    @SerialName("create_subfolder_enabled")
    val createSubfolderEnabled: Boolean? = null,
    /** True if torrents should be added in a Paused state */
    @SerialName("start_paused_enabled")
    val startPausedEnabled: Boolean? = null,
    /** */
    @SerialName("auto_delete_mode")
    val autoDeleteMode: Int,
    /** True if disk space should be pre-allocated for all files */
    @SerialName("preallocate_all")
    val preallocateAll: Boolean,
    /** True if ".!qB" should be appended to incomplete files */
    @SerialName("incomplete_files_ext")
    val incompleteFilesExt: Boolean,
    /** True if Automatic Torrent Management is enabled by default */
    @SerialName("auto_tmm_enabled")
    val autoTmmEnabled: Boolean,
    /** True if torrent should be relocated when its Category changes */
    @SerialName("torrent_changed_tmm_enabled")
    val torrentChangedTmmEnabled: Boolean,
    /** True if torrent should be relocated when the default save path changes */
    @SerialName("save_path_changed_tmm_enabled")
    val savePathChangedTmmEnabled: Boolean,
    /** True if torrent should be relocated when its Category's save path changes */
    @SerialName("category_changed_tmm_enabled")
    val categoryChangedTmmEnabled: Boolean,
    /** Default save path for torrents, separated by slashes */
    @SerialName("save_path")
    val savePath: String,
    /** True if folder for incomplete torrents is enabled */
    @SerialName("temp_path_enabled")
    val tempPathEnabled: Boolean,
    /** Path for incomplete torrents, separated by slashes */
    @SerialName("temp_path")
    val tempPath: String,
    /**  */
    @Serializable(with = ScanDirSerializer::class)
    @SerialName("scan_dirs")
    val scanDirs: List<ScanDir>,
    /** Path to directory to copy .torrent files to. Slashes are used as path separators */
    @SerialName("export_dir")
    val exportDir: String,
    /** Path to directory to copy .torrent files of completed downloads to. Slashes are used as path separators */
    @SerialName("export_dir_fin")
    val exportDirFinished: String,
    /** True if e-mail notification should be enabled */
    @SerialName("mail_notification_enabled")
    val mailNotificationEnabled: Boolean,
    /** e-mail where notifications should originate from */
    @SerialName("mail_notification_sender")
    val mailNotificationSender: String,
    /** e-mail to send notifications to */
    @SerialName("mail_notification_email")
    val mailNotificationEmail: String,
    /** smtp server for e-mail notifications */
    @SerialName("mail_notification_smtp")
    val mailNotificationSmtp: String,
    /** True if smtp server requires SSL connection */
    @SerialName("mail_notification_ssl_enabled")
    val mailNotificationSslEnabled: Boolean,
    /** True if smtp server requires authentication */
    @SerialName("mail_notification_auth_enabled")
    val mailNotificationAuthEnabled: Boolean,
    /** Username for smtp authentication */
    @SerialName("mail_notification_username")
    val mailNotificationUsername: String,
    /** Password for smtp authentication */
    @SerialName("mail_notification_password")
    val mailNotificationPassword: String,
    /** True if external program should be run after torrent has finished downloading */
    @SerialName("autorun_enabled")
    val autorunEnabled: Boolean,
    /** Program path/name/arguments to run if autorun_enabled is enabled; path is separated by slashes; you can use %f and %n arguments, which will be expanded by qBittorent as path_to_torrent_file and torrent_name (from the GUI; not the .torrent file name) respectively */
    @SerialName("autorun_program")
    val autorunProgram: String,
    /** True if torrent queuing is enabled */
    @SerialName("queueing_enabled")
    val queueingEnabled: Boolean,
    /** Maximum number of active simultaneous downloads */
    @SerialName("max_active_downloads")
    val maxActiveDownloads: Int,
    /** Maximum number of active simultaneous downloads and uploads */
    @SerialName("max_active_torrents")
    val maxActiveTorrents: Int,
    /** Maximum number of active simultaneous uploads */
    @SerialName("max_active_uploads")
    val maxActiveUploads: Int,
    /** If true torrents w/o any activity (stalled ones) will not be counted towards max_active_* limits */
    @SerialName("dont_count_slow_torrents")
    val dontCountSlowTorrents: Boolean,
    /** Download rate in KiB/s for a torrent to be considered "slow" */
    @SerialName("slow_torrent_dl_rate_threshold")
    val slowTorrentDlRateThreshold: Int,
    /** Upload rate in KiB/s for a torrent to be considered "slow" */
    @SerialName("slow_torrent_ul_rate_threshold")
    val slowTorrentUlRateThreshold: Int,
    /** Seconds a torrent should be inactive before considered "slow" */
    @SerialName("slow_torrent_inactive_timer")
    val slowTorrentInactiveTimer: Int,
    /** True if share ratio limit is enabled */
    @SerialName("max_ratio_enabled")
    val maxRatioEnabled: Boolean,
    /** Get the global share ratio limit */
    @SerialName("max_ratio")
    val maxRatio: Float,
    /** Action performed when a torrent reaches the maximum share ratio. */
    @Serializable(with = MaxRatioActionSerializer::class)
    @SerialName("max_ratio_act")
    val maxRatioAction: MaxRatioAction,
    /** Port for incoming connections */
    @SerialName("listen_port")
    val listenPort: Int,
    /** True if UPnP/NAT-PMP is enabled */
    val upnp: Boolean,
    /** True if the port is randomly selected */
    @SerialName("random_port")
    val randomPort: Boolean,
    /** Global download speed limit in KiB/s; -1 means no limit is applied */
    @SerialName("dl_limit")
    val dlLimit: Int,
    /** Global upload speed limit in KiB/s; -1 means no limit is applied */
    @SerialName("up_limit")
    val upLimit: Int,
    /** Maximum global number of simultaneous connections */
    @SerialName("max_connec")
    val maxConnections: Int,
    /** Maximum number of simultaneous connections per torrent */
    @SerialName("max_connec_per_torrent")
    val maxConnectionsPerTorrent: Int,
    /** Maximum number of upload slots */
    @SerialName("max_uploads")
    val maxUploads: Int,
    /** Maximum number of upload slots per torrent */
    @SerialName("max_uploads_per_torrent")
    val maxUploadsPerTorrent: Int,
    /** Timeout in seconds for a stopped announce request to trackers */
    @SerialName("stop_tracker_timeout")
    val stopTrackerTimeout: Int,
    /** True if the advanced libtorrent option piece_extent_affinity is enabled */
    @SerialName("enable_piece_extent_affinity")
    val enablePieceExtentAffinity: Boolean,
    /** Bittorrent Protocol to use (see list of possible values below) */
    @Serializable(with = BittorrentProtocolSerializer::class)
    @SerialName("bittorrent_protocol")
    val bittorrentProtocol: BittorrentProtocol,
    /** True if dul_limit should be applied to uTP connections; this option is only available in qBittorent built against libtorrent version 0.16.X and higher */
    @SerialName("limit_utp_rate")
    val limitUtpRate: Boolean,
    /** True if dul_limit should be applied to estimated TCP overhead (service data: e.g. packet headers) */
    @SerialName("limit_tcp_overhead")
    val limitTcpOverhead: Boolean,
    /** True if dul_limit should be applied to peers on the LAN */
    @SerialName("limit_lan_peers")
    val limitLanPeers: Boolean,
    /** Alternative global download speed limit in KiB/s */
    @SerialName("alt_dl_limit")
    val altDlLimit: Int,
    /** Alternative global upload speed limit in KiB/s */
    @SerialName("alt_up_limit")
    val altUpLimit: Int,
    /** True if alternative limits should be applied according to schedule */
    @SerialName("scheduler_enabled")
    val schedulerEnabled: Boolean,
    /** Scheduler starting hour */
    @SerialName("schedule_from_hour")
    val scheduleFromHour: Int,
    /** Scheduler starting minute */
    @SerialName("schedule_from_min")
    val scheduleFromMin: Int,
    /** Scheduler ending hour */
    @SerialName("schedule_to_hour")
    val scheduleToHour: Int,
    /** Scheduler ending minute */
    @SerialName("schedule_to_min")
    val scheduleToMin: Int,
    /** Scheduler days */
    @Serializable(with = SchedulerDaysSerializer::class)
    @SerialName("scheduler_days")
    val schedulerDays: SchedulerDays,
    /** True if DHT is enabled */
    val dht: Boolean,
    /** True if PeX is enabled */
    val pex: Boolean,
    /** True if LSD is enabled */
    val lsd: Boolean,
    /** */
    @Serializable(with = TorrentEncryptionSerializer::class)
    val encryption: TorrentEncryption,
    /** If true anonymous mode will be enabled */
    @SerialName("anonymous_mode")
    val anonymousMode: Boolean,
    /**  */
    @SerialName("proxy_type")
    val proxyType: ProxyType,
    /** Proxy IP address or domain name */
    @SerialName("proxy_ip")
    val proxyIp: String,
    /** Proxy port */
    @SerialName("proxy_port")
    val proxyPort: Int,
    /** True if peer and web seed connections should be proxified */
    @SerialName("proxy_peer_connections")
    val proxyPeerConnections: Boolean,
    /** True proxy requires authentication; doesn't apply to SOCKS4 proxies */
    @SerialName("proxy_auth_enabled")
    val proxyAuthEnabled: Boolean,
    /** Username for proxy authentication */
    @SerialName("proxy_username")
    val proxyUsername: String,
    /** Password for proxy authentication */
    @SerialName("proxy_password")
    val proxyPassword: String,
    /** True if proxy is only used for torrents */
    @SerialName("proxy_torrents_only")
    val proxyTorrentsOnly: Boolean? = null,
    /** True if external IP filter should be enabled */
    @SerialName("ip_filter_enabled")
    val ipFilterEnabled: Boolean,
    /** Path to IP filter file (.dat, .p2p, .p2b files are supported); path is separated by slashes */
    @SerialName("ip_filter_path")
    val ipFilterPath: String,
    /** True if IP filters are applied to trackers */
    @SerialName("ip_filter_trackers")
    val ipFilterTrackers: Boolean,
    /** Semicolon-separated list of domains to accept when performing Host header validation */
    @Serializable(with = SemiColonListSerializer::class)
    @SerialName("web_ui_domain_list")
    val webUiDomainList: List<String>,
    /** IP address to use for the WebUI */
    @SerialName("web_ui_address")
    val webUiAddress: String,
    /** WebUI port */
    @SerialName("web_ui_port")
    val webUiPort: Int,
    /** True if UPnP is used for the WebUI port */
    @SerialName("web_ui_upnp")
    val webUiUpnp: Boolean,
    /** WebUI username */
    @SerialName("web_ui_username")
    val webUiUsername: String,
    /** Plaintext WebUI password, not readable, write-only */
    @SerialName("web_ui_password")
    val webUiPassword: String? = null,
    /** True if WebUI CSRF protection is enabled */
    @SerialName("web_ui_csrf_protection_enabled")
    val webUiCsrfProtectionEnabled: Boolean,
    /** True if WebUI clickjacking protection is enabled */
    @SerialName("web_ui_clickjacking_protection_enabled")
    val webUiClickjackingProtectionEnabled: Boolean,
    /** True if WebUI cookie Secure flag is enabled */
    @SerialName("web_ui_secure_cookie_enabled")
    val webUiSecureCookieEnabled: Boolean,
    /** Maximum number of authentication failures before WebUI access ban */
    @SerialName("web_ui_max_auth_fail_count")
    val webUiMaxAuthFailCount: Int,
    /** WebUI access ban duration in seconds */
    @SerialName("web_ui_ban_duration")
    val webUiBanDuration: Int,
    /** Seconds until WebUI is automatically signed off */
    @SerialName("web_ui_session_timeout")
    val webUiSessionTimeout: Int,
    /** True if WebUI host header validation is enabled */
    @SerialName("web_ui_host_header_validation_enabled")
    val webUiHostHeaderValidationEnabled: Boolean,
    /** True if authentication challenge for loopback address (127.0.0.1) should be disabled */
    @SerialName("bypass_local_auth")
    val bypassLocalAuth: Boolean,
    /** True if webui authentication should be bypassed for clients whose ip resides within (at least) one of the subnets on the whitelist */
    @SerialName("bypass_auth_subnet_whitelist_enabled")
    val bypassAuthSubnetWhitelistEnabled: Boolean,
    /** (White)list of ipv4/ipv6 subnets for which webui authentication should be bypassed */
    @Serializable(with = NewLineListSerializer::class)
    @SerialName("bypass_auth_subnet_whitelist")
    val bypassAuthSubnetWhitelist: List<String>,
    /** True if an alternative WebUI should be used */
    @SerialName("alternative_webui_enabled")
    val alternativeWebUiEnabled: Boolean,
    /** File path to the alternative WebUI */
    @SerialName("alternative_webui_path")
    val alternativeWebUiPath: String,
    /** True if WebUI HTTPS access is enabled */
    @SerialName("use_https")
    val useHttps: Boolean,
    /** Path to SSL keyfile */
    @SerialName("web_ui_https_key_path")
    val webUiHttpsKeyPath: String,
    /** Path to SSL certificate */
    @SerialName("web_ui_https_cert_path")
    val webUiHttpsCertPath: String,
    /** True if server DNS should be updated dynamically */
    @SerialName("dyndns_enabled")
    val dyndnsEnabled: Boolean,
    /**  */
    @Serializable(with = DyndnsServiceSerializer::class)
    @SerialName("dyndns_service")
    val dyndnsService: DyndnsService,
    /** Username for DDNS service */
    @SerialName("dyndns_username")
    val dyndnsUsername: String,
    /** Password for DDNS service */
    @SerialName("dyndns_password")
    val dyndnsPassword: String,
    /** Your DDNS domain name */
    @SerialName("dyndns_domain")
    val dyndnsDomain: String,
    /** RSS refresh interval */
    @SerialName("rss_refresh_interval")
    val rssRefreshInterval: Int,
    /** Max stored articles per RSS feed */
    @SerialName("rss_max_articles_per_feed")
    val rssMaxArticlesPerFeed: Int,
    /** Enable processing of RSS feeds */
    @SerialName("rss_processing_enabled")
    val rssProcessingEnabled: Boolean,
    /** Enable auto-downloading of torrents from the RSS feeds */
    @SerialName("rss_auto_downloading_enabled")
    val rssAutoDownloadingEnabled: Boolean,
    /** Enable downloading of repack/proper Episodes */
    @SerialName("rss_download_repack_proper_episodes")
    val rssDownloadRepackProperEpisodes: Boolean,
    /** List of RSS Smart Episode Filters */
    @SerialName("rss_smart_episode_filters")
    val rssSmartEpisodeFilters: String,
    /** Enable automatic adding of trackers to new torrents */
    @SerialName("add_trackers_enabled")
    val addTrackersEnabled: Boolean,
    /** List of trackers to add to new torrent */
    @Serializable(with = NewLineListSerializer::class)
    @SerialName("add_trackers")
    val addTrackers: List<String>,
    /** Enable custom http headers */
    @SerialName("web_ui_use_custom_http_headers_enabled")
    val webUiUseCustomHttpHeadersEnabled: Boolean,
    /** List of custom http headers */
    @Serializable(with = NewLineListSerializer::class)
    @SerialName("web_ui_custom_http_headers")
    val webUiCustomHttpHeaders: List<String>,
    /** True enables max seeding time */
    @SerialName("max_seeding_time_enabled")
    val maxSeedingTimeEnabled: Boolean,
    /** Number of minutes to seed a torrent */
    @SerialName("max_seeding_time")
    val maxSeedingTime: Int,
    /**  */
    @SerialName("announce_ip")
    val announceIp: String,
    /** True always announce to all tiers */
    @SerialName("announce_to_all_tiers")
    val announceToAllTiers: Boolean,
    /** True always announce to all trackers in a tier */
    @SerialName("announce_to_all_trackers")
    val announceToAllTrackers: Boolean,
    /** Number of asynchronous I/O threads */
    @SerialName("async_io_threads")
    val asyncIoThreads: Int,
    /** List of banned IPs */
    @Serializable(with = NewLineListSerializer::class)
    @SerialName("banned_IPs")
    val bannedIps: List<String>,
    /** Outstanding memory when checking torrents in MiB */
    @SerialName("checking_memory_use")
    val checkingMemoryUse: Int,
    /** IP Address to bind to. Empty String means All addresses */
    @SerialName("current_interface_address")
    val currentInterfaceAddress: String,
    /** Network Interface used */
    @SerialName("current_network_interface")
    val currentNetworkInterface: String,
    /** Disk cache used in MiB */
    @SerialName("disk_cache")
    val diskCache: Int,
    /** Disk cache expiry interval in seconds */
    @SerialName("disk_cache_ttl")
    val diskCacheTtl: Int,
    /** Port used for embedded tracker */
    @SerialName("embedded_tracker_port")
    val embeddedTrackerPort: Int,
    /** True enables coalesce reads & writes */
    @SerialName("enable_coalesce_read_write")
    val enableCoalesceReadWrite: Boolean,
    /** True enables embedded tracker */
    @SerialName("enable_embedded_tracker")
    val enableEmbeddedTracker: Boolean,
    /** True allows multiple connections from the same IP address */
    @SerialName("enable_multi_connections_from_same_ip")
    val enableMultiConnectionsFromSameIp: Boolean,
    /** True enables os cache */
    @SerialName("enable_os_cache")
    val enableOsCache: Boolean? = null,
    /** True enables sending of upload piece suggestions */
    @SerialName("enable_upload_suggestions")
    val enableUploadSuggestions: Boolean,
    /** File pool size */
    @SerialName("file_pool_size")
    val filePoolSize: Int,
    /** Maximal outgoing port (0: Disabled) */
    @SerialName("outgoing_ports_max")
    val outgoingPortsMax: Int,
    /** Minimal outgoing port (0: Disabled) */
    @SerialName("outgoing_ports_min")
    val outgoingPortsMin: Int,
    /** True rechecks torrents on completion */
    @SerialName("recheck_completed_torrents")
    val recheckCompletedTorrents: Boolean,
    /** True resolves peer countries */
    @SerialName("resolve_peer_countries")
    val resolvePeerCountries: Boolean,
    /** Save resume data interval in min */
    @SerialName("save_resume_data_interval")
    val saveResumeDataInterval: Int,
    /** Send buffer low watermark in KiB */
    @SerialName("send_buffer_low_watermark")
    val sendBufferLowWatermark: Int,
    /** Send buffer watermark in KiB */
    @SerialName("send_buffer_watermark")
    val sendBufferWatermark: Int,
    /** Send buffer watermark factor in percent */
    @SerialName("send_buffer_watermark_factor")
    val sendBufferWatermarkFactor: Int,
    /** Socket backlog size */
    @SerialName("socket_backlog_size")
    val socketBacklogSize: Int,
    /** Upload choking algorithm used */
    @Serializable(with = UploadChokingAlgorithmSerializer::class)
    @SerialName("upload_choking_algorithm")
    val uploadChokingAlgorithm: UploadChokingAlgorithm,
    /** Upload slots behavior used */
    @Serializable(with = UploadSlotsBehaviorSerializer::class)
    @SerialName("upload_slots_behavior")
    val uploadSlotsBehavior: UploadSlotsBehavior,
    /** UPnP lease duration (0: Permanent lease) */
    @SerialName("upnp_lease_duration")
    val upnpLeaseDuration: Int,
    /** μTP-TCP mixed mode algorithm */
    @Serializable(with = UtpTcpMixedModeSerializer::class)
    @SerialName("utp_tcp_mixed_mode")
    val utpTcpMixedMode: UtpTcpMixedMode,
)
