package rakshith.com.audioupload

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.internal.entity.CaptureStrategy
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.concurrent.Immutable

class MainActivity : AppCompatActivity(), View.OnClickListener {
    internal var audioSavePath: String? = null
    internal var imagePath: String? = null
    internal var mediaRecorder: MediaRecorder? = null
    internal var mediaPlayer: MediaPlayer? = null

    var firebaseStorageReference: StorageReference? = null

    var uploadedAudioUrl: String? = null

    private val PICK_AUDIO: Int = 1001
    private val PICK_IMAGE: Int = 1002
    private val CAPTURE_IMAGE: Int = 1003

    val EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path"

    private var uriAdapter: UriAdapter? = UriAdapter()
    var imageUris: List<Uri> = listOf()

    var mCurrentPhotoPath: String? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this);

        firebaseStorageReference = FirebaseStorage.getInstance().getReference()

//        LocalBroadcastManager.getInstance(this).registerReceiver(mCallbackReciver, IntentFilter(Constants.CALLBACK_INTENT_FILTER_RECIVER))

//        getAuthToken()

        signInAnonymously()

        var builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        var gridLayoutManger: GridLayoutManager? = GridLayoutManager(this, 3)
        gridLayoutManger?.orientation = LinearLayoutManager.VERTICAL
        recyclerview?.setLayoutManager(gridLayoutManger)
        recyclerview?.setAdapter(uriAdapter)

        imagePath = Environment.getExternalStorageDirectory().absolutePath + "/" + resources.getString(R.string.app_name) + System.currentTimeMillis() + ".jpeg"

        record_button.setOnClickListener(this)
        activity_main_btn_record.setOnClickListener(this)
        activity_main_btn_stop.setOnClickListener(this)
        activity_main_btn_upload.setOnClickListener(this)
        pick_button.setOnClickListener(this)
        pick_image_button.setOnClickListener(this)
        activity_main_tv_download_url.setOnClickListener(this)
        capture_image_button.setOnClickListener(this)
        activity_main_iv_upload_image.setOnClickListener(this)
    }

    /**
     * onClick event
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.record_button -> {
                activity_main_ll_record.visibility = View.VISIBLE
            }
            R.id.activity_main_btn_record -> {
                if (checkPermission()) {

                    audioSavePath = Environment.getExternalStorageDirectory().absolutePath + "/" + resources.getString(R.string.app_name) + System.currentTimeMillis() + ".mp3"

                    MediaRecorderReady()
                    activity_main_iv_record_gif.visibility = View.VISIBLE
                    Glide.with(this).load(R.drawable.recording_audio).into(activity_main_iv_record_gif)

                    try {
                        mediaRecorder?.prepare()
                        mediaRecorder?.start()
                    } catch (e: IllegalStateException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }

                    activity_main_btn_stop?.isEnabled = true
                    activity_main_btn_stop?.isClickable = true
                    activity_main_btn_upload?.isEnabled = false
                    activity_main_btn_upload?.isClickable = false
                    Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show()
                } else {
                    requestPermission()
                }
            }
            R.id.activity_main_btn_stop -> {
                if (mediaRecorder != null)
                    mediaRecorder?.stop()

                audioSavePath = Uri.fromFile(File(audioSavePath)).toString()

                Glide.with(this).load(R.drawable.ic_mic).into(activity_main_iv_record_gif)

                activity_main_btn_record?.isEnabled = false
                activity_main_btn_record?.isClickable = false
                activity_main_btn_upload?.isEnabled = true
                activity_main_btn_upload?.isClickable = true

                if (!TextUtils.isEmpty(audioSavePath)) {
                    activity_main_ll_player.visibility = View.VISIBLE
                    activity_main_btn_upload.visibility = View.VISIBLE
                    streamAudioFromFirebase(audioSavePath as String)
                }

                Toast.makeText(this, "Recording Completed", Toast.LENGTH_LONG).show()
            }
            R.id.activity_main_btn_upload -> {
                activity_main_btn_record?.isEnabled = true
                activity_main_btn_record?.isClickable = true
                activity_main_btn_stop?.isEnabled = false
                activity_main_btn_stop?.isClickable = false
                activity_main_btn_upload?.isEnabled = false
                activity_main_btn_upload?.isClickable = false

                var storyName: String? = activity_main_tie_story_name?.text.toString()
                if (!TextUtils.isEmpty(storyName)) {
                    activity_main_til_story_name?.isErrorEnabled = false
                    uploadAudioToFirebase(storyName)
                } else {
                    activity_main_til_story_name?.error = resources.getString(R.string.error_story_name)
                }
            }
            R.id.pick_button -> {
                pickAudioFileFromLocal()
            }
            R.id.activity_main_tv_download_url -> {
                streamAudioFromFirebase(uploadedAudioUrl as String)
            }
            R.id.pick_image_button -> {
                if (checkPermission()) {
                    pickImageFromGallery()
                } else {
                    requestPermission()
                }
            }
//            R.id.capture_image_button -> {
//                if (checkPermission()) {
//                    captureFromCamera()
//                } else {
//                    requestPermission()
//                }
//            }
            R.id.activity_main_iv_upload_image -> {
                var storyName: String? = activity_main_tie_story_name?.text.toString()
                if (!TextUtils.isEmpty(storyName)) {
                    activity_main_til_story_name?.isErrorEnabled = false
                    if (isSafeImage) {
                        uploadImageToFirebase(storyName)
                    } else {
                        Toast.makeText(this, "Image selected is not safe to upload", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    activity_main_til_story_name?.error = resources.getString(R.string.error_story_name)
                }
            }
        }
    }

    /**
     * This method is used to pick the image from gallery
     */
    private fun pickImageFromGallery() {
        Matisse.from(this@MainActivity)
                .choose(MimeType.allOf())
                .capture(true)
                .captureStrategy(CaptureStrategy(true, "com.your.package.fileProvider"))
                .countable(true)
                .maxSelectable(Constants.MAX_IMAGES)
                .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.size_120dp))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(GlideEngine())
                .forResult(PICK_IMAGE);

