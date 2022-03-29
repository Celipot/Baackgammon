package com.example.pa6

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.pa6.ml.Model
import kotlinx.android.synthetic.main.fragment_second.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.Executor

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), Executor {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = activity?.let { ProcessCameraProvider.getInstance(it) }
        cameraProviderFuture?.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val viewFinder = view?.findViewById<PreviewView>(R.id.previewViewFinder)

            val surfaceProvider = viewFinder?.surfaceProvider
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(surfaceProvider)
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageCapture = ImageCapture.Builder().build()

            initListener(imageCapture)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, preview)

            } catch(exc: Exception) {}

        }, activity?.let { ContextCompat.getMainExecutor(it) })
    }

    private fun initListener(imageCapture: ImageCapture) {
        button_take_photo.setOnClickListener {
            val fileName = "JPEG_${System.currentTimeMillis()}"
            val file = File(context?.externalCacheDir.toString() + File.separator + System.currentTimeMillis() + ".png")
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(outputFileOptions, this,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException)
                    {
                        val msg = "Photo capture failed: $exception"
                        Log.e("CameraXApp", msg)
                    }
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Log.d("CameraXApp", msg)
                        processModel(file.absolutePath)
                    }
                })

        }
    }
    private fun processModel(imagePath: String) {
        if (context != null) {
            val model = Model.newInstance(requireContext())
            val image = TensorBuffer.createFixedSize(intArrayOf(1, 416, 416, 3),DataType.FLOAT32)
            val resized = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imagePath), 416, 416, true)
            val byteBuffer = TensorImage.createFrom(TensorImage.fromBitmap(resized),DataType.FLOAT32).buffer
            image.loadBuffer(byteBuffer)
            val outputs = model.process(image)
            Log.i("result", outputs.categoryAsTensorBuffer.floatArray.contentToString())
            Log.i("result", outputs.categoryAsTensorBuffer.intArray.contentToString())
            Log.i("result", outputs.categoryAsTensorBuffer.dataType.name)
            Log.i("result", outputs.categoryAsTensorBuffer.shape.contentToString())
            Log.i("result", outputs.categoryAsTensorBuffer.buffer.toString())
            model.close()
        }
    }

    override fun execute(command: Runnable) {
        command.run()
    }

}