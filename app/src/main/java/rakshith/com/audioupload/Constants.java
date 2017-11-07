package rakshith.com.audioupload;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Constants {
    public static final int MAX_KEYWORD_LENGTH = 30;
    public static final String DEFAULT_KEYWORD = "youTubeAndroidUpload";
    // A playlist ID is a string that begins with PL. You must replace this string with the correct
    // playlist ID for the app to work
//    public static final String UPLOAD_PLAYLIST = "PLsRbvkxQ5LsX1rKjVknT-mVPEP9LoO6zO";
    public static final String UPLOAD_PLAYLIST = "https://www.youtube.com/playlist?list=PLsRbvkxQ5LsX1rKjVknT-mVPEP9LoO6zO&jct=wa17AMhZmgqc5WTI5-0eAkbK1b1PmA";
    public static final String APP_NAME = "YouTubeUpload";
    public static final String ADD_VIDEO_PLAYLIST = "https://www.googleapis.com/youtube/v3/playlistItems";
    public static final String OAUTH_CODE = "OAUTH_CODE";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String UPLOAD_AUDIO_URL = "https://api.mixcloud.com//upload/?access_token=";
    public static final String CALLBACK_INTENT_FILTER_RECIVER = "CALLBACK_INTENT_FILTER_RECIVER";
    public static final int MAX_IMAGES = 4;

    //for google vision API
    public static String accessToken = "AIzaSyCiAJLcneiMwv33Kw8HvXStp8B5uiwpRGY";
    public static final String VERIFY_OFFENSIVE_IMAGE_URL = "https://vision.googleapis.com/v1/images:annotate?key=" + accessToken;
    public static final String SAFE_SEARCH_DETECTION = "SAFE_SEARCH_DETECTION";
    public static final String VERY_UNLIKELY = "VERY_UNLIKELY";
    public static final String UNLIKELY = "UNLIKELY";

    /**
     * Returns a shared preference value based on the key provided.
     *
     * @param mContext current app context
     * @param key      key whose shared preference value is to be fetched
     * @return value from the shared preferences
     */
    public static String getSharedPreference(Context mContext, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getString(key, "");
    }

    /**
     * sets shared preferences
     *
     * @param mContext current app context
     * @param key      key whose value is to be changed/added
     * @param value    value to be updated
     */
    public static void setSharedPrefrence(Context mContext, String key, String value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }
}