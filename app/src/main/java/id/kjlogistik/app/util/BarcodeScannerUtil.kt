package id.kjlogistik.app.util

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class BarcodeScannerUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanBarcode(): Result<String> {
        val moduleInstallClient = ModuleInstall.getClient(context)
        val barcodeScanner = GmsBarcodeScanning.getClient(context)

        return suspendCoroutine { continuation ->
            moduleInstallClient
                .areModulesAvailable(barcodeScanner)
                .addOnSuccessListener {
                    if (it.areModulesAvailable()) {
                        initiateScan(continuation)
                    } else {
                        val moduleInstallRequest =
                            ModuleInstallRequest.newBuilder()
                                .addApi(barcodeScanner)
                                .build()
                        moduleInstallClient
                            .installModules(moduleInstallRequest)
                            .addOnSuccessListener {
                                initiateScan(continuation)
                            }
                            .addOnFailureListener { e ->
                                continuation.resume(Result.failure(e))
                            }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }

    private fun initiateScan(continuation: kotlin.coroutines.Continuation<Result<String>>) {
        val scanner = GmsBarcodeScanning.getClient(context)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let {
                    continuation.resume(Result.success(it))
                } ?: continuation.resume(Result.failure(Exception("No value in barcode")))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
            .addOnCanceledListener {
                continuation.resume(Result.failure(Exception("Scan cancelled")))
            }
    }
}