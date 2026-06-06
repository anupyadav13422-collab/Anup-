package com.example.ui

import com.example.data.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val displayValue by viewModel.displayValue.collectAsState()
    val previewValue by viewModel.previewValue.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val config by viewModel.vaultConfig.collectAsState()

    val showPinSetup by viewModel.showPinSetupDialog.collectAsState()
    val showRecovery by viewModel.showRecoveryDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
    ) {
        AnimatedContent(
            targetState = isUnlocked,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut())
            },
            label = "VaultTransition"
        ) { unlocked ->
            if (unlocked) {
                VaultView(viewModel = viewModel)
            } else {
                CalculatorView(
                    displayValue = displayValue,
                    previewValue = previewValue,
                    isSetup = config?.isSetup == true,
                    onDigit = viewModel::onDigitPressed,
                    onOperator = viewModel::onOperatorPressed,
                    onEquals = viewModel::onEqualsPressed,
                    onClear = viewModel::onClearPressed,
                    onDelete = viewModel::onDeletePressed,
                    onPercent = viewModel::onPercentPressed,
                    onDecimal = viewModel::onDecimalPressed
                )
            }
        }

        // --- PIN SETUP FLOW OVERLAY DIALOGS ---
        if (showPinSetup) {
            PinSetupDialog(viewModel = viewModel)
        }

        // --- PIN RECOVERY FLOW OVERLAY DIALOGS ---
        if (showRecovery) {
            PinRecoveryDialog(viewModel = viewModel)
        }
    }
}

@Composable
fun CalculatorView(
    displayValue: String,
    previewValue: String,
    isSetup: Boolean,
    onDigit: (Char) -> Unit,
    onOperator: (Char) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onPercent: () -> Unit,
    onDecimal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App header hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isSetup) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E2FD)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "🔒 Vault Setup: Enter a 4-digit PIN and press '='",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF041E49),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Standard Calculator",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF44474E),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Display screen
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = displayValue,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = if (displayValue.length > 10) 36.sp else 54.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Default,
                    color = Color(0xFF1C1B1F)
                ),
                maxLines = 2,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag("calc_display")
            )
            if (previewValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = previewValue,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Default,
                        color = Color(0xFF44474E)
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFC4C7C5).copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Custom Grid-Based Calculator Keyboard
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: C, Backspace, %, Division
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton("C", Modifier.weight(1f), containerColor = Color(0xFFF2F0F4), contentColor = Color(0xFFB3261E), action = onClear)
                CalcButton("⌫", Modifier.weight(1f), containerColor = Color(0xFFF2F0F4), contentColor = Color(0xFF0061A4), action = onDelete)
                CalcButton("%", Modifier.weight(1f), containerColor = Color(0xFFF2F0F4), contentColor = Color(0xFF0061A4), action = onPercent)
                CalcButton("÷", Modifier.weight(1f), containerColor = Color(0xFFD3E2FD), contentColor = Color(0xFF041E49), action = { onOperator('÷') })
            }
            // Row 2: 7, 8, 9, Multiplication
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton("7", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('7') })
                CalcButton("8", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('8') })
                CalcButton("9", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('9') })
                CalcButton("×", Modifier.weight(1f), containerColor = Color(0xFFD3E2FD), contentColor = Color(0xFF041E49), action = { onOperator('×') })
            }
            // Row 3: 4, 5, 6, Subtraction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton("4", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('4') })
                CalcButton("5", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('5') })
                CalcButton("6", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('6') })
                CalcButton("-", Modifier.weight(1f), containerColor = Color(0xFFD3E2FD), contentColor = Color(0xFF041E49), action = { onOperator('-') })
            }
            // Row 4: 1, 2, 3, Addition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton("1", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('1') })
                CalcButton("2", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('2') })
                CalcButton("3", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('3') })
                CalcButton("+", Modifier.weight(1f), containerColor = Color(0xFFD3E2FD), contentColor = Color(0xFF041E49), action = { onOperator('+') })
            }
            // Row 5: 0, Decimal point, Equals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Double width 0
                CalcButton("0", Modifier.weight(2.15f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = { onDigit('0') })
                CalcButton(".", Modifier.weight(1f), containerColor = Color(0xFFFDFBFF), contentColor = Color(0xFF1C1B1F), borderColor = Color(0xFFC4C7C5), action = onDecimal)
                CalcButton("=", Modifier.weight(1f), containerColor = Color(0xFF0061A4), contentColor = Color.White, tagSpec = "equals_button", action = onEquals)
            }
        }
    }
}

