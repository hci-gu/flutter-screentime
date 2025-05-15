
import 'screentime_platform_interface.dart';

class Screentime {
  Future<String?> getPlatformVersion() {
    return ScreentimePlatform.instance.getPlatformVersion();
  }
}