//        val intent = Intent()
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        intent.action = Intent.ACTION_GET_CONTENT
//        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }

    /**
     * This method is used to capture the image
     */
    //    private fun captureFromCamera() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        // Ensure that there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            // Create the File where the photo should go
//            var photoFile: File? = null
//            try {
//                photoFile = createImageFile()
//            } catch (ex: IOException) {
//                // Error occurred while creating the File
//                return
//            }
//
//            // Continue only if the File was successfully created
//            if (photoFile != null) {
//                val photoURI = Uri.fromFile(createImageFile())
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                startActivityForResult(takePictureIntent, CAPTURE_IMAGE)
//            }
//        }
//    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera")
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.absolutePath
        return image
    }

    /**
     * This method is for singIn of anonymous user
     */
    private fun signInAnonymously() {
        var auth: FirebaseAuth = FirebaseAuth.getInstance()
        auth.signInAnonymously().addOnSuccessListener {
            object : OnSuccessListener<AuthResult> {
                override fun onSuccess(authResult: AuthResult?) {
                    Log.d("Rakshith", "Anonymous login success ==> " + authResult)
                }
            }
            object : OnFailureListener {
                override fun onFailure(exception: Exception) {
                    Log.d("Rakshith", "Anonymous login failure ==> " + exception)
                }
            }
            object : OnCompleteListener<Task<AuthResult>> {
                override fun onComplete(authResult: Task<Task<AuthResult>>) {
                    Log.d("Rakshith", "Anonymous login success ==> " + authResult.result)

                }
            }
        }
    }

    /**
     * This method is for picking audio file from sd-card
     */
    private fun pickAudioFileFromLocal() {
        var intent: Intent = Intent()
        intent.setAction(Intent.ACTION_GET_CONTENT)
        intent.setType("audio/*")
        startActivityForResult(intent, PICK_AUDIO)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                //the selected audio.
                var uri = data?.getData();
                Log.d("Rakshith", "Uri from picking the audio file ==> " + uri)

                if (uri != null) {
                    activity_main_ll_player.visibility = View.VISIBLE
                    activity_main_btn_upload.visibility = View.VISIBLE

                    activity_main_btn_upload.isEnabled = true
                    activity_main_btn_upload.isClickable = true

                    audioSavePath = uri.toString()

                    activity_main_btn_play.setOnClickListener(View.OnClickListener {
                        var mediaPlayer: MediaPlayer = MediaPlayer()

                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        try {
                            mediaPlayer.setDataSource(uri.toString())
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnPreparedListener(MediaPlayer.OnPreparedListener { mediaPlayer.start() })
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    })
                }
            }
        } else if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && null != data) {
            // Get the Image from data
            imageUris = Matisse.obtainResult(data)
            uriAdapter?.setData(Matisse.obtainResult(data))
//            var uri: Uri? = data?.data
//            imagePath = uri?.toString()
//            var inputStream: InputStream = this.getContentResolver().openInputStream(uri)
//            verifyAndDisplaySelectedImage(inputStream)
        }
