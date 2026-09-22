package com.joshfactory.qurantafseel

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.joshfactory.qurantafseel.data.PdfManifest
import com.joshfactory.qurantafseel.data.PdfPageRenderer
import com.joshfactory.qurantafseel.ui.BookFlipPageTransformer
import com.joshfactory.qurantafseel.ui.PageAdapter
import com.joshfactory.qurantafseel.ui.SurahListAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var renderer: PdfPageRenderer

    private val prefsName = "quran_flipbook_prefs"
    private val keyLastPage = "last_page"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PdfManifest.load(this)
        renderer = PdfPageRenderer(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = PageAdapter(PdfManifest.totalPages, renderer)
        viewPager.setPageTransformer(BookFlipPageTransformer())
        viewPager.offscreenPageLimit = 1

        // Rotated pages must not be clipped by ViewPager2's own bounds, or the
        // 3D flip looks chopped off at the edges instead of lifting cleanly.
        viewPager.clipChildren = false
        viewPager.clipToPadding = false
        (viewPager.getChildAt(0) as? RecyclerView)?.apply {
            clipChildren = false
            clipToPadding = false
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTitle(position)
                getSharedPreferences(prefsName, MODE_PRIVATE).edit()
                    .putInt(keyLastPage, position)
                    .apply()
            }
        })

        findViewById<FloatingActionButton>(R.id.btnSurahList).setOnClickListener {
            showSurahList()
        }

        val lastPage = getSharedPreferences(prefsName, MODE_PRIVATE)
            .getInt(keyLastPage, 0)
            .coerceIn(0, (PdfManifest.totalPages - 1).coerceAtLeast(0))
        viewPager.setCurrentItem(lastPage, false)
        updateTitle(lastPage)
    }

    private fun updateTitle(absolutePage: Int) {
        val surah = PdfManifest.surahForPage(absolutePage)
        title = if (surah != null) "${surah.surahNum}. ${surah.surahName}" else getString(R.string.app_name)
    }

    private fun showSurahList() {
        val dialog = BottomSheetDialog(this)
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SurahListAdapter(PdfManifest.surahs) { surah ->
            viewPager.setCurrentItem(surah.startPageIndex, false)
            dialog.dismiss()
        }
        dialog.setContentView(recyclerView)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.shutdown()
    }
}
