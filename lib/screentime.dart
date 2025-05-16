import 'dart:convert';
import 'package:flutter/services.dart';

class Screentime {
  final MethodChannel _platform = const MethodChannel(
    "com.example.screen_time/usage",
  );

  Future<bool> hasPermission() async {
    final bool permitted = await _platform.invokeMethod(
      "hasUsageStatsPermission",
    );

    return permitted;
  }

  Future<void> requestUsageStatsPermission() async {
    await _platform.invokeMethod("requestUsageStatsPermission");
  }

  Future<Map<String, int>> getUsageStats(String date) async {
    final String jsonString = await _platform.invokeMethod("getHourlyUsage", {
      "date": date,
    });

    final Map<String, dynamic> jsonMap = json.decode(jsonString);
    final List<dynamic> entries = jsonMap["screenTimeEntries"];

    final Map<String, int> resultMap = <String, int>{};
    for (final entry in entries) {
      final String hour = entry["hour"];
      final int seconds = entry["seconds"];
      resultMap[hour] = seconds;
    }

    return resultMap;
  }
}
