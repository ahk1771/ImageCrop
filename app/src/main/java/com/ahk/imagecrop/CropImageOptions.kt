package com.ahk.imagecrop

import com.canhub.cropper.CropImageView


object CropImageOptions {
    var scaleType: CropImageView.ScaleType = CropImageView.ScaleType.CENTER_INSIDE
    var cropShape: CropImageView.CropShape = CropImageView.CropShape.RECTANGLE
    var cornerShape: CropImageView.CropCornerShape = CropImageView.CropCornerShape.RECTANGLE
    var guidelines: CropImageView.Guidelines = CropImageView.Guidelines.ON_TOUCH
    var ratio: Pair<Int, Int>? = Pair(1, 1)
    var maxZoomLvl: Int = 2
    var fixAspectRatio: Boolean = true
    var autoZoom: Boolean = false
    var multiTouch: Boolean = true
    var centerMove: Boolean = true
    var showCropOverlay: Boolean = true
    var showProgressBar: Boolean = true
    var flipHorizontally: Boolean = false
    var flipVertically: Boolean = false
    var showCropLabel: Boolean = false
}