@Composable
fun CalcButton(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF333333),
    contentColor: Color = Color.White,
    borderColor: Color? = null,
    tagSpec: String? = null,
    action: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(if (label == "0") 2.15f else 1f)
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable { action() }
            .then(if (tagSpec != null) Modifier.testTag(tagSpec) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

// --- VAULT VISUALIZATION SHEETS ---
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VaultView(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }
    
    val notes by viewModel.secretNotes.collectAsState()
    val contacts by viewModel.secretContacts.collectAsState()
    val credentials by viewModel.secretCredentials.collectAsState()
    val media by viewModel.secretMedia.collectAsState()

    // Dialog trigger flags
    var showAddNote by remember { mutableStateOf(false) }
    var showAddContact by remember { mutableStateOf(false) }
    var showAddCredential by remember { mutableStateOf(false) }

    // Detailed Inspect Modal states
    var activeNoteToView by remember { mutableStateOf<SecretNote?>(null) }
    var activeContactToView by remember { mutableStateOf<SecretContact?>(null) }
    var activeCredToView by remember { mutableStateOf<SecretCredential?>(null) }

    // Photo picker configuration
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addPhoto(uri, context, "imported_${System.currentTimeMillis()}.jpg")
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0061A4))
                        Text("Secret Workspace", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1C1B1F))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("lock_vault_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = Color(0xFF1C1B1F))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF2F0F4),
                    titleContentColor = Color(0xFF1C1B1F)
                )
            )
        },
        floatingActionButton = {
            if (activeTab != 3) {
                FloatingActionButton(
                    onClick = {
                        when (activeTab) {
                            0 -> showAddNote = true
                            1 -> showAddCredential = true
                            2 -> showAddContact = true
                        }
                    },
                    containerColor = Color(0xFF0061A4),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp).testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Content")
                }
            } else {
                // Photo importing FAB
                FloatingActionButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    containerColor = Color(0xFF0061A4),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp).testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import Photo")
                }
            }
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFFFDFBFF),
                contentColor = Color(0xFF0061A4)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Notes", fontSize = 12.sp, color = if (activeTab == 0) Color(0xFF0061A4) else Color(0xFF44474E)) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (activeTab == 0) Color(0xFF0061A4) else Color(0xFF44474E)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Logins", fontSize = 12.sp, color = if (activeTab == 1) Color(0xFF0061A4) else Color(0xFF44474E)) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (activeTab == 1) Color(0xFF0061A4) else Color(0xFF44474E)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Contacts", fontSize = 12.sp, color = if (activeTab == 2) Color(0xFF0061A4) else Color(0xFF44474E)) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (activeTab == 2) Color(0xFF0061A4) else Color(0xFF44474E)) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("Photos", fontSize = 12.sp, color = if (activeTab == 3) Color(0xFF0061A4) else Color(0xFF44474E)) },
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (activeTab == 3) Color(0xFF0061A4) else Color(0xFF44474E)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (activeTab) {
                    0 -> NotesTabSection(
                        notes = notes,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onViewNote = { activeNoteToView = it }
                    )
                    1 -> CredentialsTabSection(
                        credentials = credentials,
                        onDeleteCredential = { viewModel.deleteCredential(it) },
                        onViewCredential = { activeCredToView = it }
                    )
                    2 -> ContactsTabSection(
                        contacts = contacts,
                        onDeleteContact = { viewModel.deleteContact(it) },
                        onViewContact = { activeContactToView = it }
                    )
                    3 -> MediaTabSection(
                        mediaList = media,
                        onDeleteMedia = { id, path -> viewModel.deletePhoto(id, path) }
                    )
                }
            }
        }
    }

    // --- DIALOGS FOR ADDING DATA ITEMS ---
    if (showAddNote) {
        AddNoteDialog(
            onDismiss = { showAddNote = false },
            onSave = { title, content ->
                viewModel.addNote(title, content)
                showAddNote = false
            }
        )
    }

    if (showAddCredential) {
        AddCredentialDialog(
            onDismiss = { showAddCredential = false },
            onSave = { site, user, pass, info ->
                viewModel.addCredential(site, user, pass, info)
                showAddCredential = false
            }
        )
    }

    if (showAddContact) {
        AddContactDialog(
            onDismiss = { showAddContact = false },
            onSave = { name, phone, email, desc ->
                viewModel.addContact(name, phone, email, desc)
                showAddContact = false
            }
        )
    }

    // --- DETAILED CARD RETRIEVAL MODALS ---
    val noteToView = activeNoteToView
    if (noteToView != null) {
        ViewNoteDetailsDialog(note = noteToView, onDismiss = { activeNoteToView = null })
    }

    val contactToView = activeContactToView
    if (contactToView != null) {
        ViewContactDetailsDialog(contact = contactToView, onDismiss = { activeContactToView = null })
    }

    val credToView = activeCredToView
    if (credToView != null) {
        ViewCredentialDetailsDialog(credential = credToView, onDismiss = { activeCredToView = null })
    }
}

