package com.securefilemanager.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PDF viewer built on the platform {@link PdfRenderer}. Replaces the unmaintained
 * barteksc AndroidPdfViewer (jcenter-only, pdfium .so not 16 KB page aligned).
 */
public class PdfActivity extends AppCompatActivity {
    private static final String TAG = "PdfActivity";

    private final ExecutorService renderer = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<Integer, Bitmap> cache = new LruCache<Integer, Bitmap>(6) {
    };
    private ParcelFileDescriptor fd;
    private PdfRenderer pdf;
    private String pdfFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        String path = getIntent().getStringExtra("paths");
        if (path == null) {
            finish();
            return;
        }
        pdfFileName = path.substring(path.lastIndexOf('/') + 1);
        try {
            fd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            pdf = new PdfRenderer(fd);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open PDF", e);
            Toast.makeText(this, R.string.unknown_error_occurred, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setTitle(String.format("%s (%s)", pdfFileName, pdf.getPageCount()));
        RecyclerView pages = findViewById(R.id.pdfView);
        pages.setLayoutManager(new LinearLayoutManager(this));
        pages.setAdapter(new PageAdapter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        renderer.execute(() -> {
            try {
                if (pdf != null) pdf.close();
                if (fd != null) fd.close();
            } catch (Exception ignored) {
            }
        });
        renderer.shutdown();
    }

    /** Renders one page at screen width; PdfRenderer allows one open page at a time (single thread). */
    private Bitmap render(int index, int width) {
        Bitmap cached = cache.get(index);
        if (cached != null) return cached;
        synchronized (this) {
            if (pdf == null) return null;
            try (PdfRenderer.Page page = pdf.openPage(index)) {
                int w = Math.max(1, width);
                int h = Math.max(1, (int) ((long) w * page.getHeight() / Math.max(1, page.getWidth())));
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                cache.put(index, bmp);
                return bmp;
            } catch (Exception e) {
                Log.e(TAG, "render " + index, e);
                return null;
            }
        }
    }

    private class PageAdapter extends RecyclerView.Adapter<PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setAdjustViewBounds(true);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 8;
            iv.setLayoutParams(lp);
            return new PageHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            ImageView iv = (ImageView) holder.itemView;
            iv.setImageDrawable(null);
            iv.setTag(position);
            int width = getResources().getDisplayMetrics().widthPixels;
            renderer.execute(() -> {
                Bitmap bmp = render(position, width);
                main.post(() -> {
                    if (Integer.valueOf(position).equals(iv.getTag()) && bmp != null) {
                        iv.setImageBitmap(bmp);
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return pdf == null ? 0 : pdf.getPageCount();
        }
    }

    private static class PageHolder extends RecyclerView.ViewHolder {
        PageHolder(@NonNull ImageView v) {
            super(v);
        }
    }
}
