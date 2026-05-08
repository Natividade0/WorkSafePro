package com.worksafepro.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private val fileChooserCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                }
                val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraImageUri = createImageUri()
                cameraImageUri?.let {
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, it)
                    camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(gallery, "Selecionar evidência fotográfica")
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
                startActivityForResult(chooser, fileChooserCode)
                return true
            }
        }

        webView.loadUrl("file:///android_asset/app-full-dark.html")
    }

    private fun createImageUri(): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "worksafepro_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileChooserCode) {
            val result = if (resultCode == RESULT_OK) {
                data?.data?.let { arrayOf(it) } ?: cameraImageUri?.let { arrayOf(it) }
            } else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
            cameraImageUri = null
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun savePsePdf(json: String) {
            runOnUiThread {
                try {
                    val uri = generateNativePdf(JSONObject(json), false)
                    if (uri != null) {
                        Toast.makeText(this@MainActivity, "PDF salvo em Downloads/WorkSafePro", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun sharePsePdf(json: String) {
            runOnUiThread {
                try {
                    val uri = generateNativePdf(JSONObject(json), true)
                    if (uri != null) sharePdf(uri)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun printHtml(html: String, jobName: String) {
            Toast.makeText(this@MainActivity, "Use Salvar PDF atualizado", Toast.LENGTH_LONG).show()
        }

        @JavascriptInterface
        fun shareHtml(html: String) {
            Toast.makeText(this@MainActivity, "Use Compartilhar atualizado", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateNativePdf(data: JSONObject, share: Boolean): Uri? {
        val fileName = safeName("WorkSafePro_${data.optString("id", System.currentTimeMillis().toString())}.pdf")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 40f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 40f
        }

        fun ensure(space: Float) {
            if (y + space > pageHeight - 35) newPage()
        }

        fun text(txt: String, x: Float, size: Float = 10f, bold: Boolean = false) {
            paint.color = Color.rgb(13, 27, 46)
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val lines = wrap(txt, if (size >= 14f) 50 else 82)
            for (line in lines) {
                ensure(size + 8)
                canvas.drawText(line, x, y, paint)
                y += size + 5
            }
        }

        fun section(title: String) {
            ensure(34f)
            paint.color = Color.rgb(13, 27, 46)
            canvas.drawRect(30f, y, 565f, y + 24f, paint)
            paint.color = Color.rgb(0, 200, 150)
            canvas.drawRect(30f, y, 37f, y + 24f, paint)
            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(title, 45f, y + 16f, paint)
            y += 34f
        }

        fun line(label: String, value: String) {
            text("$label: $value", 35f, 10f, false)
        }

        paint.color = Color.rgb(13, 27, 46)
        canvas.drawRect(30f, 25f, 565f, 95f, paint)
        paint.color = Color.rgb(0, 200, 150)
        canvas.drawRect(30f, 25f, 38f, 95f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 20f
        canvas.drawText("WorkSafePro", 50f, 55f, paint)
        paint.textSize = 14f
        canvas.drawText("Permissão de Serviço Eletrônica", 50f, 78f, paint)
        paint.textSize = 10f
        canvas.drawText("ID: ${data.optString("id")}", 385f, 55f, paint)
        canvas.drawText("Aut.: ${data.optString("auth")}", 385f, 75f, paint)
        y = 120f

        section("1. Informações Gerais")
        line("Empresa", data.optString("empresa"))
        line("Início", data.optString("inicio"))
        line("Fim", data.optString("fim"))
        line("Autenticador", data.optString("autenticador"))
        line("Tipo", data.optString("tipo"))
        line("Condição", data.optString("condicao"))
        line("Área", data.optString("area"))
        line("Setor", data.optString("setor"))
        line("Local", data.optString("local"))
        line("Executantes", data.optString("executantesEmp"))
        line("Descrição", data.optString("descricao"))
        line("Observações", data.optString("obs"))

        section("6. Lista Geral de Verificação")
        val geral = data.optJSONArray("geral") ?: JSONArray()
        for (i in 0 until geral.length()) {
            val item = geral.optJSONObject(i) ?: JSONObject()
            text("[${item.optString("resp", "-")}] ${item.optString("q")}", 35f, 9.5f)
        }

        section("7. Riscos / Medidas Mitigadoras")
        val riscos = data.optJSONArray("riscos") ?: JSONArray()
        for (i in 0 until riscos.length()) {
            val r = riscos.optJSONObject(i) ?: JSONObject()
            text(r.optString("nome"), 35f, 10f, true)
            val controls = r.optJSONArray("controls") ?: JSONArray()
            val marked = mutableListOf<String>()
            for (j in 0 until controls.length()) {
                val c = controls.optJSONObject(j) ?: JSONObject()
                if (c.optBoolean("on")) marked.add(c.optString("c"))
            }
            text(if (marked.isEmpty()) "Medidas marcadas: --" else "Medidas marcadas: ${marked.joinToString(", ")}", 45f, 9.5f)
        }

        section("8. Emergência")
        val emerg = data.optJSONObject("emerg") ?: JSONObject()
        line("Telefone", emerg.optString("tel"))
        line("Ponto de encontro", emerg.optString("ponto"))
        line("Ambulância", emerg.optString("amb"))
        line("Rádio", emerg.optString("radio"))

        section("10. Declaração")
        line("Declaração", data.optString("decl"))

        section("11. Emissor")
        drawPerson(data.optJSONObject("emissor"), ::text, canvas, paint, ::ensure, ::addImage, yProvider = { y }, ySetter = { y = it })
        y += 15f

        section("12. Executor Líder")
        drawPerson(data.optJSONObject("lider"), ::text, canvas, paint, ::ensure, ::addImage, yProvider = { y }, ySetter = { y = it })
        y += 15f

        section("13. Trabalhadores Envolvidos")
        val trabs = data.optJSONArray("trabs") ?: JSONArray()
        for (i in 0 until trabs.length()) {
            drawPerson(trabs.optJSONObject(i), ::text, canvas, paint, ::ensure, ::addImage, yProvider = { y }, ySetter = { y = it })
            y += 12f
        }

        section("14. Evidência Fotográfica")
        line("Descrição", data.optString("evidencia"))
        val foto = data.optString("foto")
        if (foto.isNotBlank()) addImage(foto, 35f, 300f, 210f)

        document.finishPage(page)

        val uri = createPdfUri(fileName)
        if (uri == null) {
            document.close()
            return null
        }
        val out: OutputStream? = contentResolver.openOutputStream(uri)
        if (out == null) {
            document.close()
            return null
        }
        document.writeTo(out)
        out.close()
        document.close()
        return uri
    }

    private fun drawPerson(
        obj: JSONObject?,
        text: (String, Float, Float, Boolean) -> Unit,
        canvas: Canvas,
        paint: Paint,
        ensure: (Float) -> Unit,
        addImage: (String, Float, Float, Float) -> Unit,
        yProvider: () -> Float,
        ySetter: (Float) -> Unit
    ) {
        val p = obj ?: JSONObject()
        text("Nome: ${p.optString("nome")}", 35f, 10f, true)
        text("Função: ${p.optString("funcao")} | Matrícula: ${p.optString("mat")}", 35f, 9.5f, false)
        val sig = p.optString("sig")
        if (sig.isNotBlank()) {
            addImage(sig, 35f, 180f, 55f)
            text("Assinado em: ${p.optString("dt")}", 35f, 8f, false)
        }
    }

    private fun wrap(text: String, max: Int): List<String> {
        val words = text.replace("\n", " ").split(" ")
        val out = mutableListOf<String>()
        var line = ""
        for (w in words) {
            if ((line + " " + w).trim().length > max) {
                if (line.isNotBlank()) out.add(line)
                line = w
            } else line = (line + " " + w).trim()
        }
        if (line.isNotBlank()) out.add(line)
        return if (out.isEmpty()) listOf("") else out
    }

    private fun addImage(dataUrl: String, x: Float, maxW: Float, maxH: Float) {
        try {
            val clean = dataUrl.substringAfter(",", dataUrl)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            // This function is rebound by closure in generateNativePdf using current canvas/y via fields is not needed there.
        } catch (_: Exception) {}
    }

    private fun createPdfUri(fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WorkSafePro")
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        }
    }

    private fun sharePdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar PDF da PSe"))
    }

    private fun safeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_.-]"), "_")

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
