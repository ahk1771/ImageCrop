package com.ahk.imagecrop

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView

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
    private lateinit var imageRotate : ImageView
    private lateinit var imageFlip : ImageView
    private var initWidth = 0
    private var initHeight = 0

    private val captureImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { intent : ActivityResult? ->
        //intent
        intent?.data?.let { uri ->
            val imageUri = CropImage.getPickImageResultUriContent(this, uri)

            imageCropView.setImageUriAsync(imageUri)
            resetCropRect()

            try {

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
        imageRotate = findViewById<ImageView>(R.id.mainRotate)
        imageFlip = findViewById<ImageView>(R.id.mainFlip)
        val imageDone = findViewById<ImageView>(R.id.mainDone)
        imageCropView = findViewById<CropImageView>(R.id.mainCropImageView)

        imagePick.setOnClickListener{
            imageFromGallery()
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

            //Todo: add Exif dialog & save final image

            Log.e("Aperture", ">> "+ exifData.getAttribute(ExifInterface.TAG_APERTURE_VALUE))
            Log.e("DateTime", ">> "+ exifData.getAttribute(ExifInterface.TAG_DATETIME))
            Log.e("Exposure", ">> "+ exifData.getAttribute(ExifInterface.TAG_EXPOSURE_INDEX))
            Log.e("Flash", ">> "+ exifData.getAttribute(ExifInterface.TAG_FLASH))
            Log.e("FileSource", ">> "+ exifData.getAttribute(ExifInterface.TAG_FILE_SOURCE))
            Log.e("F Number", ">> "+ exifData.getAttribute(ExifInterface.TAG_F_NUMBER))
            Log.e("Lat", ">> "+ exifData.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            Log.e("Lon", ">> "+ exifData.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
            Log.e("Length", ">> "+ exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
            Log.e("Width", ">> "+ exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))

        }

        imageCrop.setOnClickListener{
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

        imageAspect.setOnClickListener{

            resetSelections()
            imageAspect.setColorFilter(resources.getColor(R.color.orange, theme))

            val menu = PopupMenu(this, it)
            menu.inflate(R.menu.aspect_ratio_menu)
            menu.setForceShowIcon(true)

            menu.setOnMenuItemClickListener {

                when(it.itemId){

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

        imageRotate.setOnClickListener{

            if(this::croppedImg.isInitialized)
                imageCropView.cropRect = Rect(0, 0, croppedImg.width, croppedImg.height)
            else
                imageCropView.cropRect = Rect(0, 0, initWidth, initHeight)
            imageCropView.rotateImage(90)

            resetSelections()
            imageRotate.setColorFilter(resources.getColor(R.color.orange, theme))

            try {
                croppedImg = imageCropView.croppedImage!!
                undoList.add(croppedImg)
            }
            catch (e: Exception){
                e.printStackTrace()
            }

        }

        imageFlip.setOnClickListener{

            resetSelections()
            imageFlip.setColorFilter(resources.getColor(R.color.orange, theme))

            val menu = PopupMenu(this, it)
            menu.inflate(R.menu.flip_image_menu)
            menu.setForceShowIcon(true)

            menu.setOnMenuItemClickListener {

                when(it.itemId){

                    R.id.main_action_flip_horizontally -> {

                        try {
                            if(this::croppedImg.isInitialized) {
                                imageCropView.cropRect =
                                    Rect(0, 0, croppedImg.width, croppedImg.height)
                                croppedImg = createFlippedBitmap(croppedImg, true, false)!!
                            }
                            else {
                                imageCropView.cropRect = Rect(0, 0, initWidth, initHeight)
                                croppedImg = createFlippedBitmap(initImg, true, false)!!
                            }
//                            imageCropView.flipImageHorizontally()

                            imageCropView.setImageBitmap(croppedImg)
                            undoList.add(croppedImg)
                        }
                        catch (e: Exception){
                            e.printStackTrace()
                        }
                    }

                    R.id.main_action_flip_vertically -> {
                        try {
                            if(this::croppedImg.isInitialized) {
                                imageCropView.cropRect =
                                    Rect(0, 0, croppedImg.width, croppedImg.height)
                                croppedImg = createFlippedBitmap(croppedImg, false, true)!!
                            }
                            else {
                                imageCropView.cropRect = Rect(0, 0, initWidth, initHeight)
                                croppedImg = createFlippedBitmap(initImg, false, true)!!
                            }
//                            imageCropView.flipImageVertically()

                            imageCropView.setImageBitmap(croppedImg)
                            undoList.add(croppedImg)
                        }
                        catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }

                false
            }

            menu.show()
        }

        imageDone.setOnClickListener{

            if(this::croppedImg.isInitialized){

            }
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
        imageRotate.colorFilter = null
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

}