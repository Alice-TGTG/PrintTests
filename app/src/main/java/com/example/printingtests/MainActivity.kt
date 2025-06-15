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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext


class MainActivity : ComponentActivity() {

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
                Column {
                    GeneratePdf(
                        onClickGenerate = { viewModel.generatePdfFromFields(
                            context = context,
                            discountPercentage = 38,
                            productName = "Frieda Miranda",
                            initialPriceCents = 9305,
                            finalPriceCents = 7495,
                            qrCodeContent = " 9988#61#2000001091449#000485",
                            )}
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
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_PDF && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                viewModel.sendPdfToPrinter(
                    contentResolver = contentResolver,
                    pdfUri = uri,
                    printerIp = "192.168.1.32",
                    port = 9100,
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
            Text("Send hardcoded content")
        })
}


