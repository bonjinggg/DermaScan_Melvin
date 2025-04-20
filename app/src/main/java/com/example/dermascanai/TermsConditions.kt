package com.example.dermascanai

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import com.example.dermascanai.databinding.ActivityTermsConditionsBinding

class TermsConditions : AppCompatActivity() {

    private lateinit var binding: ActivityTermsConditionsBinding

    private lateinit var pdfContainer: LinearLayout
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var fileDescriptor: ParcelFileDescriptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBTN.setOnClickListener {
            finish()
        }

        pdfContainer = findViewById(R.id.pdfContainer)

        val file = File(cacheDir, "dermascan_terms.pdf")
        if (!file.exists()) {
            val asset = assets.open("dermascan_terms.pdf")
            val output = FileOutputStream(file)
            asset.copyTo(output)
            asset.close()
            output.close()
        }

        // Open the PDF
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)

        for (i in 0 until pdfRenderer.pageCount) {
            val page = pdfRenderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val imageView = ImageView(this)
            imageView.setImageBitmap(bitmap)
            imageView.adjustViewBounds = true
            pdfContainer.addView(imageView)

            page.close()
        }

        pdfRenderer.close()
        fileDescriptor.close()
    }
}