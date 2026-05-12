package com.worksafepro.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
                val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraImageUri = createImageUri()
                cameraImageUri?.let {
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, it)
                    camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(gallery, "Selecionar evidencia fotografica")
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
            val result = if (resultCode == RESULT_OK) data?.data?.let { arrayOf(it) } ?: cameraImageUri?.let { arrayOf(it) } else null
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
                    val uri = generateNativePdf(JSONObject(json))
                    if (uri != null) Toast.makeText(this@MainActivity, "PDF salvo em Downloads/WorkSafePro", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this@MainActivity, "Nao foi possivel salvar o PDF", Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Toast.makeText(this@MainActivity, "Erro ao salvar PDF", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun sharePsePdf(json: String) {
            runOnUiThread {
                try {
                    val uri = generateNativePdf(JSONObject(json))
                    if (uri != null) sharePdf(uri) else Toast.makeText(this@MainActivity, "Nao foi possivel gerar o PDF", Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Toast.makeText(this@MainActivity, "Erro ao compartilhar PDF", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface fun printHtml(html: String, jobName: String) { Toast.makeText(this@MainActivity, "Use Salvar PDF", Toast.LENGTH_LONG).show() }
        @JavascriptInterface fun shareHtml(html: String) { Toast.makeText(this@MainActivity, "Use Compartilhar", Toast.LENGTH_LONG).show() }
    }

    private fun generateNativePdf(data: JSONObject): Uri? {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentW = pageWidth - margin * 2
        val dark = Color.rgb(13, 27, 46)
        val green = Color.rgb(0, 200, 150)
        val blue = Color.rgb(47, 140, 255)
        val box = Color.rgb(244, 249, 252)
        val line = Color.rgb(197, 211, 225)
        val text = Color.rgb(22, 34, 43)
        val muted = Color.rgb(91, 107, 124)
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun rect(x: Float, yy: Float, w: Float, h: Float, color: Int, radius: Float = 0f) {
            val c = canvas ?: return
            paint.style = Paint.Style.FILL
            paint.color = color
            val r = RectF(x, yy, x + w, yy + h)
            if (radius > 0f) c.drawRoundRect(r, radius, radius, paint) else c.drawRect(r, paint)
        }

        fun stroke(x: Float, yy: Float, w: Float, h: Float, color: Int, radius: Float = 0f) {
            val c = canvas ?: return
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            paint.color = color
            val r = RectF(x, yy, x + w, yy + h)
            if (radius > 0f) c.drawRoundRect(r, radius, radius, paint) else c.drawRect(r, paint)
            paint.style = Paint.Style.FILL
        }

        fun header() {
            val c = canvas ?: return
            rect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), Color.WHITE)
            rect(0f, 0f, pageWidth.toFloat(), 88f, dark)
            rect(0f, 0f, 9f, 88f, green)
            rect(pageWidth - 9f, 0f, 9f, 88f, blue)
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 23f
            c.drawText("WorkSafePro", margin, 35f, paint)
            paint.textSize = 13f
            c.drawText("Permissao de Servico Eletronica - PSe", margin, 59f, paint)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.RIGHT
            c.drawText("ID: ${data.optString("id", "--")}", pageWidth - margin, 32f, paint)
            c.drawText("Aut.: ${data.optString("auth", "--")}", pageWidth - margin, 49f, paint)
            c.drawText("Status: ${data.optString("condicao", "--")}", pageWidth - margin, 66f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        fun footer() {
            val c = canvas ?: return
            paint.color = muted
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
            c.drawText("Gerado pelo WorkSafePro em $now", margin, pageHeight - 18f, paint)
            paint.textAlign = Paint.Align.RIGHT
            c.drawText("Pagina $pageNo", pageWidth - margin, pageHeight - 18f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        fun startPage() {
            pageNo++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
            canvas = page!!.canvas
            header()
            y = 112f
        }

        fun finishPage() {
            page?.let {
                footer()
                document.finishPage(it)
            }
        }

        fun newPage() { finishPage(); startPage() }
        fun ensure(space: Float) { if (y + space > pageHeight - 58f) newPage() }

        fun wrap(raw: String, max: Int): List<String> {
            val words = raw.replace("\n", " ").split(" ").filter { it.isNotBlank() }
            val out = mutableListOf<String>()
            var current = ""
            for (w in words) {
                val next = if (current.isBlank()) w else "$current $w"
                if (next.length > max) { if (current.isNotBlank()) out.add(current); current = w } else current = next
            }
            if (current.isNotBlank()) out.add(current)
            return if (out.isEmpty()) listOf("--") else out
        }

        fun section(title: String) {
            ensure(42f)
            rect(margin, y, contentW, 28f, dark, 8f)
            rect(margin, y, 7f, 28f, green, 8f)
            val c = canvas ?: return
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 12f
            c.drawText(title, margin + 16f, y + 18f, paint)
            y += 40f
        }

        fun fullBox(label: String, value: String, minH: Float = 52f) {
            val c = canvas ?: return
            val lines = wrap(value.ifBlank { "--" }, 88)
            val h = maxOf(minH, 28f + lines.size * 12f)
            ensure(h + 9f)
            rect(margin, y, contentW, h, box, 7f)
            stroke(margin, y, contentW, h, line, 7f)
            paint.color = muted
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 7.8f
            c.drawText(label.uppercase(Locale("pt", "BR")), margin + 8f, y + 13f, paint)
            paint.color = text
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            var ly = y + 29f
            for (ln in lines) { c.drawText(ln, margin + 8f, ly, paint); ly += 12f }
            y += h + 9f
        }

        fun three(a: Pair<String, String>, b: Pair<String, String>, cPair: Pair<String, String>) {
            ensure(60f)
            val gap = 8f
            val w = (contentW - gap * 2) / 3f
            val items = listOf(a, b, cPair)
            for (i in items.indices) {
                val x = margin + i * (w + gap)
                rect(x, y, w, 48f, box, 7f)
                stroke(x, y, w, 48f, line, 7f)
                val cnv = canvas ?: continue
                paint.color = muted
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = 7.5f
                cnv.drawText(items[i].first.uppercase(Locale("pt", "BR")), x + 8f, y + 13f, paint)
                paint.color = text
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                val lines = wrap(items[i].second.ifBlank { "--" }, 20).take(2)
                var ly = y + 29f
                for (ln in lines) { cnv.drawText(ln, x + 8f, ly, paint); ly += 11f }
            }
            y += 60f
        }

        fun checklist(q: String, resp: String) {
            val c = canvas ?: return
            val lines = wrap(q, 67)
            val h = maxOf(34f, 16f + lines.size * 12f)
            ensure(h + 6f)
            rect(margin, y, contentW, h, Color.rgb(248, 251, 253), 6f)
            stroke(margin, y, contentW, h, line, 6f)
            paint.color = text
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            var ly = y + 15f
            for (ln in lines) { c.drawText(ln, margin + 8f, ly, paint); ly += 12f }
            val color = when(resp) { "S" -> green; "N" -> Color.rgb(255,76,76); "NA" -> blue; else -> muted }
            rect(pageWidth - margin - 58f, y + 8f, 48f, 18f, color, 9f)
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 8.5f
            c.drawText(if (resp.isBlank()) "--" else resp, pageWidth - margin - 39f, y + 20.5f, paint)
            y += h + 6f
        }

        fun person(title: String, obj: JSONObject?) {
            val p = obj ?: JSONObject()
            val signed = p.optString("sig").isNotBlank()
            val txt = "Nome: ${p.optString("nome", "--")}\nFuncao: ${p.optString("funcao", "--")} | Matricula: ${p.optString("mat", "--")}\nAssinatura digital: ${if (signed) "registrada" else "nao registrada"}${if (p.optString("dt").isNotBlank()) " | ${p.optString("dt")}" else ""}"
            fullBox(title, txt, 64f)
        }

        startPage()
        section("1. Informacoes Gerais")
        three("Empresa" to data.optString("empresa"), "Inicio" to data.optString("inicio"), "Fim" to data.optString("fim"))
        three("Autenticador" to data.optString("autenticador"), "Tipo" to data.optString("tipo"), "Condicao" to data.optString("condicao"))
        three("Area" to data.optString("area"), "Setor" to data.optString("setor"), "Local" to data.optString("local"))
        fullBox("Descricao detalhada da atividade", data.optString("descricao"), 58f)
        fullBox("Empresas executantes", data.optString("executantesEmp"), 48f)
        fullBox("Observacoes", data.optString("obs"), 46f)

        section("2. Atencao")
        fullBox("Condicao de suspensao", "Emergencia, abandono de area ou mudanca nas condicoes de trabalho que altere as medidas de controle previstas suspende esta permissao.", 58f)

        section("6. Lista Geral de Verificacao")
        val geral = data.optJSONArray("geral") ?: JSONArray()
        for (i in 0 until geral.length()) {
            val item = geral.optJSONObject(i) ?: JSONObject()
            checklist(item.optString("q"), item.optString("resp"))
        }

        section("7. Identificacao e Controle de Riscos / Medidas Mitigadoras")
        val riscos = data.optJSONArray("riscos") ?: JSONArray()
        if (riscos.length() == 0) fullBox("Riscos", "Nenhum risco informado.", 44f)
        for (i in 0 until riscos.length()) {
            val r = riscos.optJSONObject(i) ?: JSONObject()
            val controls = r.optJSONArray("controls") ?: JSONArray()
            val marked = mutableListOf<String>()
            for (j in 0 until controls.length()) {
                val item = controls.optJSONObject(j) ?: JSONObject()
                if (item.optBoolean("on")) marked.add(item.optString("c"))
            }
            fullBox(r.optString("nome", "Risco"), if (marked.isEmpty()) "Medidas marcadas: --" else "Medidas marcadas: ${marked.joinToString(", ")}", 48f)
        }

        section("8. Emergencia")
        val emerg = data.optJSONObject("emerg") ?: JSONObject()
        three("Telefone" to emerg.optString("tel"), "Ponto encontro" to emerg.optString("ponto"), "Radio" to emerg.optString("radio"))
        fullBox("Parada de ambulancia", emerg.optString("amb"), 44f)

        section("10. Declaracao")
        fullBox("Declaracao dos envolvidos", data.optString("decl"), 72f)

        section("11. Emissor")
        person("Emissor", data.optJSONObject("emissor"))
        section("12. Executor Lider")
        person("Executor Lider", data.optJSONObject("lider"))
        section("13. Trabalhadores Envolvidos")
        val trabs = data.optJSONArray("trabs") ?: JSONArray()
        if (trabs.length() == 0) fullBox("Trabalhadores", "Nenhum trabalhador informado.", 44f)
        for (i in 0 until trabs.length()) person("Trabalhador ${i + 1}", trabs.optJSONObject(i))

        section("14. Evidencia Fotografica")
        fullBox("Descricao da evidencia", data.optString("evidencia"), 52f)
        fullBox("Foto", if (data.optString("foto").isNotBlank()) "Foto registrada no aplicativo. Imagem nao incluida neste PDF para evitar falha de memoria." else "Nenhuma foto anexada.", 44f)

        finishPage()
        val fileName = safeName("WorkSafePro_${data.optString("id", System.currentTimeMillis().toString())}.pdf")
        val uri = createPdfUri(fileName)
        if (uri == null) { document.close(); return null }
        val out: OutputStream? = contentResolver.openOutputStream(uri)
        if (out == null) { document.close(); return null }
        document.writeTo(out)
        out.close()
        document.close()
        return uri
    }

    private fun createPdfUri(fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WorkSafePro")
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) else contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
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
    override fun onBackPressed() { if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed() }
}
