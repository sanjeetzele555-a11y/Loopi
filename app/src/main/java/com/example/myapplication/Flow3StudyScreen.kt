package com.example.myapplication

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.theme.LoopiColors

/* =================================================
   3B — FLASHCARD STUDY MODE
   Flip through a topic's cards and rate each one. Ratings are a
   self-check for this session; quizzes are what get saved to progress.
   ================================================= */

private val studyRatings = listOf(
    Triple("Not sure", "😕", LoopiColors.Red),
    Triple("Maybe", "🤔", LoopiColors.Amber),
    Triple("I know it", "😎", LoopiColors.Teal)
)

@Composable
fun FlashcardStudyScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    topicId: Int
) {
    val topic = appState.findTopic(topicId)
    val cards = flow3State.flashcardsForTopic(topicId)
    var index by rememberSaveable { mutableIntStateOf(0) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    // Card id -> rating, so going back to a card shows what was chosen before.
    val ratings = remember { mutableStateMapOf<Int, String>() }
    var finished by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar(topic?.title ?: "Flashcards") { navController.popBackStack() } }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cards.isEmpty()) {
                Spacer(Modifier.height(60.dp))
                Text("No flashcards yet", color = LoopiColors.Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Add cards from the topic page to start studying.", color = LoopiColors.Muted)
                return@Column
            }

            if (finished) {
                val known = cards.count { ratings[it.id] == "I know it" }
                val unsure = cards.count { ratings[it.id] == "Not sure" }
                val maybe = cards.count { ratings[it.id] == "Maybe" }
                Spacer(Modifier.height(40.dp))
                Text("🎉", fontSize = 56.sp)
                Text("Deck complete", color = LoopiColors.Navy, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudyTally("$known", "Know it", LoopiColors.Teal, Modifier.weight(1f))
                    StudyTally("$maybe", "Maybe", LoopiColors.Amber, Modifier.weight(1f))
                    StudyTally("$unsure", "Not sure", LoopiColors.Red, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    if (unsure + maybe > 0) "Test yourself with a quiz to lock in the cards you weren't sure about."
                    else "Nice! A quiz will confirm it and update your progress.",
                    color = LoopiColors.Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { navController.navigate("review_settings/$topicId") },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Take a quiz", fontWeight = FontWeight.Bold) }
                TextButton(onClick = {
                    index = 0
                    flipped = false
                    ratings.clear()
                    finished = false
                }) { Text("Study again") }
                return@Column
            }

            val card = cards[index.coerceIn(0, cards.lastIndex)]
            val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(350), label = "flip")

            Spacer(Modifier.height(12.dp))
            Text("${index + 1}/${cards.size}", color = LoopiColors.Muted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LoopiProgressBar(progress = { (index + 1f) / cards.size }, modifier = Modifier.fillMaxWidth().height(6.dp))
            Spacer(Modifier.height(18.dp))

            Card(
                onClick = { flipped = !flipped },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (rotation > 90f) LoopiColors.BlueSoft else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
            ) {
                Box(
                    Modifier.fillMaxSize().padding(24.dp).graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (rotation > 90f) "ANSWER" else "QUESTION",
                            color = LoopiColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (rotation > 90f) card.answer else card.question,
                            color = LoopiColors.Navy,
                            fontSize = 22.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (rotation > 90f && card.explanation.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(card.explanation, color = LoopiColors.Ink, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (rotation > 90f) "Tap to see the question" else "Tap to reveal the answer",
                            color = LoopiColors.Blue,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("How well do you know it?", color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                studyRatings.forEach { (label, face, accent) ->
                    val chosen = ratings[card.id] == label
                    Surface(
                        onClick = { ratings[card.id] = label },
                        shape = RoundedCornerShape(14.dp),
                        color = if (chosen) accent.copy(alpha = 0.14f) else Color.White,
                        border = BorderStroke(if (chosen) 2.dp else 1.dp, if (chosen) accent else LoopiColors.Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(face, fontSize = 22.sp, modifier = Modifier.scale(if (chosen) 1.15f else 1f))
                            Text(label, fontSize = 12.sp, color = if (chosen) LoopiColors.Navy else LoopiColors.Muted, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { index--; flipped = false },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Previous") }
                Button(
                    onClick = {
                        if (index < cards.lastIndex) {
                            index++
                            flipped = false
                        } else {
                            finished = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text(if (index < cards.lastIndex) "Next" else "Finish", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun StudyTally(value: String, label: String, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.12f)) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(label, color = LoopiColors.Navy, fontSize = 12.sp)
        }
    }
}
