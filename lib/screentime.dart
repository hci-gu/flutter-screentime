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
    final Map<Object?, Object?> result = await _platform.invokeMethod(
      "getHourlyUsage",
      {"date": date},
    );

    // cast the result to Map<String, int>
    final Map<String, int> resultMap = <String, int>{};
    result.forEach((Object? key, Object? value) {
      if (key is String && value is int) {
        resultMap[key] = value;
      }
    });
    return resultMap;
  }
}
