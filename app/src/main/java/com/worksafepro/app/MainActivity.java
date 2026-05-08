package com.worksafepro.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;

                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                gallery.setType("image/*");

                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraImageUri = createImageUri();
                if (cameraImageUri != null) {
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                    camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                Intent chooser = Intent.createChooser(gallery, "Selecionar evidência fotográfica");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
                startActivityForResult(chooser, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/app-full-dark.html");
    }

    private Uri createImageUri() {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "worksafepro_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && filePathCallback != null) {
            Uri[] out = null;
            if (resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
                else if (cameraImageUri != null) out = new Uri[]{cameraImageUri};
            }
            filePathCallback.onReceiveValue(out);
            filePathCallback = null;
            cameraImageUri = null;
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void printHtml(final String html, final String jobName) {
            runOnUiThread(new Runnable() { @Override public void run() {
                createPdf(html, cleanName(jobName), false);
            }});
        }

        @JavascriptInterface
        public void shareHtml(final String html) {
            runOnUiThread(new Runnable() { @Override public void run() {
                createPdf(html, "WorkSafePro_PSe_" + System.currentTimeMillis(), true);
            }});
        }
    }

    private String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) return "WorkSafePro_PSe_" + System.currentTimeMillis();
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void createPdf(final String html, final String fileName, final boolean share) {
        final WebView v = new WebView(this);
        v.getSettings().setJavaScriptEnabled(true);
        v.getSettings().setDomStorageEnabled(true);
        v.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                view.postDelayed(new Runnable() { @Override public void run() {
                    try {
                        Uri uri = saveWebViewAsPdf(v, fileName);
                        if (uri == null) {
                            Toast.makeText(MainActivity.this, "Não foi possível gerar o PDF", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (share) sharePdf(uri);
                        else Toast.makeText(MainActivity.this, "PDF salvo em Downloads/WorkSafePro", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Erro ao gerar PDF", Toast.LENGTH_LONG).show();
                    }
                }}, 900);
            }
        });
        v.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
    }

    private Uri saveWebViewAsPdf(WebView v, String fileName) throws Exception {
        int webWidth = 1080;
        int pdfWidth = 595;
        int pdfHeight = 842;
        int ws = android.view.View.MeasureSpec.makeMeasureSpec(webWidth, android.view.View.MeasureSpec.EXACTLY);
        int hs = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);
        v.measure(ws, hs);
        int contentHeight = Math.max(v.getMeasuredHeight(), (int)(v.getContentHeight() * v.getScale()));
        if (contentHeight < 1400) contentHeight = 1400;
        v.layout(0, 0, webWidth, contentHeight);

        float scale = pdfWidth / (float) webWidth;
        int pageHeightPx = (int)(pdfHeight / scale);
        PdfDocument doc = new PdfDocument();
        int page = 1;
        for (int y = 0; y < contentHeight; y += pageHeightPx) {
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, page++).create();
            PdfDocument.Page p = doc.startPage(info);
            Canvas c = p.getCanvas();
            c.scale(scale, scale);
            c.translate(0, -y);
            v.draw(c);
            doc.finishPage(p);
        }

        String finalName = fileName.endsWith(".pdf") ? fileName : fileName + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, finalName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WorkSafePro");
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        } else {
            uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        }
        if (uri == null) { doc.close(); return null; }
        OutputStream os = getContentResolver().openOutputStream(uri);
        if (os == null) { doc.close(); return null; }
        doc.writeTo(os);
        os.close();
        doc.close();
        return uri;
    }

    private void sharePdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartilhar PDF da PSe"));
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
