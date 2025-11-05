import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

// Você pode passar seu detector como um parâmetro
class FaceAnalyzer(private val onFacesDetected: (MutableList<Face>, Float) -> Unit) : ImageAnalysis.Analyzer {

    // Configurações do detector de rosto (ML Kit é uma ótima opção aqui, mas você pode usar seu TFLite)
    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()
    val detector = FaceDetection.getClient(highAccuracyOpts)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Cria um InputImage a partir do ImageProxy do CameraX
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // --- AQUI É ONDE SUA LÓGICA DE DETECÇÃO ENTRA ---
            // Este exemplo usa o ML Kit, mas a lógica é a mesma para seu modelo TFLite
            detector.process(image)
                .addOnSuccessListener { faces ->
                    // Tarefa bem-sucedida. 'faces' é uma lista de rostos detectados.
                    // Você pode iterar sobre eles, pegar os bounding boxes,
                    // recortar os rostos e rodar seu modelo de reconhecimento.
                    onFacesDetected(faces, mediaImage.width.toFloat())
                }
                .addOnFailureListener { e ->
                    // A tarefa falhou.
                    Log.e("FaceAnalyzer", "Detecção de rosto falhou", e)
                }
                .addOnCompleteListener {
                    // SEMPRE feche o imageProxy quando terminar,
                    // caso contrário, a câmera para de enviar frames.
                    imageProxy.close()
                }

        } else imageProxy.close() // Garanta que feche mesmo se a imagem for nula
    }
}
