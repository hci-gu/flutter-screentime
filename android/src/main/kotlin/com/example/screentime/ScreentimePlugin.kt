package com.example.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

class ScreentimePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: android.app.Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.example.screen_time/usage")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getHourlyUsage" -> {
                val date = call.argument<String>("date") ?: ""
                val usageData = getHourlyUsage(date)
                // convert the usage data to JSON { "hour": seconds }
                val json = JSONObject()
                for ((hour, seconds) in usageData) {
                    json.put(hour, seconds)
                }
                val jsonString = json.toString()
                result.success(jsonString)
            }
            "hasUsageStatsPermission" -> {
                result.success(hasUsageStatsPermission())
            }
            "requestUsageStatsPermission" -> {
                requestUsageStatsPermission()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        activity?.let {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            it.startActivity(intent)
        }
    }

    private fun getHourlyUsage(date: String): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

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

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // Handle activity context
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
