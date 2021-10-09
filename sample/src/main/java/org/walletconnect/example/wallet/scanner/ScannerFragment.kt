package org.walletconnect.example.wallet.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.walletconnect.example.R
import org.walletconnect.example.databinding.ScannerFragmentBinding
import org.walletconnect.example.wallet.WalletViewModel
import java.util.concurrent.Executors

class ScannerFragment : Fragment(R.layout.scanner_fragment) {

    private lateinit var binding: ScannerFragmentBinding
    private val viewModel: WalletViewModel by activityViewModels()

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var shouldScan: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ScannerFragmentBinding.bind(view)
        activity?.invalidateOptionsMenu()
        setupCamera()
        viewModel.hideBottomNav()
    }

    private fun setupCamera() {
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        setupObserver()
    }

    private val cameraPermissionCallback =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                bindCameraUseCases()
            }
        }

    private fun setupObserver() {
        viewModel
            .processCameraProvider
            .observe(viewLifecycleOwner, { provider: ProcessCameraProvider ->
                cameraProvider = provider
                if (isCameraPermissionGranted()) {
                    bindCameraUseCases()
                } else {
                    cameraPermissionCallback.launch(Manifest.permission.CAMERA)
                }
            })
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    private fun bindPreviewUseCase() {
        cameraProvider?.let {
            if (previewUseCase != null) {
                it.unbind(previewUseCase)
            }

            previewUseCase = Preview.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .build()
            previewUseCase?.setSurfaceProvider(binding.previewView.surfaceProvider)
            cameraProvider?.bindToLifecycle(this, cameraSelector!!, previewUseCase)
        }
    }

    private fun bindAnalyseUseCase() {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build();
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

        cameraProvider?.let {
            if (analysisUseCase != null) {
                it.unbind(analysisUseCase)
            }

            analysisUseCase = ImageAnalysis.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .build()

            analysisUseCase?.setAnalyzer(Executors.newSingleThreadExecutor(),
                { imageProxy -> processImageProxy(barcodeScanner, imageProxy) }
            )

            cameraProvider?.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val inputImage =
            imageProxy.image?.let {
                InputImage.fromMediaImage(
                    it,
                    imageProxy.imageInfo.rotationDegrees
                )
            }
        inputImage?.let { image ->
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && shouldScan) {
                        barcodes.first().rawValue?.let {
                            shouldScan = false
                            viewModel.pair(it)
                            activity?.onBackPressed()
                        }
                    }
                }
                .addOnFailureListener { Log.e("Failure", it.message.toString()) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}