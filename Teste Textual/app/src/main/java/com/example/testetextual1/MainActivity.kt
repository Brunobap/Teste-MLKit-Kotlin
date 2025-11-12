package com.example.testetextual1

import android.app.Activity
import android.app.ComponentCaller
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    // Componentes gráficos
    lateinit var inpDoc: TextView
    lateinit var cx01: LinearLayout
    lateinit var cx02: ScrollView

    // Objeto que faz o reconhecimento textual,
    // com a configuração padrão (caracteres em latim)
    val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Informações do arquivo selecionado
    lateinit var uriDoc: Uri
    lateinit var nomeDoc: String
    lateinit var tipoDoc: String

    val someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the result here
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data

            // Pegar o URI selecionado no seletor
            uriDoc = data?.data!!
            val cursor = contentResolver.query(uriDoc, null, null, null, null)

            // Procurar a localização real do arquivo no sistema de armazenamento
            if (cursor != null && cursor.moveToFirst()){
                // Pegar o nome de verdade do arquivo (3a coluna de informações)
                nomeDoc = cursor.getString(2)
                inpDoc.text = nomeDoc

                // Pegar o tipo de arquivo (usados: "image/jpeg", "image/png", "application/pdf") (2a coluna de informações)
                tipoDoc = contentResolver.getType(uriDoc)!!

                cursor.close()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Antes de tudo, pedir a permissão para acessar os arquivos
        if (getPermission()) {
            return
        }

        // Colocar a função de escolher um arquivo no EditText
        inpDoc = findViewById(R.id.inpDoc)
        inpDoc.setOnClickListener { abrirSeletor() }

        // Colocar a função de enviar a imagem ao reconhecimento se estiver certo
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)
        btnEnviar.setOnClickListener { enviarDoc() }

        // Pegar as caixas com o conteúdo
        cx01 = findViewById(R.id.cx01)
        cx02 = findViewById(R.id.cx02)

        // Colocar a função do botão de cancelar envio
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            cx01.visibility = LinearLayout.VISIBLE
            cx02.visibility = LinearLayout.GONE
        }
    }

    /**
     * Função auxiliar para pedir a permissão de ler os arquivos
     */
    private fun getPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 101)
            return true
        } else return false
    }

    /**
     * Função auxiliar que abre a tela de selecionar um arquivo
     */
    private fun abrirSeletor(){
        val intent = Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        someActivityResultLauncher.launch(Intent.createChooser(intent, "Selecione o documento que deseja enviar"))
    }

    /**
     * Função auxiliar que vê se o documento é usável, e se for usa no reconhecimento
     */
    private fun enviarDoc(){
        // Montar a entrada do reconhecedor textual
        val inpRecog = InputImage.fromFilePath(this, uriDoc)

        // Enviar ao reconhecimento textual
        reconhecedor.process(inpRecog)
            .addOnSuccessListener { texto -> exibirInfo(texto) }
    }

    private fun exibirInfo(texto: Text){
        // Verificar se há um texto lido na imagem
        // Se não tiver, não fazer nada
        if (texto.textBlocks.isEmpty()) return ;

        // Se houver, salvar essas informações para a próxima tela
        var txtTemp = ""
        for (bloco in texto.textBlocks) txtTemp = txtTemp + bloco.text + "\n\n"
        val txtResultado = findViewById<TextView>(R.id.txtResultado)
        txtResultado.text = txtTemp

        // Trocar a caixa sendo mostrada
        cx01.visibility = LinearLayout.GONE
        cx02.visibility = LinearLayout.VISIBLE
    }
}