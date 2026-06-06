package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.DecimalFormat

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VaultDatabase.getDatabase(application)
    private val repository = VaultRepository(db)

    // --- Calculator Engine States ---
    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _previewValue = MutableStateFlow("")
    val previewValue: StateFlow<String> = _previewValue.asStateFlow()

    // --- Vault Security States ---
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val vaultConfig: StateFlow<VaultConfig?> = repository.vaultConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    private val _setupStep = MutableStateFlow(1) // 1 = Enter PIN, 2 = Confirm PIN, 3 = Security Question
    val setupStep: StateFlow<Int> = _setupStep.asStateFlow()

    private val _tempPin = MutableStateFlow("")
    val tempPin: StateFlow<String> = _tempPin.asStateFlow()

    private val _showRecoveryDialog = MutableStateFlow(false)
    val showRecoveryDialog: StateFlow<Boolean> = _showRecoveryDialog.asStateFlow()

    private val _recoveryMessage = MutableStateFlow("")
    val recoveryMessage: StateFlow<String> = _recoveryMessage.asStateFlow()

    // --- Vault Data Streams ---
    val secretNotes: StateFlow<List<SecretNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretContacts: StateFlow<List<SecretContact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretCredentials: StateFlow<List<SecretCredential>> = repository.allCredentials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretMedia: StateFlow<List<SecretMedia>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Helper Formatters ---
    private val df = DecimalFormat("#.#######")

    // --- Calculator Input Handlers ---
    fun onDigitPressed(char: Char) {
        val current = _displayValue.value
        if (current == "0" || current == "Error") {
            _displayValue.value = char.toString()
        } else {
            _displayValue.value = current + char
        }
        updatePreview()
    }

    fun onOperatorPressed(op: Char) {
        val current = _displayValue.value
        if (current == "Error") return
        val lastChar = if (current.isNotEmpty()) current.last() else ' '
        if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷') {
            _displayValue.value = current.dropLast(1) + op
        } else {
            _displayValue.value = current + op
        }
        updatePreview()
    }

    fun onDecimalPressed() {
        val current = _displayValue.value
        if (current == "Error") return
        
        // Find current token (operand starting after the last operator)
        val lastOperatorIdx = current.indexOfLast { it == '+' || it == '-' || it == '×' || it == '÷' }
        val currentOperand = if (lastOperatorIdx == -1) current else current.substring(lastOperatorIdx + 1)
        
        if (!currentOperand.contains('.')) {
            _displayValue.value = current + "."
        }
        updatePreview()
    }

    fun onClearPressed() {
        _displayValue.value = "0"
        _previewValue.value = ""
    }

    fun onDeletePressed() {
        val current = _displayValue.value
        if (current == "Error" || current.length <= 1) {
            _displayValue.value = "0"
        } else {
            _displayValue.value = current.dropLast(1)
        }
        updatePreview()
    }

    fun onPercentPressed() {
        val current = _displayValue.value
        if (current == "Error" || current == "0") return
        try {
            val lastOperatorIdx = current.indexOfLast { it == '+' || it == '-' || it == '×' || it == '÷' }
            if (lastOperatorIdx == -1) {
                val value = current.toDouble() / 100.0
                _displayValue.value = df.format(value)
            } else {
                val base = current.substring(0, lastOperatorIdx + 1)
                val value = current.substring(lastOperatorIdx + 1).toDouble() / 100.0
                _displayValue.value = base + df.format(value)
            }
            updatePreview()
        } catch (e: Exception) {
            _displayValue.value = "Error"
        }
    }

    private fun updatePreview() {
        val expr = _displayValue.value
        val hasOp = expr.any { it == '+' || it == '-' || it == '×' || it == '÷' }
        if (!hasOp) {
            _previewValue.value = ""
            return
        }
        // Avoid preview on bare trailing operator
        val last = expr.lastOrNull()
        if (last == '+' || last == '-' || last == '×' || last == '÷') {
            return
        }
        try {
            val parsedRes = evaluate(expr)
            _previewValue.value = df.format(parsedRes)
        } catch (e: Exception) {
            _previewValue.value = ""
        }
    }

    fun onEqualsPressed() {
        val expr = _displayValue.value
        
        // Check for PIN Unlock or Passcode Reset first
        if (expr == "999999") {
            triggerRecovery()
            return
        }

        // Check if expr can be a 4-digit PIN setup trigger
        val pinCandidate = expr.filter { it.isDigit() }
        val isPlainNumber = expr == pinCandidate && expr.length == 4

        viewModelScope.launch {
            val config = repository.getConfigDirect()
            if (config == null || !config.isSetup) {
                if (isPlainNumber) {
                    // Try to trigger PIN Setup flow
                    _tempPin.value = expr
                    _setupStep.value = 2 // progress to verify
                    _showPinSetupDialog.value = true
                } else {
                    evaluateAndSetResult()
                }
            } else {
                // Config exists and is set up! Let's check hash
                val hashedCandidate = hashPin(expr)
                if (hashedCandidate == config.pinHash) {
                    _isUnlocked.value = true
                    _displayValue.value = "0"
                    _previewValue.value = ""
                } else {
                    evaluateAndSetResult()
                }
            }
        }
    }

    private fun evaluateAndSetResult() {
        val expr = _displayValue.value
        try {
            val result = evaluate(expr)
            _displayValue.value = df.format(result)
            _previewValue.value = ""
        } catch (e: Exception) {
            _displayValue.value = "Error"
            _previewValue.value = ""
        }
    }

    private fun triggerRecovery() {
        viewModelScope.launch {
            val config = repository.getConfigDirect()
            if (config != null && config.isSetup) {
                _recoveryMessage.value = ""
                _showRecoveryDialog.value = true
            } else {
                _displayValue.value = "0"
            }
        }
    }

    // --- Vault Action Handlers ---
    fun lockVault() {
        _isUnlocked.value = false
        _displayValue.value = "0"
    }

    fun completePinSetup(pin: String, question: String, answer: String) {
        viewModelScope.launch {
            val config = VaultConfig(
                pinHash = hashPin(pin),
                isSetup = true,
                securityQuestion = question,
                securityAnswerHash = hashPin(answer.lowercase().trim())
            )
            repository.saveConfig(config)
            _showPinSetupDialog.value = false
            _isUnlocked.value = true
            _displayValue.value = "0"
        }
    }

    fun closeSetupDialog() {
        _showPinSetupDialog.value = false
        _setupStep.value = 1
        _tempPin.value = ""
    }

    fun verifyRecoveryAnswer(answer: String): Boolean {
        var success = false
        viewModelScope.launch {
            val config = repository.getConfigDirect() ?: return@launch
            val hashedAnswer = hashPin(answer.lowercase().trim())
            if (hashedAnswer == config.securityAnswerHash) {
                success = true
                // Reset vault setup so they can generate a new one
                repository.saveConfig(VaultConfig(pinHash = "", isSetup = false))
                _showRecoveryDialog.value = false
                _displayValue.value = "0"
            }
        }
        return success
    }

    fun dismissRecoveryDialog() {
        _showRecoveryDialog.value = false
        _displayValue.value = "0"
    }

    // --- Live CRUD operations ---
    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(SecretNote(title = title, content = content))
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(id)
        }
    }

    fun addContact(name: String, phone: String, email: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveContact(SecretContact(name = name, phoneNumber = phone, email = email, notes = notes))
        }
    }

    fun deleteContact(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContactById(id)
        }
    }

    fun addCredential(siteName: String, uName: String, pWord: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCredential(SecretCredential(siteName = siteName, username = uName, password = pWord, notes = notes))
        }
    }

    fun deleteCredential(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCredentialById(id)
        }
    }

    fun addPhoto(uri: Uri, context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val stream = resolver.openInputStream(uri) ?: return@launch
                val vaultDir = File(context.filesDir, "vault_photos")
                if (!vaultDir.exists()) {
                    vaultDir.mkdirs()
                    File(vaultDir, ".nomedia").createNewFile()
                }

                val savedFile = File(vaultDir, "photo_${System.currentTimeMillis()}.dat")
                val fos = FileOutputStream(savedFile)
                stream.use { input ->
                    fos.use { output ->
                        input.copyTo(output)
                    }
                }

                val record = SecretMedia(
                    fileName = name,
                    localPath = savedFile.absolutePath,
                    fileSize = savedFile.length()
                )
                repository.saveMedia(record)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePhoto(id: Int, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteMediaById(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Math Evaluator ---
    private fun evaluate(expression: String): Double {
        val sanitized = expression.replace("×", "*").replace("÷", "/")
        return parseString(sanitized)
    }

    private fun parseString(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected expression structure: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val numStr = str.substring(startPos, this.pos)
                    x = numStr.toDouble()
                } else {
                    throw RuntimeException("Unknown token: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }

    // --- Hash helper for simple cryptographic security ---
    private fun hashPin(pin: String): String {
        return try {
            val bytes = pin.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin // fallback to plain if hash service fails
        }
    }
}
