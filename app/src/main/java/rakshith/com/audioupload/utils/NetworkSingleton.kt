package rakshith.com.audioupload.utils

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley

/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */
class NetworkSingleton private constructor(context: Context) {
    private var mRequestQueue: RequestQueue? = null
    val imageLoader: ImageLoader

    init {
        mCtx = context
        mRequestQueue = requestQueue

        imageLoader = ImageLoader(mRequestQueue,
                LruBitmapCache(context))
    }

    // getApplicationContext() is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(mCtx?.applicationContext)
            }
            return mRequestQueue as RequestQueue
        }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    companion object {
        private var mInstance: NetworkSingleton? = null
        private var mCtx: Context? = null

        @Synchronized fun getInstance(context: Context): NetworkSingleton {
            if (mInstance == null) {
                mInstance = NetworkSingleton(context)
            }
            return mInstance as NetworkSingleton
        }
    }
}