//        else if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK && data != null) {
//            // Show the thumbnail on ImageView
//            var imageUri: Uri = Uri.parse(mCurrentPhotoPath)
//            imagePath = imageUri?.toString()
//            var file = File(imageUri.getPath());
//            try {
//                var inputStream: InputStream = FileInputStream(file)
//                verifyAndDisplaySelectedImage(inputStream)
//            } catch (e: FileNotFoundException) {
//                return
//            }
//        }
    }

    private fun verifyAndDisplaySelectedImage(inputStream: InputStream) {
        var bitmap = BitmapFactory.decodeStream(inputStream)
        activity_main_iv_image.setImageBitmap(bitmap)

//        verifyImage(bitmap)
    }

    private var isSafeImage: Boolean = true

//    private fun verifyImage(bitmap: Bitmap) {
//        var byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//        var byteArray: ByteArray? = byteArrayOutputStream.toByteArray()
//
//        var encodedImage: String? = Base64.encodeToString(byteArray, Base64.DEFAULT)
//
//        var params = HashMap<String, String>()
//
////        var jsonFeatureTypeObject = JSONObject()
////        var jsonFeatureTypeArray = JSONArray()
////        var jsonContentObject = JSONObject()
////        var jsonImageObject = JSONObject()
////        var jsonRequestArray = JSONArray()
////        var jsonRequestObject = JSONObject()
////
////        jsonFeatureTypeObject.put("type", Constants.SAFE_SEARCH_DETECTION)
////        jsonFeatureTypeArray.put(jsonFeatureTypeObject)
////
////        var featureMap = HashMap<String, Any>()
////        featureMap.put("features", jsonFeatureTypeArray)
////
////        jsonContentObject.put("content", encodedImage)
////        jsonImageObject.put("image", jsonContentObject)
////
////        jsonRequestArray.put(jsonImageObject)
////        jsonRequestArray.put(featureMap)
////        jsonRequestObject.put("requests", jsonRequestArray)
//
//        var paramString = "{\n" +
//                "\t\"requests\": [{\n" +
//                "\t\t\"image\": {\n" +
//                "\t\t\t\"content\": \"$encodedImage" +
//                "\t\t},\n" +
//                "\t\t\"features\": [{\n" +
//                "\t\t\t\"type\": \"SAFE_SEARCH_DETECTION\"\n" +
//                "\t\t}]\n" +
//                "\t}]\n" +
//                "}"
//
//        params.put("body", paramString)
//
//        Log.d("Rakshith", "json request for safe search => " + paramString)
//
//        var safeSearchRequest = NetworkVolleyRequest(NetworkVolleyRequest.RequestMethod.POST, Constants.VERIFY_OFFENSIVE_IMAGE_URL, String::class.java, params, HashMap<String, Any>(), object : NetworkVolleyRequest.Callback<Any> {
//            override fun onSuccess(response: Any) {
//                Log.d("Rakshith", "success response==" + response.toString())
//                var gson: Gson = Gson()
//                var safeResponseJson = gson.fromJson(response as String, SafeResponse::class.java)
//
//                var adultContent: String? = safeResponseJson?.responses?.get(0)?.safeSearchAnnotation?.adult as String
//                var violentContent: String? = safeResponseJson?.responses?.get(0)?.safeSearchAnnotation?.violence as String
//
//
//                if (adultContent?.equals(Constants.VERY_UNLIKELY) as Boolean && adultContent?.equals(Constants.UNLIKELY) as Boolean ||
//                        violentContent?.equals(Constants.VERY_UNLIKELY) as Boolean && violentContent?.equals(Constants.UNLIKELY) as Boolean) {
//                    isSafeImage = true
//                } else isSafeImage = false
//            }
//
//            override fun onError(errorCode: Int, errorMessage: String) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//        }, NetworkVolleyRequest.ContentType.JSON)
//
//        safeSearchRequest.execute()
//    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun streamAudioFromFirebase(url: String) {
        if (!TextUtils.isEmpty(url)) {
            var mediaPlayer: MediaPlayer = MediaPlayer()
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare() // might take long! (for buffering, etc)

            Log.d("Rakshith", "duration of the audio file ==> " + mediaPlayer.duration)
            mediaPlayer.start()
        }
    }

    private fun uploadAudioToFirebase(storyName: String?) {
//        var uri: Uri = Uri.fromFile(File(audioSavePath))
        var storageRefrence: StorageReference? = firebaseStorageReference?.child("audioUploads/$storyName/upload_Sample_" + System.currentTimeMillis() + ".mp3")

        storageRefrence?.
                putFile(Uri.parse(audioSavePath))?.
                addOnCompleteListener(object : OnCompleteListener<UploadTask.TaskSnapshot> {
                    override fun onComplete(taskSnapshot: Task<UploadTask.TaskSnapshot>) {
                        val result = taskSnapshot?.result
                        Log.d("Rakshith", "success from onCompleteListner ==> " + result.downloadUrl)
                        Toast.makeText(this@MainActivity, result.downloadUrl.toString(), Toast.LENGTH_SHORT).show()

                        uploadedAudioUrl = result?.downloadUrl?.toString()
                        activity_main_tv_download_url.text = result?.downloadUrl?.toString()
                    }
                })?.addOnFailureListener(object : OnFailureListener {
            override fun onFailure(exception: Exception) {
                Log.d("Rakshith", "failure ==> " + exception?.message)
            }
        })?.addOnProgressListener(object : OnProgressListener<UploadTask.TaskSnapshot> {
            override fun onProgress(progress: UploadTask.TaskSnapshot?) {
                val progress = (progress?.getBytesTransferred()?.times(100))?.div(progress?.getTotalByteCount())
                Toast.makeText(this@MainActivity, "uploading " + progress + "% done", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadImageToFirebase(storyName: String?) {
        for (pos in imageUris?.indices) {
            var storageRefrence: StorageReference? = firebaseStorageReference?.child("audioUploads/$storyName/image_upload_Sample" + pos + ".jpeg")

            storageRefrence?.putFile(imageUris?.get(pos))?.addOnCompleteListener(object : OnCompleteListener<UploadTask.TaskSnapshot> {
                override fun onComplete(taskSnapShot: Task<UploadTask.TaskSnapshot>) {
//                var result = taskSnapShot?.getResult()
                    Toast.makeText(this@MainActivity, "image upload success" + imageUris?.get(pos), Toast.LENGTH_SHORT).show()
                    Log.d("Rakshith", "image upload success ==> " + imageUris?.get(pos))
                }
            })?.addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: Exception) {
                    Log.d("Rakshith", "failure ==> " + exception?.message)
                }
            })

        }
    }


    fun MediaRecorderReady() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(audioSavePath)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), RequestPermissionCode)
    }

    override fun onResume() {
        super.onResume()
//        getAccessToken()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RequestPermissionCode -> if (grantResults.size > 0) {

                val StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                val CameraPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED

                if (StoragePermission && RecordPermission && CameraPermission) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()

                }
            }
        }
    }

    fun checkPermission(): Boolean {

        val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        val result2 = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val RequestPermissionCode = 1
    }

    private class UriAdapter : RecyclerView.Adapter<UriAdapter.UriViewHolder>() {

        private var mUris: List<Uri>? = null

        internal fun setData(uris: MutableList<Uri>?) {
            mUris = uris
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriViewHolder {
            return UriViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.uri_item, parent, false))
        }

        override fun onBindViewHolder(holder: UriViewHolder, position: Int) {
            var imageUri: Uri? = mUris?.get(position)
//            holder?.mUri.text = imageUri?.toString()

            try {
                var inputStream: InputStream = holder?.mImage?.context.getContentResolver().openInputStream(imageUri)
                var bitmap = BitmapFactory.decodeStream(inputStream)

                getScaledBitmap(bitmap, 400F, 400F)
                holder?.mImage?.setImageBitmap(bitmap)
            } catch (e: FileNotFoundException) {
                return
            }

//            holder?.mUri.alpha = if (position % 2 == 0) 1.0f else 0.54f
        }

        private fun getScaledBitmap(bitmap: Bitmap?, reqWidth: Float, reqHeight: Float): Bitmap {
            var matrix: Matrix = Matrix()
            matrix.setRectToRect(RectF(0F, 0F, bitmap?.getWidth()?.toFloat() as Float, bitmap?.getHeight()?.toFloat()), RectF(0F, 0F, reqWidth, reqHeight), Matrix.ScaleToFit.CENTER)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap?.width, bitmap?.height, matrix, true)
        }

        override fun getItemCount(): Int {
            return if (mUris == null) 0 else mUris?.size as Int
        }

        internal class UriViewHolder(contentView: View) : RecyclerView.ViewHolder(contentView) {
            //            val mUri: TextView
            val mImage: ImageView

            init {
//                mUri = contentView.findViewById<TextView>(R.id.uri)
                mImage = contentView.findViewById<ImageView>(R.id.uri_item_iv_image)
            }
        }
    }
}