package ch.liip.picshare.inlaying


import android.view.View
import java.io.Serializable

open class InlayViewProvider(val viewResourceId: Int) : Serializable {

    open fun populate(view: View) {

    }
}