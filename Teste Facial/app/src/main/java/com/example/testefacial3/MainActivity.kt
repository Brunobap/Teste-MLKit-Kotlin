package com.example.testefacial3

import FaceAnalyzer
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.Visibility
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity() {

    // Objetos para o controle da câmera
    lateinit var previewView: PreviewView

    // Objetos para controle do reconhecimento de faces
    lateinit var cameraExecutor: ExecutorService
    lateinit var imageAnalyzer: ImageAnalysis

    // Componente de texto para teste de captura de valores
    lateinit var textView: TextView

    // Componente de imagem da moldura para botar o rosto
    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Checar a permissão da câmera
        if (getPermission()) {
            return
        }

        // Pegar o componente que terá o texto para teste
        textView = findViewById(R.id.textView)

        // Pegar o componente da moldura de rosto
        imageView = findViewById(R.id.imageView)

        // Pegar o componente da preview da câmera
        previewView = findViewById(R.id.previewView)

        // Pegar um solicitador de câmera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Criar uma thread para a execução da análise
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Ligar a análise da imagem com a função personalizada
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                // Definir a função personalizada como analisador
                it.setAnalyzer(cameraExecutor, FaceAnalyzer{
                        faces, largura -> displayInfo(faces, largura)
                })
            }

        // Verificar se o provedor de câmera está disponível
        cameraProviderFuture.addListener(Runnable {
            // Se estiver OK, configurar a preview e o uso da câmera
            val cameraProvider = cameraProviderFuture.get()
            ligarPreview(cameraProvider)

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()

        // Desligar a thread de análise ao sair
        cameraExecutor.close()
    }

    /**
     * Auxiliar que liga o preview a câmera
     */
    private fun  ligarPreview(cameraProvider: ProcessCameraProvider){
        // Criar o elemento de controle da preview
        val preview = Preview.Builder().build()
        // Ligar a superfície da previewView com a câmera
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Selecionar a lente a ser usada
        val cameraSelecionada = CameraSelector.DEFAULT_FRONT_CAMERA

        // Tirar outras utilidades "penduradas" na câmera
        cameraProvider.unbindAll()

        // Ligar o funcionamento da câmera aquela superfície até o fim do ciclo do app
        cameraProvider.bindToLifecycle(this, cameraSelecionada, preview, imageAnalyzer)
    }

    /**
     * Auxiliar que pega uma informação do rosto e bota no texto de display
     */
    private fun displayInfo(faces: MutableList<Face>, tamTela: Float){
        if (faces.isNotEmpty()){
            val face = faces[0]

            // Texto escolhido para ser exibido no final
            var texto = "..."

            // Abertura de ambos os olhos
            if (face.leftEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
                val olhoEsq = (face.leftEyeOpenProbability!! * 100f).toInt()
                val olhoDir = (face.rightEyeOpenProbability!! * 100f).toInt()

                if (olhoDir < 90 || olhoEsq < 90) texto = "Mantenha os 2 olhos abertos"
            }// else return ;

            // Expressão facial
            if (face.smilingProbability != null) {
                val sorriso = (face.smilingProbability!! * 100f).toInt()
                if (sorriso > 10) texto = "Mantenha uma expressão facial neutra"
            } //else return ;

            // Inclinação sobre os ombros
            val rotZ = face.headEulerAngleZ
            if (rotZ.absoluteValue > 2){
                texto = "Mantenha a cabeça erguida"
            }

            // Inclinação para cima/baixo
            val rotX = face.headEulerAngleX
            if (rotX.absoluteValue > 10){
                texto =
                    if (rotX > 0) "Incline a sua cabeça para a baixo"
                    else "Incline a sua cabeça para a cima"
            }

            // Movimentação para os lados
            val rotY = face.headEulerAngleY
            if (rotY.absoluteValue > 10){
                texto =
                    if (rotY > 0) "Incline a sua cabeça para a direita"
                    else "Incline a sua cabeça para a esquerda"
            }

            // Proporção do tamanho do rosto
            val tamCabeca = face.boundingBox.width().toFloat()
            if (tamCabeca / tamTela < .45) texto = "Aproxime um pouco mais o rosto"

            // Se alguma instrução tiver sido escolhida,
            if (texto == "...") {
                // Colocar ela e a cor amarela
                textView.text = "Posicionamento correto"
                textView.setBackgroundColor(Color.GREEN)

            // Se nenhuma tiver sido escolhida, a imagem está na qualidade boa
            } else {
                textView.text = texto
                textView.setBackgroundColor(Color.YELLOW)
            }

            // Ligar a moldura de rosto
            imageView.visibility = View.VISIBLE

            // Caso não detecte nenhum rosto, informar o usuário
        } else {
            textView.text = "Aproxime o rosto da câmera"
            textView.setBackgroundColor(Color.RED)
            // Desligar a moldura de rosto
            imageView.visibility = View.INVISIBLE
        }
    }

    /**
     * Auxiliar para checar a permissão da câmera
     */
    fun getPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
            return true
        } else return false
    }
}