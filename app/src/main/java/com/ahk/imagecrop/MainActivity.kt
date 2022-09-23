package com.ahk.imagecrop

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivityDebug"
    private lateinit var croppedImg : Bitmap
    private lateinit var initImg : Bitmap
    private var cropImgOptions = CropImageOptions
    private var undoList: ArrayList<Bitmap> = ArrayList()
    private lateinit var exifData : ExifInterface
    private lateinit var imageCropView : CropImageView
    private lateinit var imageCrop : ImageView
    private lateinit var imageAspect : ImageView
    private lateinit var imageFlip : ImageView
    private var initWidth = 0
    private var initHeight = 0
    private var storagePermissionGranted = false
    private var isImageAvailable = false
    private var fileName : String = ""

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(!it){
            Toast.makeText(this, "Storage Permission Required", Toast.LENGTH_SHORT).show()
        }
        storagePermissionGranted = it
    }

    private val captureImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { intent : ActivityResult? ->
        //intent
        intent?.data?.let { uri ->
            val imageUri = CropImage.getPickImageResultUriContent(this, uri)

            imageCropView.setImageUriAsync(imageUri)
            isImageAvailable = true
            resetCropRect()

            try {
                fileName = getFileName(imageUri)!!

                initImg = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
                undoList.add(initImg)

                val inputStream = contentResolver.openInputStream(imageUri)
                exifData = ExifInterface(inputStream!!)

                if(exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null &&
                    exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null){
                    initWidth = exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)!!.toInt()
                    initHeight = exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)!!.toInt()

                    initCropImageView()
                }
            }
            catch (e : Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imagePick = findViewById<ImageView>(R.id.mainPickImage)
        val imageUndo = findViewById<ImageView>(R.id.mainUndoImage)
        val imageInfo = findViewById<ImageView>(R.id.mainImageInfo)
        imageCrop = findViewById<ImageView>(R.id.mainCrop)
        imageAspect = findViewById<ImageView>(R.id.mainAspect)
        imageFlip = findViewById<ImageView>(R.id.mainFlip)
        val imageDone = findViewById<ImageView>(R.id.mainDone)
        imageCropView = findViewById<CropImageView>(R.id.mainCropImageView)

        imagePick.setOnClickListener{
            if(storagePermissionGranted)
                imageFromGallery()
            else
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        imageUndo.setOnClickListener{
            Log.e("Undo", "size : "+undoList.size)
            when (undoList.size){

                in 2 .. Int.MAX_VALUE -> {
                    try {
                        undoList.removeAt(undoList.size - 1)
                        croppedImg = undoList[undoList.size - 1]
                        imageCropView.setImageBitmap(croppedImg)
                        imageCropView.cropRect =
                            Rect(0, 0, croppedImg.width, croppedImg.height)
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                    }
                }

                1 -> {
                    try {
//                        croppedImg = undoList[0]
                        imageCropView.setImageBitmap(initImg)
                        imageCropView.cropRect = Rect(0, 0, initImg.width, initImg.height)
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                    }
                }

                else -> Toast.makeText(this, "Nothing to go back to", Toast.LENGTH_SHORT).show()
            }
        }

        imageInfo.setOnClickListener{

            if(this::initImg.isInitialized && isImageAvailable && this::exifData.isInitialized){

                var resolution = ""
                var date = ""
                var flash = ""
                var exposure = ""
                var aperture = ""

                if(exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null && exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null){
                    resolution = exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) + " x "+exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                }

                if(exifData.getAttribute(ExifInterface.TAG_DATETIME) != null){

                    date = exifData.getAttribute(ExifInterface.TAG_DATETIME).toString()
                }

                /*
                No flash - 16
                Flash - 9
                Auto - 24
                Fill - 9
                 */
                if(exifData.getAttribute(ExifInterface.TAG_FLASH) != null){
                    when(exifData.getAttribute(ExifInterface.TAG_FLASH)!!.toInt()) {

                        in 0..10 -> flash = "Flash fired"

                        else -> flash = "No flash"
                    }
                }

                if(exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) != null) {


                    if(exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)!!.toFloat() < 1f) {
                        val exp = (1 / exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)!!
                            .toFloat()).toInt()
                        exposure = "1/"+ exp
                    }
                    else
                        exposure = exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME).toString()

                    Log.e("Exp", "Val : "+exposure)
                }

                if(exifData.getAttribute(ExifInterface.TAG_F_NUMBER) != null) {
                    aperture =  "f/" + exifData.getAttribute(ExifInterface.TAG_F_NUMBER)
                }

                showImageInfo(fileName, date, resolution, flash, exposure, aperture)
            }
            else
                Toast.makeText(this, "Please add an image to begin", Toast.LENGTH_SHORT).show()

        }

        imageCrop.setOnClickListener{
            if(this::initImg.isInitialized && isImageAvailable){

                try {
                    croppedImg = imageCropView.croppedImage!!
                    imageCropView.setImageBitmap(croppedImg)
                    imageCropView.cropRect = Rect(0, 0, croppedImg.width, croppedImg.height)
                    undoList.add(croppedImg)
                }
                catch (e : Exception){
                    e.printStackTrace()
                }

                resetSelections()
                imageCrop.setColorFilter(resources.getColor(R.color.orange, theme))
            }
            else
                Toast.makeText(this, "Please add an image to begin", Toast.LENGTH_SHORT).show()
        }

        imageAspect.setOnClickListener{

            if(this::initImg.isInitialized && isImageAvailable) {
                resetSelections()
                imageAspect.setColorFilter(resources.getColor(R.color.orange, theme))

                val menu = PopupMenu(this, it)
                menu.inflate(R.menu.aspect_ratio_menu)
                menu.setForceShowIcon(true)

                menu.setOnMenuItemClickListener {

                    when (it.itemId) {

                        R.id.main_action_ratio_0 -> {
                            cropImgOptions.fixAspectRatio = false
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }

                        R.id.main_action_ratio_1 -> { //1:1
                            cropImgOptions.fixAspectRatio = true

                            cropImgOptions.ratio = kotlin.Pair(1, 1)
                            imageCropView.setAspectRatio(
                                cropImgOptions.ratio!!.first,
                                cropImgOptions.ratio!!.second
                            )
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }
                        R.id.main_action_ratio_2 -> { //4:3
                            cropImgOptions.fixAspectRatio = true

                            cropImgOptions.ratio = kotlin.Pair(4, 3)
                            imageCropView.setAspectRatio(
                                cropImgOptions.ratio!!.first,
                                cropImgOptions.ratio!!.second
                            )
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }
                        R.id.main_action_ratio_3 -> { //3:4
                            cropImgOptions.fixAspectRatio = true

                            cropImgOptions.ratio = kotlin.Pair(3, 4)
                            imageCropView.setAspectRatio(
                                cropImgOptions.ratio!!.first,
                                cropImgOptions.ratio!!.second
                            )
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }
                        R.id.main_action_ratio_4 -> { //16:9
                            cropImgOptions.fixAspectRatio = true

                            cropImgOptions.ratio = kotlin.Pair(16, 9)
                            imageCropView.setAspectRatio(
                                cropImgOptions.ratio!!.first,
                                cropImgOptions.ratio!!.second
                            )
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }
                        R.id.main_action_ratio_5 -> { //9:16
                            cropImgOptions.fixAspectRatio = true

                            cropImgOptions.ratio = kotlin.Pair(9, 16)
                            imageCropView.setAspectRatio(
                                cropImgOptions.ratio!!.first,
                                cropImgOptions.ratio!!.second
                            )
                            imageCropView.setFixedAspectRatio(cropImgOptions.fixAspectRatio)
                        }
                    }

                    false
                }

                menu.show()
            }
            else
                Toast.makeText(this, "Please add an image to begin", Toast.LENGTH_SHORT).show()
        }

        imageFlip.setOnClickListener{

            if(this::initImg.isInitialized && isImageAvailable) {
                resetSelections()
                imageFlip.setColorFilter(resources.getColor(R.color.orange, theme))

                val menu = PopupMenu(this, it)
                menu.inflate(R.menu.flip_image_menu)
                menu.setForceShowIcon(true)

                menu.setOnMenuItemClickListener {

                    when (it.itemId) {

                        R.id.main_action_flip_horizontally -> {

                            try {
                                if (this::croppedImg.isInitialized) {
                                    imageCropView.cropRect =
                                        Rect(0, 0, croppedImg.width, croppedImg.height)
                                    croppedImg = createFlippedBitmap(croppedImg, true, false)!!
                                } else {
                                    imageCropView.cropRect = Rect(0, 0, initWidth, initHeight)
                                    croppedImg = createFlippedBitmap(initImg, true, false)!!
                                }
//                            imageCropView.flipImageHorizontally()

                                imageCropView.setImageBitmap(croppedImg)
                                undoList.add(croppedImg)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        R.id.main_action_flip_vertically -> {
                            try {
                                if (this::croppedImg.isInitialized) {
                                    imageCropView.cropRect =
                                        Rect(0, 0, croppedImg.width, croppedImg.height)
                                    croppedImg = createFlippedBitmap(croppedImg, false, true)!!
                                } else {
                                    imageCropView.cropRect = Rect(0, 0, initWidth, initHeight)
                                    croppedImg = createFlippedBitmap(initImg, false, true)!!
                                }
//                            imageCropView.flipImageVertically()

                                imageCropView.setImageBitmap(croppedImg)
                                undoList.add(croppedImg)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    false
                }

                menu.show()
            }
            else
                Toast.makeText(this, "Please add an image to begin", Toast.LENGTH_SHORT).show()
        }

        imageDone.setOnClickListener{

            if(isImageAvailable){
                if(this::croppedImg.isInitialized){
                    imageCropView.cropRect = imageCropView.wholeImageRect
                    croppedImg = imageCropView.croppedImage!!
                    saveImage(croppedImg)
                }
                else
                    Toast.makeText(this, "Click crop to perform action", Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(this, "Please add an image to begin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFlippedBitmap(source: Bitmap, xFlip: Boolean, yFlip: Boolean): Bitmap? {
        val matrix = Matrix()
        matrix.postScale(
            if (xFlip) -1f else 1f,
            if (yFlip) -1f else 1f,
            source.width / 2f,
            source.height / 2f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun initCropImageView(){

        imageCropView.guidelines = cropImgOptions.guidelines
        imageCropView.cropRect = Rect(100, 300, 500, 1200)
    }

    private fun resetCropRect(){
        imageCropView.cropRect = imageCropView.wholeImageRect
    }

    private fun resetSelections(){
        imageCrop.colorFilter = null
        imageAspect.colorFilter = null
        imageFlip.colorFilter = null
    }

    private fun imageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        val mimeTypes = arrayOf("image/jpg", "image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        captureImageResult.launch(intent)
    }

    private fun saveImage(bitmap: Bitmap) {

        var name = "IMG_" + (Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time).toString() + ".jpg"
        val fos: OutputStream?
        val resolver = contentResolver
        val contentValues = ContentValues()
        //Using MediaStore to save image to gallery
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = resolver.openOutputStream(imageUri!!)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos!!.close()

        showImage(name, bitmap)
        isImageAvailable = false
    }

    private fun showImage(fileName : String,image : Bitmap) {
        val builder = Dialog(this)
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.getWindow()!!.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        builder.setOnDismissListener(DialogInterface.OnDismissListener {
            imageCropView.clearImage()
            resetSelections()
        })
        val imageView = ImageView(this)
        imageView.setImageBitmap(image)

        val textView = TextView(this)
        textView.setText(fileName)
        textView.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textView.background = ContextCompat.getDrawable(this, R.color.white)
        textView.textAlignment = LinearLayout.TEXT_ALIGNMENT_CENTER

        val lLayout = LinearLayout(this)
        lLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lLayout.orientation = LinearLayout.VERTICAL
        lLayout.addView(imageView, 0)
        lLayout.addView(textView, 1)

        builder.addContentView(
            lLayout, RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        builder.show()
    }

    private fun showImageInfo(fileName: String, date: String, resolution: String, flash: String, exposure: String, aperture: String) {
        val builder = Dialog(this)
        /*builder.requestWindowFeature(Window.FEATURE_NO_TITLE)

        )*/
        builder.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.setContentView(R.layout.dialog_image_info)
        /*builder.setOnDismissListener(DialogInterface.OnDismissListener {
        })*/

        val dateLayout = builder.findViewById<LinearLayout>(R.id.dialogDateLayout)
        val timeLayout = builder.findViewById<LinearLayout>(R.id.dialogTimeLayout)
        val resolutionLayout = builder.findViewById<LinearLayout>(R.id.dialogResLayout)
        val flashLayout = builder.findViewById<LinearLayout>(R.id.dialogFlashLayout)
        val exposureLayout = builder.findViewById<LinearLayout>(R.id.dialogExposureLayout)
        val apertureLayout = builder.findViewById<LinearLayout>(R.id.dialogApertureLayout)

        val title = builder.findViewById<TextView>(R.id.dialogTitle)
        val dateTV = builder.findViewById<TextView>(R.id.dialogDate)
        val timeTV = builder.findViewById<TextView>(R.id.dialogTime)
        val resolutionTV = builder.findViewById<TextView>(R.id.dialogResolution)
        val flashTV = builder.findViewById<TextView>(R.id.dialogFlash)
        val exposureTV = builder.findViewById<TextView>(R.id.dialogExposure)
        val apertureTV = builder.findViewById<TextView>(R.id.dialogAperture)

        title.setText(fileName)

        if(date.length > 0){

            dateLayout.visibility = View.VISIBLE
            timeLayout.visibility = View.VISIBLE

            dateTV.setText(date.split(" ")[0])
            timeTV.setText(date.split(" ")[1])
        }
        else{
            dateLayout.visibility = View.GONE
            timeLayout.visibility = View.GONE
        }

        if(resolution.length > 0){

            resolutionLayout.visibility = View.VISIBLE
            resolutionTV.setText(resolution)
        }
        else{
            resolutionLayout.visibility = View.GONE
        }

        if(flash.length > 0){

            flashLayout.visibility = View.VISIBLE
            flashTV.setText(flash)
        }
        else{
            flashLayout.visibility = View.GONE
        }

        if(exposure.length > 0){

            exposureLayout.visibility = View.VISIBLE
            exposureTV.setText(exposure)
        }
        else{
            exposureLayout.visibility = View.GONE
        }

        if(aperture.length > 0){

            apertureLayout.visibility = View.VISIBLE
            apertureTV.setText(aperture)
        }
        else{
            apertureLayout.visibility = View.GONE
        }

        builder.show()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.getScheme().equals("content")) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    if(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) >= 0) {

                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        result =
                            cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.getPath()
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}