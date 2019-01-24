package ch.liip.picshare.sharing.sharing.options

import android.os.Bundle

internal class SharingOptions : BaseOptions {

    private val cropKey = "$BaseKey.crop_options"

    private val inlayKey = "$BaseKey.inlay_options"

    private val previewKey = "$BaseKey.preview_options"

    val isCropEnabled = optionBundle.containsKey(cropKey)

    val isInlayEnabled = optionBundle.containsKey(inlayKey)

    val isPreviewEnabled = optionBundle.containsKey(previewKey)

    private val maxSizeXKey = "$BaseKey.max_size_x_key"
    var maxSizeX: Int
        get() = optionBundle.getInt(maxSizeXKey)
        private set(value) = optionBundle.putInt(maxSizeXKey, value)

    private val maxSizeYKey = "$BaseKey.max_size_y_key"
    var maxSizeY: Int
        get() = optionBundle.getInt(maxSizeYKey)
        private set(value) = optionBundle.putInt(maxSizeYKey, value)

    constructor() : super()
    constructor(bundle: Bundle) : super(bundle)

    fun setCrop(options: CropOptions?): SharingOptions {
        options?.let {optionBundle.putBundle(cropKey, options.bundle())}
        return this
    }

    fun setInlay(options: InlayOptions?): SharingOptions  {
        options?.let {optionBundle.putBundle(inlayKey, options.bundle())}
        return this
    }

    fun setPreview(options: PreviewOptions?): SharingOptions  {
        options?.let {optionBundle.putBundle(previewKey, options.bundle())}
        return this
    }

    fun setMaxSize(x: Int, y: Int): SharingOptions {
        maxSizeX = x
        maxSizeY = y
        return this
    }

    fun cropOption(): CropOptions = CropOptions(optionBundle.getBundle(cropKey)!!)

    fun inlayOption(): InlayOptions = InlayOptions(optionBundle.getBundle(inlayKey)!!)

    fun previewOption(): PreviewOptions = PreviewOptions(optionBundle.getBundle(previewKey)!!)
}