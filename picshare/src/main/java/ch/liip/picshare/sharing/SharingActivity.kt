package ch.liip.picshare.sharing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ch.liip.picshare.R
import ch.liip.picshare.exceptions.InvalidOptionException
import ch.liip.picshare.helpers.*
import ch.liip.picshare.inlaying.InlayBitmap
import ch.liip.picshare.screens.PreviewActivity
import ch.liip.picshare.sharing.sharing.options.*
import com.yalantis.ucrop.UCrop
import kotlin.math.min


internal class SharingActivity : AppCompatActivity() {

    private lateinit var viewModel: SharingViewModel

    private val PICK_IMAGE_REQUEST_KEY = 0
    private val CROP_IMAGE_REQUEST_KEY = 1
    private val PREVIEW_IMAGE_REQUEST_KEY = 2
    private val SHARING_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createEmptyCacheFile(this)

        viewModel = ViewModelProviders.of(this).get(SharingViewModel::class.java)

        intent.extras?.let { viewModel.setFromBundle(it) }


        bindViewModel()

        startImageSelection()
    }

    private fun bindViewModel() {
        viewModel.startCrop.observe(this, Observer { uri ->
            startCrop(uri!!, viewModel.sharingOptions.cropOption())
        })

        viewModel.startInlay.observe(this, Observer { uri ->
            startInlay(uri!!, viewModel.sharingOptions.inlayOption())
        })

        viewModel.startPreview.observe(this, Observer { uri ->
            startPreview(uri!!, viewModel.sharingOptions.previewOption())
        })

        viewModel.shareNow.observe(this, Observer { uri ->
            shareNow(uri!!)
        })
    }

    private fun startImageSelection() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGE_REQUEST_KEY)
    }

    private fun startCrop(uri: Uri, cropOptions: CropOptions) {


        val color1 = primaryColor(resources)
        val color2 = secondaryColor(resources)
        val color3 = textColor(resources)

        val options = UCrop.Options().apply {

            setToolbarTitle(if (cropOptions.hasTitle) cropOptions.title else null)
            setActiveWidgetColor(color1)
            setToolbarColor(color1)
            setStatusBarColor(color1)
            setCropFrameColor(color2)
            setCropGridColor(color2)
            setToolbarWidgetColor(color3)

            setHideBottomControls(true)
            setFreeStyleCropEnabled(!cropOptions.hasFixedAssetRatio())
        }


        val crop = UCrop.of(uri, localImageUri(this))
                .withOptions(options)

        if (cropOptions.hasFixedAssetRatio()) {
            crop.withAspectRatio(1f, cropOptions.aspectRatio)
        }

        if (cropOptions.hasFixedSize()) {
            crop.withMaxResultSize(cropOptions.fixedSizeX, cropOptions.fixedSizeY)
        }

        crop.start(this, CROP_IMAGE_REQUEST_KEY)
    }

    private fun startInlay(sourceUri: Uri, inlayOptions: InlayOptions) {
        val destinationUri = localImageUri(this)
        inlay(sourceUri, destinationUri, inlayOptions)
        viewModel.inlayCompleted(destinationUri)
    }

    private fun startPreview(uri: Uri, previewOptions: PreviewOptions) {
        val sharingPreviewIntent = Intent(this, PreviewActivity::class.java)
        sharingPreviewIntent.putExtras(previewOptions.bundle())
        sharingPreviewIntent.putExtra(ImageUriKey, uri)

        startActivityForResult(sharingPreviewIntent, PREVIEW_IMAGE_REQUEST_KEY)
    }

    private fun shareNow(uri: Uri) {
        openSharingPanel(this, uri, "", "", SHARING_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            finish()
            return
        }

        when (requestCode) {
            PICK_IMAGE_REQUEST_KEY -> {
                val selectedUri = data?.data!!

                var destUri = localImageUri(this)
                scaleImageToFit(viewModel.sharingOptions.maxSizeX, viewModel.sharingOptions.maxSizeY, selectedUri, destUri)

                viewModel.pictureSelectionCompleted(destUri)
            }

            CROP_IMAGE_REQUEST_KEY -> {
                val uri = localImageUri(this)
                val cropOptions = viewModel.sharingOptions.cropOption()
                if (cropOptions.hasFixedSize()) {
                    scaleImage(cropOptions.fixedSizeX, cropOptions.fixedSizeY, uri, uri)
                }

                viewModel.cropCompleted(localImageUri(this))
            }

            PREVIEW_IMAGE_REQUEST_KEY -> {
                finish()
            }

            SHARING_REQUEST_CODE -> {
                // Warning, this might not be called, as sharing usually returns a Result_cancel status
                // which would be caught by the RESULT_OK test above
                finish()
            }
        }
    }

    private fun scaleImageToFit(x: Int, y: Int, srcUri: Uri, destUri: Uri) {
        val bitmap = load(contentResolver, srcUri, false)

        val xRatio = x.toFloat() / bitmap.width.toFloat()
        val yRatio = y.toFloat() / bitmap.height.toFloat()
        val ratio = MathUtils.clamp(min(xRatio, yRatio), 0f, 1f) // Clamp because we only want to scale down

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (ratio * bitmap.width).toInt(), (ratio * bitmap.height).toInt(), false)
        save(scaledBitmap, destUri)

        bitmap.recycle()
        scaledBitmap.recycle()
    }

    private fun scaleImage(x: Int, y: Int, srcUri: Uri, destUri: Uri) {
        val bitmap = load(contentResolver, srcUri, false)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, x, y, false)
        save(scaledBitmap, destUri)

        bitmap.recycle()
        scaledBitmap.recycle()
    }

    private fun inlay(sourceUri: Uri, destUri: Uri, inlayOptions: InlayOptions): Boolean {

        val mutableBitmap = load(contentResolver, sourceUri, true)

        //InlayBitmap.withText("DEMO Text", scaledMutableBitmap)

        val provider = inlayOptions.inlayViewProvider
        val view = layoutInflater.inflate(provider.viewResourceId, null)
        provider.populate(view)
        InlayBitmap.withView(mutableBitmap, view)

        val success = save(mutableBitmap, destUri)

        mutableBitmap.recycle()

        return success
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyCacheFile(this)
    }
}


