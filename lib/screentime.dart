import 'package:flutter/services.dart';

import 'screentime_platform_interface.dart';

class Screentime {
  final MethodChannel _platform = const MethodChannel(
    "com.example.screen_time/usage",
  );

  Future<String?> getPlatformVersion() {
    return ScreentimePlatform.instance.getPlatformVersion();
  }

  Future<bool> requestPermission() async {
    final bool permitted = await _platform.invokeMethod(
      "hasUsageStatsPermission",
    );

    return permitted;
  }

  Future<void> requestUsageStatsPermission() async {
    await _platform.invokeMethod("requestUsageStatsPermission");
  }

  Future<Map<String, int>> getUsageStats(String date) async {
    final Map<String, int> result = await _platform.invokeMethod(
      "getHourlyUsage",
      {"date": date},
    );

    return result;
  }
}
