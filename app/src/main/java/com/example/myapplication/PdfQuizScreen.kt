package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.theme.LoopiColors
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* =================================================
   3D — PDF TO QUIZ
   Reads a PDF on the device and suggests multiple-choice
   questions. Nothing is uploaded; the learner reviews every
   question before it becomes a flashcard.
   ================================================= */

private const val MAX_PDF_PAGES = 40

/** Pulls the text layer out of a PDF. Scanned PDFs have no text layer and return blank. */
internal suspend fun extractPdfText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    PDFBoxResourceLoader.init(context.applicationContext)
    val input = context.contentResolver.openInputStream(uri) ?: error("Could not open that file.")
    input.use { stream ->
        PDDocument.load(stream).use { document ->
            PDFTextStripper().apply {
                startPage = 1
                endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
            }.getText(document)
        }
    }
}

private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "Notes.pdf"

private class EditableQuestion(source: GeneratedQuestion) {
    var question by mutableStateOf(source.question)
    var answer by mutableStateOf(source.answer)
    var selected by mutableStateOf(true)
    val distractors = source.distractors
    val explanation = source.explanation
}

private sealed interface PdfStep {
    data object Pick : PdfStep
    data class Reading(val fileName: String) : PdfStep
    data class Failed(val message: String) : PdfStep
    data class Review(val fileName: String, val uri: Uri) : PdfStep
}

