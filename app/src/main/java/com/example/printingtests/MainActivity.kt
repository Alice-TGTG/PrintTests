package com.example.printingtests

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {

    val PRINTER_IP = "192.168.8.201" // MACBOOK: "10.24.111.121" // "192.168.1.32" // ZQL620: "192.168.8.201"
    val PRINTER_PORT = 9100


    val viewModel: MainViewModel by viewModels<MainViewModel>()

    val REQUEST_CODE_OPEN_PDF = 555

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                val pdfState = viewModel.isPdfGenerated.collectAsState()
                val printState = viewModel.isSentToPrinter.collectAsState()

                Column {
                    ShowStatus(pdfState.value, printState.value)
                    GeneratePdf(
                        onClickGenerate = {
                            viewModel.generatePdfFromFields(
                                context = context,
                                discountPercentage = 38,
                                productName = "Frieda Miranda",
                                initialPriceCents = 9305,
                                finalPriceCents = 7495,
                                qrCodeContent = "9988#61#2000001091449#000485",
                            )
                        }
                    )
                    PrintCommand(
                        onPrintClicked = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/pdf"
                            }

                            startActivityForResult(intent, REQUEST_CODE_OPEN_PDF)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    PrintZPLWithQRCode(onClickSendZPL = {
                        viewModel.sendZPL(printerIp = PRINTER_IP)
                    })
                }
            }
        }
    }

    @Composable
    fun PrintZPLWithQRCode(onClickSendZPL: () -> Unit = {}) {
        Button(onClick = { onClickSendZPL() }) { Text("Send ZPL with QR code") }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_PDF && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                viewModel.sendPdfToPrinter(
                    contentResolver = contentResolver,
                    pdfUri = uri,
                    printerIp = PRINTER_IP,
                    port = PRINTER_PORT,
                )
                Log.i("Main", "Done sending PdF")
            }
        }
    }

}


@Composable
fun GeneratePdf(
    onClickGenerate: () -> Unit = {},
) {
    Button(onClick = { onClickGenerate() }) { Text("Generate PDF") }
}

@Composable
fun PrintCommand(
    onPrintClicked: () -> Unit = {},
) {

    Button(
        onClick = { onPrintClicked() },
        content = {
            Text("Send PDF bytes to printer IP:port")
        })
}

@Composable
fun ShowStatus(isPdfGenerated: Boolean, isSentToPrinter: Boolean) {
    Row {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isPdfGenerated) Color.Green else Color.Gray
        )
        Text(text = "PDF ${if (isPdfGenerated) "" else "not"} generated")
        Spacer(modifier = Modifier.width(18.dp))
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isSentToPrinter) Color.Green else Color.Gray
        )
        Text(text = "PDF ${if (isPdfGenerated) "" else "not"} sent to printer")
    }
}


