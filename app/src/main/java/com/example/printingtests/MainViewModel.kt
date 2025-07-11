package com.example.printingtests

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.annotation.IntRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.comm.TcpConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import qrcode.QRCode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.Socket

class MainViewModel : ViewModel() {

    val _pdfFileUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val pdfFileUri: StateFlow<Uri?> = _pdfFileUri

    private val _isPdfGenerated = MutableStateFlow(false)
    val isPdfGenerated: StateFlow<Boolean>
        get() = _isPdfGenerated
    private val _isSentToPrinter = MutableStateFlow(false)
    val isSentToPrinter: StateFlow<Boolean>
        get() = _isSentToPrinter

    fun sendZPL(printerIp: String,
                qrCodeContent: String,
                ) {

        Log.i("Print Test", "Attempting to send ZPL bytes...")

        viewModelScope.launch(context = Dispatchers.IO) {
            // Instantiate connection for ZPL TCP port at given address
            val thePrinterConn = TcpConnection(printerIp, TcpConnection.DEFAULT_ZPL_TCP_PORT)

            try {
                // Open the connection - physical connection is established here.
                thePrinterConn.open()

                // This example prints "This is a ZPL test." near the top of the label.
                val zplData =
                // Send QR Code
                """
                ^^XA
                ^PW575
                ^LL400

                ^FO0,40
                ^FB575,1,0,C,0
                ^A0N,40,40
                ^FDThis is a QR code test^FS

                ^FO224,100           
                ^BQN,2,6
                ^FDLA,$qrCodeContent^FS

                ^XZ
                """.trimIndent()

                // Send the data to printer as a byte array.
                thePrinterConn.write(zplData.toByteArray())
                Log.i("Print Test", "Sent ZPL bytes to printer")
            } catch (e: ConnectionException) {
                // Handle communications error here.
                e.printStackTrace()
            } finally {
                // Close the connection to release resources.
                thePrinterConn.close()
            }
        }
    }


    fun sendPdfToPrinter(
        contentResolver: ContentResolver,
        pdfUri: Uri, // IGNORED!
        printerIp: String,
        port: Int = 9100 // Default RAW port
    ) {

        viewModelScope.launch(context = Dispatchers.IO) {
            try {
                Socket(printerIp, port).use { socket ->

                    socket.getOutputStream().use { outStream ->

                        // Use generated PDF or just the requested one
                        val finalPdf = _pdfFileUri.value ?: pdfUri
                        contentResolver.openInputStream(finalPdf)?.use { inStream ->
                            val buffer = ByteArray(4096)
                            var bytesRead: Int
                            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                                outStream.write(buffer, 0, bytesRead)
                            }
                            outStream.flush()
                            _isSentToPrinter.value = true
                        } ?: throw Exception("Unable to open PDF InputStream")
                    }
                }
                // Success: PDF sent to printer
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error (show message, log, etc.)
                _isSentToPrinter.value = false
            }
        }
    }

    fun generatePdfFromFields(
        context: Context,
        @IntRange(from = 0L, to = 100L) discountPercentage: Int,
        productName: String,
        initialPriceCents: Int,
        finalPriceCents: Int,
        qrCodeContent: String,
    ) {
        viewModelScope.launch {
            // 3 cm = 85 points (1 inch = 2.54 cm = 72 points, so 3/2.54*72)
            val pageWidth = 85
            val pageHeight = 85

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            val paintBg = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paintBg)

            // Pretty border
            val borderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(2f, 2f, pageWidth - 2f, pageHeight - 2f, borderPaint)

            // Title
            val titlePaint = Paint().apply {
                color = Color.rgb(33, 150, 243)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Product Coupon", 8f, 15f, titlePaint)

            // Product name
            val labelPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 7f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Product:", 8f, 25f, labelPaint)
            val valuePaint = Paint().apply {
                color = Color.BLACK
                textSize = 7f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(productName, 35f, 25f, valuePaint)

            // Prices
            canvas.drawText("Initial:", 8f, 35f, labelPaint)
            canvas.drawText("€%.2f".format(initialPriceCents / 100.0), 35f, 35f, valuePaint)
            canvas.drawText("Final:", 8f, 43f, labelPaint)
            canvas.drawText("€%.2f".format(finalPriceCents / 100.0), 35f, 43f, valuePaint)

            // Discount
            canvas.drawText("Discount:", 8f, 51f, labelPaint)
            canvas.drawText("$discountPercentage%", 50f, 51f, valuePaint)



            generateQRCode(canvas, qrCodeContent)

            pdfDocument.finishPage(page)

            // Save the PDF to the app's files directory
            val file = File(context.filesDir, "coupon.pdf")
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                fos.use { output ->
                    pdfDocument.writeTo(output)
                }
                Log.i("Main", "PDF saved to: ${file.absolutePath}")
                // Save URI for printing
                _pdfFileUri.value = Uri.fromFile(file)
                _isPdfGenerated.value = true
            } catch (e: IOException) {
                e.printStackTrace()
                _isPdfGenerated.value = false
            } finally {
                pdfDocument.close()
                fos?.close()
            }
        }
    }

    private fun generateQRCodeFake(canvas: Canvas?, qrCodeContent: String) {
        if (canvas == null) return

        val qrRect = RectF(55f, 8f, 80f, 33f)
        val qrPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(qrRect, qrPaint)
        val qrTextPaint = Paint().apply {
            color = Color.GRAY
            textSize = 5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("QR", qrRect.centerX(), qrRect.centerY() + 2f, qrTextPaint)
        // Optionally, draw the qrCodeInt string below the QR code
        canvas.drawText(qrCodeContent, qrRect.centerX(), qrRect.bottom + 6f, qrTextPaint)
    }


    private fun generateQRCode(canvas: Canvas?, qrCodeContent: String) {
        if (canvas == null) return

        // Generate QR code PNG as ByteArray
        val qrCodeBitmap = QRCode.ofSquares()
            .withSize(1) // Default is 25
            .build(data = qrCodeContent)
            .render()
            .nativeImage() as Bitmap

        val pageWidth = 85 // 3cm in points
        val pageHeight = 85

        val qrSize = 20f // size in points
        val left = (pageWidth - qrSize) / 2f
        val top = pageHeight * 2f / 3

        canvas.drawBitmap(
            qrCodeBitmap,
            null, // source rect (null = entire bitmap)
            RectF(
                left,
                top,
                left + qrCodeBitmap.width,
                top + qrCodeBitmap.height
            ), // destination rect in PDF units
            null // paint
        )
    }
}


//