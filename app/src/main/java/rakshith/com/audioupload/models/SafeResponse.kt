package rakshith.com.audioupload.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created AudioUpload by rakshith on 11/7/17.
 */
class SafeResponse {
    @SerializedName("responses")
    @Expose
    var responses: List<SafeSearchResponse>? = null

}

class SafeSearchResponse {
    @SerializedName("safeSearchAnnotation")
    @Expose
    val safeSearchAnnotation: SafeSearchAnnotation? = null
}

class SafeSearchAnnotation {
    @SerializedName("adult")
    @Expose
    val adult: String? = null
    @SerializedName("spoof")
    @Expose
    val spoof: String? = null
    @SerializedName("medical")
    @Expose
    val medical: String? = null
    @SerializedName("violence")
    @Expose
    val violence: String? = null
}
