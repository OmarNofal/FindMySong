package com.omar.findmysong.network



const val IP: String = "192.168.1.77"
const val PORT: Int = 8000

const val URL: String = "$IP:$PORT"
const val WS_URL: String = "ws://$URL/identify_song"