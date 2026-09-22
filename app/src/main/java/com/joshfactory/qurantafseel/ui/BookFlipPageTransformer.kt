package com.joshfactory.qurantafseel.ui

import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * Gives ViewPager2 a genuine 3D "turning page" look instead of a flat slide:
 * each page rotates around its vertical spine edge (left edge for pages
 * coming from the right, right edge for pages leaving to the left) using
 * rotationY, with a large cameraDistance so the perspective looks like a
 * page lifting off a book rather than squashing flat.
 *
 * This intentionally avoids a third-party OpenGL "page curl" library:
 * those exist (e.g. eschao/android-PageFlip) but require hand-written GL
 * rendering/touch-event glue and have a history of broken JitPack
 * resolution, which is a bad trade-off for something nobody can test-build
 * here. This transformer uses only stable, built-in Android APIs.
 */
class BookFlipPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val density = page.resources.displayMetrics.density
        page.cameraDistance = 14000f * density

        when {
            position < -1f || position > 1f -> {
                page.alpha = 0f
            }
            else -> {
                page.alpha = 1f
                page.pivotY = page.height * 0.5f
                page.pivotX = if (position < 0f) page.width.toFloat() else 0f
                page.rotationY = 180f * position
                page.translationX = 0f
            }
        }
    }
}
