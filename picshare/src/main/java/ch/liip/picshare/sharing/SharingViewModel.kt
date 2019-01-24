package ch.liip.picshare.sharing

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ch.liip.picshare.sharing.sharing.options.SharingOptions

internal class SharingViewModel : ViewModel() {

    var sharingOptions = SharingOptions()

    val startCrop = MutableLiveData<Uri>()
    val startInlay = MutableLiveData<Uri>()
    val startPreview = MutableLiveData<Uri>()
    val shareNow = MutableLiveData<Uri>()


    fun setFromBundle(newBundle: Bundle){
        sharingOptions = SharingOptions(newBundle)
    }

    fun pictureSelectionCompleted(uri: Uri) {
        when {
            sharingOptions.isCropEnabled -> startCrop.value = uri
            sharingOptions.isInlayEnabled -> startInlay.value = uri
            sharingOptions.isPreviewEnabled -> startPreview.value = uri
            else -> shareNow.value = uri
        }
    }

    fun cropCompleted(uri: Uri) {
        when {
            sharingOptions.isInlayEnabled -> startInlay.value = uri
            sharingOptions.isPreviewEnabled -> startPreview.value = uri
            else -> shareNow.value = uri
        }
    }

    fun inlayCompleted(uri: Uri) {
        if(sharingOptions.isPreviewEnabled) {
            startPreview.value = uri
        }
        else {
            shareNow.value = uri
        }
    }
}