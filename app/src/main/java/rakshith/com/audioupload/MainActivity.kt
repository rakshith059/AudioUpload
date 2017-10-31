package rakshith.com.audioupload

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import junit.framework.TestResult
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URL

class MainActivity : AppCompatActivity() {
    internal var audioSavePath: String? = null
    internal var mediaRecorder: MediaRecorder? = null
    internal var mediaPlayer: MediaPlayer? = null

    var firebaseStorageReference: StorageReference? = null

    var uploadedAudioUrl: String? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this);

        firebaseStorageReference = FirebaseStorage.getInstance().getReference()

//        LocalBroadcastManager.getInstance(this).registerReceiver(mCallbackReciver, IntentFilter(Constants.CALLBACK_INTENT_FILTER_RECIVER))

//        getAuthToken()

        signInAnonymously()

        record_button.setOnClickListener(View.OnClickListener {
            activity_main_ll_record.visibility = View.VISIBLE
        })

        activity_main_btn_record.setOnClickListener {
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

//                fab_record.visibility = View.GONE
//                fab_stop.visibility = View.VISIBLE
//                fab_upload.visibility = View.GONE

                Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show()
            } else {
                requestPermission()
            }
        }

        activity_main_btn_stop.setOnClickListener {
            if (mediaRecorder != null)
                mediaRecorder?.stop()

            audioSavePath = Uri.fromFile(File(audioSavePath)).toString()

            Glide.with(this).load(R.drawable.ic_mic).into(activity_main_iv_record_gif)

            activity_main_btn_record?.isEnabled = false
            activity_main_btn_record?.isClickable = false
            activity_main_btn_upload?.isEnabled = true
            activity_main_btn_upload?.isClickable = true

//            fab_record.visibility = View.GONE
//            fab_stop.visibility = View.GONE
//            fab_upload.visibility = View.VISIBLE

            if (!TextUtils.isEmpty(audioSavePath)) {
                activity_main_ll_player.visibility = View.VISIBLE
                activity_main_btn_upload.visibility = View.VISIBLE
                streamAudioFromFirebase(audioSavePath as String)
            }

            Toast.makeText(this, "Recording Completed", Toast.LENGTH_LONG).show()
        }

        activity_main_btn_upload.setOnClickListener {
            //            fab_record.visibility = View.VISIBLE
//            fab_stop.visibility = View.GONE
//            fab_upload.visibility = View.GONE

//            getAccessToken()

            activity_main_btn_record?.isEnabled = true
            activity_main_btn_record?.isClickable = true
            activity_main_btn_stop?.isEnabled = false
            activity_main_btn_stop?.isClickable = false
            activity_main_btn_upload?.isEnabled = false
            activity_main_btn_upload?.isClickable = false

            uploadAudioToFirebase()

//            mediaPlayer = MediaPlayer()
//
//            try {
//                mediaPlayer?.setDataSource(audioSavePath)
//                mediaPlayer?.prepare()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            mediaPlayer?.start()
        }

        pick_button.setOnClickListener(View.OnClickListener {
            pickAudioFileFromLocal()
        })

        activity_main_tv_download_url.setOnClickListener(View.OnClickListener {
            streamAudioFromFirebase(uploadedAudioUrl as String)
        })

//        fab_stream.setOnClickListener {
//        var url = "https://firebasestorage.googleapis.com/v0/b/samplefirebase-20d5a.appspot.com/o/audioUploads%2Fupload_Sample_1509295934188?alt=media&token=3d73d2de-5e73-4291-b36b-7516c0e980d4"
//            streamAudioFromFirebase(url)
//        }

//        buttonPlayLastRecordAudio.setOnClickListener {
//            buttonStop.isEnabled = false
//            buttonStart.isEnabled = false
//            buttonStopPlayingRecording.isEnabled = true
//
//            mediaPlayer = MediaPlayer()
//
//            try {
//                mediaPlayer!!.setDataSource(AudioSavePathInDevice)
//                mediaPlayer!!.prepare()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            mediaPlayer!!.start()
//
//            Toast.makeText(this@MainActivity, "Recording Playing", Toast.LENGTH_LONG).show()
//        }

//        buttonStopPlayingRecording.setOnClickListener {
//            buttonStop.isEnabled = false
//            buttonStart.isEnabled = true
//            buttonStopPlayingRecording.isEnabled = false
//            buttonPlayLastRecordAudio.isEnabled = true
//
//            if (mediaPlayer != null) {
//
//                mediaPlayer!!.stop()
//                mediaPlayer!!.release()
//
//                MediaRecorderReady()
//
//            }
//        }
    }

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

    private val PICK_AUDIO: Int = 1001

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
        }
    }

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

    private fun uploadAudioToFirebase() {
//        var uri: Uri = Uri.fromFile(File(audioSavePath))
        var storageRefrence: StorageReference? = firebaseStorageReference?.child("audioUploads/upload_Sample_" + System.currentTimeMillis() + ".mp3")

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
                })?.
                addOnFailureListener(object : OnFailureListener {
                    override fun onFailure(exception: Exception) {
                        Log.d("Rakshith", "failure ==> " + exception?.message)
                    }
                })?.addOnSuccessListener {
        }

    }

//    var mCallbackReciver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(p0: Context?, intent: Intent?) {
//            var oAuthCode: String? = intent?.getStringExtra(Constants.OAUTH_CODE)
//
//            if (!TextUtils.isEmpty(oAuthCode)) {
//                getAuthToken(oAuthCode as String)
//            }
//        }
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallbackReciver)
//    }

//    private fun getByteArrayImage(url: String?): ByteArray? {
//        try {
//            val imageUrl = URL(url)
//            val ucon = imageUrl.openConnection()
//
//            val `is` = ucon.getInputStream()
//            val bis = BufferedInputStream(`is`)
//
//            val baf = ByteArrayBuffer(500)
//            var current = 0
//            while ({ current = bis.read(); current }() != -1) {
//                baf.append(current.toByte().toInt())
//            }
//
//            return baf.toByteArray()
//        } catch (e: Exception) {
//            Log.d("ImageManager", "Error: " + e.toString())
//        }
//
//        return null
//    }


    fun MediaRecorderReady() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(audioSavePath)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO), RequestPermissionCode)
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

                if (StoragePermission && RecordPermission) {
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

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val RequestPermissionCode = 1
    }
}