@Composable
fun PdfQuizScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    topicId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topic = appState.findTopic(topicId)
    var step by remember { mutableStateOf<PdfStep>(PdfStep.Pick) }
    val questions = remember { mutableStateListOf<EditableQuestion>() }
    var saving by remember { mutableStateOf(false) }
    // topicId < 0 means "create a new topic from this PDF" (opened from Add Topic).
    val newTopic = topicId < 0
    var draftTitle by remember { mutableStateOf("") }
    var draftDescription by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Other") }
    val titleTaken = newTopic && appState.topics.any { it.title.trim().equals(draftTitle.trim(), ignoreCase = true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = displayName(context, uri)
        step = PdfStep.Reading(name)
        scope.launch {
            val result = runCatching {
                val text = extractPdfText(context, uri)
                if (text.isBlank()) error("This PDF has no readable text. It may be a scanned image; try a PDF exported from slides or notes.")
                if (newTopic) {
                    draftTitle = McqGenerator.suggestTitle(text, name)
                    draftDescription = McqGenerator.suggestDescription(text)
                    draftCategory = McqGenerator.suggestCategory(text)
                }
                withContext(Dispatchers.Default) { McqGenerator.generate(text, maxQuestions = if (newTopic) 20 else 15) }
            }
            result.onSuccess { generated ->
                if (generated.isEmpty()) {
                    step = PdfStep.Failed("Loopi couldn't find clear definitions or key terms in this PDF. Notes with lines like \"Term is ...\" or \"Term: meaning\" work best.")
                } else {
                    questions.clear()
                    questions.addAll(generated.map(::EditableQuestion))
                    runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    step = PdfStep.Review(name, uri)
                }
            }.onFailure {
                step = PdfStep.Failed(it.message ?: "That file couldn't be read as a PDF.")
            }
        }
    }

    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar(if (newTopic) "New Topic from PDF" else "Quiz from PDF") { navController.popBackStack() } },
        bottomBar = {
            val review = step as? PdfStep.Review
            if (review != null) {
                val count = questions.count { it.selected && it.question.isNotBlank() && it.answer.isNotBlank() }
                Surface(color = Color.White, shadowElevation = 10.dp) {
                    Button(
                        onClick = {
                            saving = true
                            val chosen = questions
                                .filter { it.selected && it.question.isNotBlank() && it.answer.isNotBlank() }
                                .map { GeneratedQuestion(it.question, it.answer, it.distractors, it.explanation) }
                            if (newTopic) {
                                flow3State.createTopicFromPdf(
                                    title = draftTitle,
                                    description = draftDescription.ifBlank { "Generated from ${review.fileName}" },
                                    category = draftCategory,
                                    cards = chosen,
                                    fileName = review.fileName,
                                    fileUri = review.uri.toString()
                                ) { createdId ->
                                    navController.navigate("topic_detail/$createdId") {
                                        popUpTo("add_topic") { inclusive = true }
                                    }
                                }
                                return@Button
                            }
                            // Keep the PDF with the topic's resources so it can be opened again.
                            flow3State.addResource(
                                topicId = topicId,
                                type = "File",
                                title = review.fileName.removeSuffix(".pdf"),
                                content = "",
                                url = "",
                                fileUri = review.uri.toString(),
                                description = "Generated ${countLabel(chosen.size, "question")} from this PDF.",
                                tags = "pdf"
                            )
                            flow3State.addGeneratedFlashcards(topicId, chosen) {
                                navController.navigate("flashcards/$topicId") {
                                    popUpTo("pdf_quiz/$topicId") { inclusive = true }
                                }
                            }
                        },
                        enabled = count > 0 && !saving && (!newTopic || (draftTitle.isNotBlank() && !titleTaken)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .height(54.dp)
                    ) {
                        Text(
                            if (newTopic) "Create topic with ${countLabel(count, "card")}"
                            else "Save ${countLabel(count, "question")} as flashcards",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (val current = step) {
            PdfStep.Pick, is PdfStep.Failed -> PdfPickPanel(
                topicTitle = topic?.title ?: if (newTopic) "a new topic" else "this topic",
                error = (current as? PdfStep.Failed)?.message,
                modifier = Modifier.padding(padding),
                onPick = { picker.launch(arrayOf("application/pdf")) }
            )
            is PdfStep.Reading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = LoopiColors.Blue)
                Spacer(Modifier.height(16.dp))
                Text("Reading ${current.fileName}…", color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold)
                Text("Finding key terms and definitions on your phone", color = LoopiColors.Muted, fontSize = 13.sp)
            }
            is PdfStep.Review -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = LoopiColors.BlueSoft) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                "${countLabel(questions.size, "question")} from ${current.fileName}",
                                color = LoopiColors.Navy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Generated offline, so check each one. Edit the wording, fix the answer, or untick any you don't want.",
                                color = LoopiColors.Ink,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                if (newTopic) {
                    item {
                        NewTopicDetailsCard(
                            title = draftTitle,
                            onTitle = { draftTitle = it.take(50) },
                            description = draftDescription,
                            onDescription = { draftDescription = it.take(160) },
                            category = draftCategory,
                            onCategory = { draftCategory = it },
                            titleError = when {
                                draftTitle.isBlank() -> "Give the topic a name"
                                titleTaken -> "You already have a topic with this name"
                                else -> null
                            }
                        )
                    }
                }
                itemsIndexed(questions) { index, item -> GeneratedQuestionCard(index + 1, item) }
                item {
                    TextButton(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose a different PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPickPanel(topicTitle: String, error: String?, modifier: Modifier, onPick: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .size(120.dp)
                .background(Brush.linearGradient(listOf(LoopiColors.Blue, LoopiColors.Purple)), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Turn your notes into a quiz", color = LoopiColors.Navy, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a PDF of lecture notes or slides for $topicTitle. Loopi reads it on your phone and suggests multiple-choice questions you can review before saving.",
            color = LoopiColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        listOf(
            "Works offline. The PDF never leaves your phone",
            "Best with notes that define terms (\"X is …\", \"X: …\")",
            "Scanned or photo PDFs can't be read"
        ).forEach { tip ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = LoopiColors.Teal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(tip, color = LoopiColors.Ink, fontSize = 14.sp)
            }
        }
        AnimatedVisibility(error != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = LoopiColors.RedSoft, modifier = Modifier.padding(top = 16.dp)) {
                Text(error.orEmpty(), color = LoopiColors.Red, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onPick, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (error == null) "Choose PDF" else "Try another PDF", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GeneratedQuestionCard(number: Int, item: EditableQuestion) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (item.selected) Color.White else LoopiColors.Track),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.selected) 2.dp else 0.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Q$number", color = LoopiColors.Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (item.selected) "Keep" else "Skip", color = LoopiColors.Muted, fontSize = 12.sp)
                Checkbox(checked = item.selected, onCheckedChange = { item.selected = it })
            }
            OutlinedTextField(
                value = item.question,
                onValueChange = { item.question = it },
                label = { Text("Question") },
                enabled = item.selected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = item.answer,
                onValueChange = { item.answer = it },
                label = { Text("Correct answer") },
                singleLine = true,
                enabled = item.selected,
                isError = item.selected && item.answer.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            if (item.distractors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Wrong options", color = LoopiColors.Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    item.distractors.forEach { option ->
                        Surface(shape = RoundedCornerShape(10.dp), color = LoopiColors.RedSoft) {
                            Text(option, color = LoopiColors.Red, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

private val pdfTopicCategories = listOf("Computer Science", "Information Technology", "Engineering", "Business", "Mathematics", "Other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewTopicDetailsCard(
    title: String,
    onTitle: (String) -> Unit,
    description: String,
    onDescription: (String) -> Unit,
    category: String,
    onCategory: (String) -> Unit,
    titleError: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopicIconTile(title = title.ifBlank { "Topic" }, category = category, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("New topic", color = LoopiColors.Navy, fontWeight = FontWeight.Bold)
                    Text("Filled in from the PDF. Change anything you like.", color = LoopiColors.Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                label = { Text("Topic title *") },
                singleLine = true,
                isError = titleError != null,
                supportingText = { titleError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescription,
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("Category", color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pdfTopicCategories.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { onCategory(option) },
                        label = { Text(option, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
        }
    }
}
