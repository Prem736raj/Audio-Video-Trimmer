package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AudioTrimmer : Screen("audio_trimmer?fileName={fileName}&duration={duration}&size={size}&uri={uri}") {
        fun createRoute(fileName: String, duration: String, size: String, uri: String? = null): String {
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            val encodedDuration = java.net.URLEncoder.encode(duration, "UTF-8")
            val encodedSize = java.net.URLEncoder.encode(size, "UTF-8")
            val encodedUri = uri?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            return "audio_trimmer?fileName=$encodedName&duration=$encodedDuration&size=$encodedSize&uri=$encodedUri"
        }
    }
    object VideoTrimmer : Screen("video_trimmer?fileName={fileName}&duration={duration}&size={size}&uri={uri}") {
        fun createRoute(fileName: String, duration: String, size: String, uri: String? = null): String {
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            val encodedDuration = java.net.URLEncoder.encode(duration, "UTF-8")
            val encodedSize = java.net.URLEncoder.encode(size, "UTF-8")
            val encodedUri = uri?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            return "video_trimmer?fileName=$encodedName&duration=$encodedDuration&size=$encodedSize&uri=$encodedUri"
        }
    }
    object MyFiles : Screen("my_files")
    object Profile : Screen("profile")
    object PrivacyPolicy : Screen("privacy_policy")
}
