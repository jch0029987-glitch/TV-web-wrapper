package com.example.messengerwrapper

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

class TvDebugServer(
    private val port: Int = 8080,
    private val onNavigate: (String) -> Unit,
    private val onReloadExtensions: () -> Unit,
    private val onClearCache: () -> Unit,
    private val getBlockedCount: () -> Int
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        thread {
            try {
                serverSocket = ServerSocket(port)
                Log.d("TvDebugServer", "Debug web server started on port $port")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    thread { handleClient(socket) }
                }
            } catch (e: Exception) {
                Log.e("TvDebugServer", "Server error", e)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            
            if (requestLine.contains("GET /navigate?url=")) {
                try {
                    val rawUrl = requestLine.substringAfter("url=").substringBefore(" HTTP/")
                    val decodedUrl = URLDecoder.decode(rawUrl, "UTF-8")
                    onNavigate(decodedUrl)
                } catch (e: Exception) {
                    Log.e("TvDebugServer", "Failed to parse navigation URL", e)
                }
            } else if (requestLine.contains("GET /action?type=reload_ext")) {
                onReloadExtensions()
            } else if (requestLine.contains("GET /action?type=clear_cache")) {
                onClearCache()
            }

            val logs = DebugConsoleStore.getLogs()
            val logHtml = if (logs.isEmpty()) {
                "<p style='color: #888;'>No logs recorded yet.</p>"
            } else {
                logs.joinToString("<br>") { line: String ->
                    val escaped = line.replace("<", "&lt;").replace(">", "&gt;")
                    if (escaped.contains("CRASH") || escaped.contains("ERROR")) {
                        "<span style='color: #f43f5e;'>$escaped</span>"
                    } else {
                        "<span style='color: #4ade80;'>$escaped</span>"
                    }
                }
            }

            val blockedCount = getBlockedCount()

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>TV Browser Remote Dashboard</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta http-equiv="refresh" content="3">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace; background: #0f172a; color: #38bdf8; padding: 15px; margin: 0; }
                        h1 { color: #f8fafc; font-size: 20px; margin-bottom: 5px; }
                        .sub { color: #94a3b8; font-size: 12px; margin-bottom: 15px; }
                        .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
                        .card h3 { margin-top: 0; font-size: 14px; color: #cbd5e1; }
                        .log-box { background: #090d16; padding: 10px; border-radius: 6px; height: 350px; overflow-y: scroll; font-size: 11px; line-height: 1.4; border: 1px solid #1e293b; white-space: pre-wrap; word-break: break-all; }
                        .status { display: inline-block; width: 8px; height: 8px; background: #4ade80; border-radius: 50%; margin-right: 6px; }
                        input[type=text] { width: 70%; padding: 8px; background: #0f172a; border: 1px solid #475569; color: #fff; border-radius: 4px; }
                        button { padding: 8px 12px; background: #0284c7; color: white; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
                        button.secondary { background: #475569; margin-top: 5px; }
                        .metrics { font-size: 14px; color: #f43f5e; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <h1>TV Browser Control Center</h1>
                    <div class="sub"><span class="status"></span>Status: Online (Tailscale Connected)</div>
                    
                    <div class="card">
                        <h3>Quick Navigation</h3>
                        <form action="/navigate" method="get">
                            <input type="text" name="url" placeholder="https://example.com" required>
                            <button type="submit">Go</button>
                        </form>
                    </div>

                    <div class="card">
                        <h3>Diagnostics & Actions</h3>
                        <div class="metrics">Blocked Ads/Trackers: $blockedCount</div>
                        <br>
                        <button onclick="location.href='/action?type=reload_ext'">Reload Extensions</button>
                        <button class="secondary" onclick="location.href='/action?type=clear_cache'">Clear WebView Cache</button>
                    </div>

                    <div class="card">
                        <h3>Live Stream (Java Exceptions & JS Console)</h3>
                        <div class="log-box">$logHtml</div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: ${html.toByteArray().size}\r\n\r\n$html"
            socket.outputStream.write(response.toByteArray())
            socket.outputStream.flush()
            socket.close()
        } catch (e: Exception) {
            // Suppress socket disconnect exceptions
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (e: Exception) {}
    }
}
