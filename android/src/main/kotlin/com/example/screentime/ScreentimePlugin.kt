package com.example.screentime

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ScreentimePlugin */
class ScreentimePlugin: FlutterPlugin, MethodCallHandler {
  private val CHANNEL = "com.example.screen_time/usage"

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL)
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getHourlyUsage" -> {
          val date = call.argument<String>("date") ?: ""
          val usageData = getHourlyUsage(date)
          result.success(usageData)
      }
      "hasUsageStatsPermission" -> {
          result.success(hasUsageStatsPermission())
      }
      "requestUsageStatsPermission" -> {
          requestUsageStatsPermission()
          result.success(null)
      }
      "postScreenTime" -> {
          val date = call.argument<String>("date") ?: ""
          val usageData = getHourlyUsage(date)
          val json = usageDataToJSON(date, usageData)
          result.success(json.toString())
      }
      else -> result.notImplemented()
    }
  }

  private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun getHourlyUsage(date: String): Map<String, Long> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = dateFormat.parse(date) ?: return emptyMap()

        val calendar = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = startTime + 24 * 60 * 60 * 1000 - 1

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val hourlyUsage = mutableMapOf<Int, Long>().apply {
            for (hour in 0 until 24) this[hour] = 0L
        }

        var lastEventTimestamp: Long? = null
        var lastEventType: Int? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                if (lastEventTimestamp == null && event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                    lastEventTimestamp = startTime
                    lastEventType = UsageEvents.Event.ACTIVITY_RESUMED
                }

                if (lastEventTimestamp != null && lastEventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    var current = lastEventTimestamp!!
                    val end = event.timeStamp
                    while (current < end) {
                        val hour = Calendar.getInstance().apply { timeInMillis = current }.get(Calendar.HOUR_OF_DAY)
                        val nextHourStart = ((current / 3600000) + 1) * 3600000
                        val sliceEnd = minOf(end, nextHourStart)
                        hourlyUsage[hour] = (hourlyUsage[hour] ?: 0L) + (sliceEnd - current)
                        current = sliceEnd
                    }
                }

                lastEventTimestamp = event.timeStamp
                lastEventType = event.eventType
            }
        }

        return hourlyUsage.mapKeys { it.key.toString() }.mapValues { it.value / 1000 } // to seconds
    }

    private fun usageDataToJSON(date: String, usageData: Map<String, Long>): JSONObject {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        return JSONObject().apply {
            put("deviceId", deviceId)
            put("screenTimeEntries", JSONArray().apply {
                usageData.forEach { (hour, seconds) ->
                    put(JSONObject().apply {
                        put("hour", "$date $hour")
                        put("seconds", seconds)
                    })
                }
            })
        }
    }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
