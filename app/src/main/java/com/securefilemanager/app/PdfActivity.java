package com.securefilemanager.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.List;

public class PdfActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    String TAG = "PdfActivity";
    String position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        init();
    }

    private void init() {
        pdfView = (PDFView) findViewById(R.id.pdfView);
        position = getIntent().getStringExtra("paths");
        displayFromSdcard();
    }

    private void displayFromSdcard() {
        pdfFileName = getIntent().getStringExtra("paths");
        pdfFileName = pdfFileName.substring(pdfFileName.lastIndexOf("/") + 1, pdfFileName.length());

        pdfView.fromFile(new File(position))
            .defaultPage(pageNumber)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(new DefaultScrollHandle(this))
            .load();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }
}