internal fun openSharingPanel(activity: Activity, imageUri: Uri, imageTitle: String, appChooserTitle: String, requestCode: Int) {

    // Only uri of type content:// can be shared across apps.
    // Test the Uri and attempt to convert if needed

    val sharableImageUri: Uri = if (imageUri.toString().startsWith("content://"))
        imageUri
    else
        sharableUri(activity, imageUri)

    val intent = ShareCompat.IntentBuilder.from(activity)
            .setType("image/*")
            .setSubject(imageTitle)
            .setStream(sharableImageUri)
            .setChooserTitle(appChooserTitle)
            .createChooserIntent()

    activity.startActivityForResult(intent, requestCode)
}

fun startSharing(context: Context, maxSizeX: Int, maxSizeY: Int, cropOptions: CropOptions? = null, inlayOptions: InlayOptions? = null, previewOptions: PreviewOptions? = null) {
    val imageSharingIntent = Intent(context, SharingActivity::class.java)

    if(cropOptions != null && cropOptions.hasFixedSize()
    && (cropOptions.fixedSizeX > maxSizeX || cropOptions.fixedSizeY > maxSizeY)) {
        throw InvalidOptionException("Max size can't be smaller than crop size")
    }

    val options = SharingOptions()
            .setCrop(cropOptions)
            .setInlay(inlayOptions)
            .setPreview(previewOptions)
            .setMaxSize(maxSizeX, maxSizeY)

    imageSharingIntent.putExtras(options.bundle())

    context.startActivity(imageSharingIntent)
}

private fun primaryColor(resources: Resources) = ResourcesCompat.getColor(resources, R.color.primary_color, null)
private fun secondaryColor(resources: Resources) = ResourcesCompat.getColor(resources, R.color.secondary_color, null)
private fun textColor(resources: Resources) = ResourcesCompat.getColor(resources, R.color.text_color, null)