// =================== TAB COMPOSABLES ===================

@Composable
fun NotesTabSection(
    notes: List<SecretNote>,
    onDeleteNote: (Int) -> Unit,
    onViewNote: (SecretNote) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Edit,
            title = "No Private Notes",
            description = "Store journal entries, secrets, and ideas that only you can view."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notes) { note ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F0F4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC4C7C5).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewNote(note) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title.ifEmpty { "Untitled" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF44474E),
                                maxLines = 2
                            )
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialsTabSection(
    credentials: List<SecretCredential>,
    onDeleteCredential: (Int) -> Unit,
    onViewCredential: (SecretCredential) -> Unit
) {
    if (credentials.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Lock,
            title = "No Secure Logins",
            description = "Save secure passwords, codes, and account pins local-only."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(credentials) { cred ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F0F4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC4C7C5).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewCredential(cred) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cred.siteName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Username: ${cred.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF44474E)
                            )
                        }
                        IconButton(onClick = { onDeleteCredential(cred.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTabSection(
    contacts: List<SecretContact>,
    onDeleteContact: (Int) -> Unit,
    onViewContact: (SecretContact) -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Person,
            title = "No Secret Contacts",
            description = "Store private dial connections hidden from your standard contact app."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(contacts) { contact ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F0F4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC4C7C5).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewContact(contact) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF44474E)
                            )
                        }
                        IconButton(onClick = { onDeleteContact(contact.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTabSection(
    mediaList: List<SecretMedia>,
    onDeleteMedia: (Int, String) -> Unit
) {
    if (mediaList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Face,
            title = "Empty Photo Vault",
            description = "Import private snapshots and screenshots from your phone to copy them securely inside."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mediaList) { item ->
                var showPhotoActions by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { showPhotoActions = true },
                            onLongClick = { showPhotoActions = true }
                        )
                ) {
                    val bitmap = remember(item.localPath) {
                        try {
                            BitmapFactory.decodeFile(item.localPath)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Error loading image", tint = Color.LightGray)
                        }
                    }
                }

                if (showPhotoActions) {
                    AlertDialog(
                        onDismissRequest = { showPhotoActions = false },
                        title = { Text("Secret Photo Details") },
                        text = {
                            Column {
                                Text("Imported At: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))}")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Size: ${item.fileSize / 1024} KB")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPhotoActions = false }) {
                                Text("Close")
                            }
                        },
                        dismissButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    onDeleteMedia(item.id, item.localPath)
                                    showPhotoActions = false
                                }
                            ) {
                                Text("Delete Forever", color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFC4C7C5),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF44474E),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// =================== DIALOG BUILDERS ===================

@Composable
fun PinSetupDialog(viewModel: CalculatorViewModel) {
    val tempPin by viewModel.tempPin.collectAsState()
    var verifyPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What was the name of your first pet?") }
    var securityAnswer by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // Local helper to manage setup stage transitions
    var errorMsg by remember { mutableStateOf("") }

    val securityQuestions = listOf(
        "What was the name of your first pet?",
        "In what city were you born?",
        "What is your mother's maiden name?",
        "What was your high school mascot?",
        "What was your favorite childhood book?"
    )
    var expandedDropDown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.closeSetupDialog() },
        title = {
            Text(
                text = when (step) {
                    1 -> "Step 1: Set Secret PIN"
                    2 -> "Step 2: Confirm Secret PIN"
                    else -> "Step 3: Security Question"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step == 1) {
                    Text("Your chosen 4-digit PIN is: $tempPin.", color = MaterialTheme.colorScheme.onSurface)
                    Text("Click Next to confirm your PIN.", color = Color.Gray, fontSize = 13.sp)
                } else if (step == 2) {
                    Text("Please re-enter your 4-digit PIN to confirm:")
                    OutlinedTextField(
                        value = verifyPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) verifyPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                        singleLine = true
                    )
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text("Choose a security question to recover your vault PIN if you ever lose or forget it.")
                    
                    Box {
                        OutlinedTextField(
                            value = securityQuestion,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropDown = true },
                            label = { Text("Security Question") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedDropDown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, "expand questions")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedDropDown,
                            onDismissRequest = { expandedDropDown = false }
                        ) {
                            securityQuestions.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q) },
                                    onClick = {
                                        securityQuestion = q
                                        expandedDropDown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = securityAnswer,
                        onValueChange = { securityAnswer = it },
                        label = { Text("Your Secret Answer") },
                        modifier = Modifier.fillMaxWidth().testTag("security_answer_input"),
                        singleLine = true
                    )
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else if (step == 2) {
                        if (verifyPin == tempPin) {
                            errorMsg = ""
                            step = 3
                        } else {
                            errorMsg = "PINs do not match! Please try again."
                            verifyPin = ""
                        }
                    } else {
                        if (securityAnswer.trim().isNotEmpty()) {
                            viewModel.completePinSetup(tempPin, securityQuestion, securityAnswer)
                        } else {
                            errorMsg = "Answer cannot be empty!"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4), contentColor = Color.White),
                modifier = Modifier.testTag("apply_pin_btn")
            ) {
                Text(
                    text = when (step) {
                        1 -> "Next"
                        2 -> "Next"
                        else -> "Finish Setup"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeSetupDialog() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PinRecoveryDialog(viewModel: CalculatorViewModel) {
    val config by viewModel.vaultConfig.collectAsState()
    var answer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.dismissRecoveryDialog() },
        title = { Text("Forgot Vault PIN?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Answer your security question below to reset your PIN code.")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F0F4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC4C7C5).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = config?.securityQuestion ?: "No recovery question found.",
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Your Secret Answer") },
                    modifier = Modifier.fillMaxWidth().testTag("recovery_answer_input"),
                    singleLine = true
                )
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.trim().isNotEmpty()) {
                        val verified = viewModel.verifyRecoveryAnswer(answer)
                        if (!verified) {
                            errorMessage = "Incorrect answer, try again."
                        }
                    } else {
                        errorMessage = "Please enter an answer."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4), contentColor = Color.White),
                modifier = Modifier.testTag("verify_recovery_btn")
            ) {
                Text("Verify & Reset PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissRecoveryDialog() }) {
                Text("Cancel")
            }
        }
    )
}

// =================== DATA MANIPULATION POPUPS ===================

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Secret Note") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write content...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("note_content_input"),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (content.isNotEmpty()) onSave(title, content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddCredentialDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Secure Login") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("Website or App Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                imageVector = if (passVisible) Icons.Default.Close else Icons.Default.Lock,
                                contentDescription = "toggle dynamic reveal"
                            )
                        }
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Extra Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (siteName.isNotEmpty() && password.isNotEmpty()) onSave(siteName, username, password, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Secret Contact") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Descriptions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty() && phone.isNotEmpty()) onSave(name, phone, email, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- INSPECTION DIALOG MODALS ---

@Composable
fun ViewNoteDetailsDialog(
    note: SecretNote,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifEmpty { "Untitled Note" }, fontWeight = FontWeight.Bold) },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.content,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Vault Note", note.content)
                    clipboard.setPrimaryClip(clip)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Content")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ViewContactDetailsDialog(
    contact: SecretContact,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contact.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (contact.phoneNumber.isNotEmpty()) {
                    Text("Phone: ${contact.phoneNumber}", fontWeight = FontWeight.Medium)
                }
                if (contact.email.isNotEmpty()) {
                    Text("Email: ${contact.email}")
                }
                if (contact.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contact.notes,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ViewCredentialDetailsDialog(
    credential: SecretCredential,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var revealPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(credential.siteName, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Username: ${credential.username}", fontWeight = FontWeight.Medium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (revealPassword) "Password: ${credential.password}" else "Password: ••••••••",
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { revealPassword = !revealPassword }) {
                        Icon(
                            imageVector = if (revealPassword) Icons.Default.Close else Icons.Default.Lock,
                            contentDescription = "toggle reveal"
                        )
                    }
                }
                if (credential.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = credential.notes,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Credentials Password", credential.password)
                    clipboard.setPrimaryClip(clip)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
