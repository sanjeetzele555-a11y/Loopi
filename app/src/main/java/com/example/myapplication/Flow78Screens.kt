package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.theme.LoopiColors
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* =========================================================
   FLOW 7 — COMMUNITY
   Study groups and posts are stored on this device. There is no
   server yet, so the starter groups are examples to join and post in.
   ========================================================= */

data class StudyGroup(
    val id: String,
    val name: String,
    val topic: String,
    val about: String,
    val members: List<String>
)

data class GroupPost(val groupId: String, val author: String, val text: String, val postedAt: Long)

private val starterGroups = listOf(
    StudyGroup(
        "db", "SQL Study Circle", "Databases",
        "Weekly practice on normal forms, joins and indexing before quizzes.",
        listOf("Azhar", "Dariya", "Nursaule")
    ),
    StudyGroup(
        "net", "Networks Night Review", "Networks",
        "Short evening sessions on the OSI model, TCP/IP and subnetting.",
        listOf("Sanjeet", "Dariya")
    ),
    StudyGroup(
        "algo", "Algorithms Problem Club", "Algorithms",
        "Solve one problem together each day and explain the approach simply.",
        listOf("Suzain", "Azhar", "Sanjeet", "Nursaule")
    ),
    StudyGroup(
        "os", "OS Exam Prep", "Operating Systems",
        "Flashcard swaps on scheduling, memory and deadlocks.",
        listOf("Nursaule")
    )
)

private val starterPosts = listOf(
    GroupPost("db", "Dariya", "Tip: 2NF is about partial dependencies, 3NF is about transitive ones.", 0L),
    GroupPost("algo", "Azhar", "Anyone want to compare answers on the Dijkstra cards?", 0L),
    GroupPost("net", "Sanjeet", "Made a pack for subnetting. Import it from Study Packs!", 0L)
)

class CommunityState(context: Context) {
    private val prefs = context.getSharedPreferences("loopi_community", Context.MODE_PRIVATE)
    private val authPrefs = context.getSharedPreferences("loopi_auth", Context.MODE_PRIVATE)

    val joined = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("joined", emptySet()).orEmpty())
    }
    val posts = mutableStateListOf<GroupPost>().apply {
        addAll(starterPosts)
        addAll(readPosts())
    }

    val myName: String get() = authPrefs.getString("full_name", null)?.ifBlank { null } ?: "You"

    internal val groups: List<StudyGroup> get() = starterGroups

    internal fun group(id: String) = starterGroups.firstOrNull { it.id == id }

    internal fun memberCount(group: StudyGroup) = group.members.size + if (group.id in joined) 1 else 0

    fun toggleJoin(groupId: String) {
        if (groupId in joined) joined.remove(groupId) else joined.add(groupId)
        prefs.edit().putStringSet("joined", joined.toSet()).apply()
    }

    fun addPost(groupId: String, text: String) {
        posts.add(GroupPost(groupId, myName, text.trim(), System.currentTimeMillis()))
        val saved = JSONArray()
        posts.filter { it.postedAt > 0 }.forEach {
            saved.put(JSONObject().put("g", it.groupId).put("a", it.author).put("t", it.text).put("at", it.postedAt))
        }
        prefs.edit().putString("posts", saved.toString()).apply()
    }

    private fun readPosts(): List<GroupPost> = runCatching {
        val array = JSONArray(prefs.getString("posts", "[]"))
        List(array.length()) {
            val item = array.getJSONObject(it)
            GroupPost(item.getString("g"), item.getString("a"), item.getString("t"), item.getLong("at"))
        }
    }.getOrDefault(emptyList())
}

@Composable
fun CommunityScreen(navController: NavHostController, community: CommunityState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar("Community") { navController.popBackStack() } },
        bottomBar = { LoopiBottomNavigation(navController, selected = "profile") }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = LoopiColors.Blue) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Groups") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Discussions") })
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (tab == 0) {
                    item {
                        Text(
                            "Groups and posts are saved on this device until Loopi has a sync server.",
                            color = LoopiColors.Muted,
                            fontSize = 12.sp
                        )
                    }
                    items(community.groups, key = { it.id }) { group ->
                        GroupCard(group, community) { navController.navigate("community_group/${group.id}") }
                    }
                } else {
                    val sorted = community.posts.sortedByDescending { it.postedAt }
                    items(sorted.size) { index ->
                        val post = sorted[index]
                        PostCard(post, community.group(post.groupId)?.name) {
                            navController.navigate("community_group/${post.groupId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: StudyGroup, community: CommunityState, onClick: () -> Unit) {
    val joined = group.id in community.joined
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TopicIconTile(title = group.topic, category = "", size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.name, color = LoopiColors.Navy, fontWeight = FontWeight.Bold)
                Text(
                    "${group.topic} · ${countLabel(community.memberCount(group), "member")}",
                    color = LoopiColors.Muted,
                    fontSize = 12.sp
                )
            }
            if (joined) {
                AssistChip(onClick = onClick, label = { Text("Joined") })
            } else {
                Button(onClick = { community.toggleJoin(group.id) }, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
private fun PostCard(post: GroupPost, groupName: String?, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(post.author)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.author, color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        listOfNotNull(groupName, post.postedAt.takeIf { it > 0 }?.let { formatPostTime(it) }).joinToString(" · "),
                        color = LoopiColors.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(post.text, color = LoopiColors.Ink, fontSize = 14.sp)
        }
    }
}

@Composable
private fun InitialAvatar(name: String) {
    Box(
        Modifier.size(34.dp).background(topicAccent(name).copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = topicAccent(name), fontWeight = FontWeight.Bold)
    }
}

private fun formatPostTime(time: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(time))

@Composable
fun CommunityGroupScreen(navController: NavHostController, community: CommunityState, groupId: String) {
    val group = community.group(groupId)
    if (group == null) {
        navController.popBackStack()
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    val joined = group.id in community.joined
    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar(group.name) { navController.popBackStack() } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TopicIconTile(title = group.topic, category = "", size = 64.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(group.name, color = LoopiColors.Navy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${group.topic} · ${countLabel(community.memberCount(group), "member")}",
                            color = LoopiColors.Muted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        if (joined) {
                            OutlinedButton(onClick = { community.toggleJoin(group.id) }, modifier = Modifier.fillMaxWidth()) { Text("Leave group") }
                        } else {
                            Button(onClick = { community.toggleJoin(group.id) }, modifier = Modifier.fillMaxWidth()) { Text("Join group") }
                        }
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = LoopiColors.Blue) {
                    listOf("About", "Members", "Posts").forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                    }
                }
            }
            when (tab) {
                0 -> item {
                    Text(group.about, color = LoopiColors.Ink, modifier = Modifier.padding(4.dp))
                }
                1 -> {
                    val members = group.members + if (joined) listOf(community.myName + " (you)") else emptyList()
                    items(members) { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            InitialAvatar(member)
                            Spacer(Modifier.width(12.dp))
                            Text(member, color = LoopiColors.Navy, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    item {
                        if (joined) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it.take(280) },
                                    placeholder = { Text("Share a tip or question") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    community.addPost(group.id, draft)
                                    draft = ""
                                }, enabled = draft.isNotBlank()) { Text("Post") }
                            }
                        } else {
                            Text("Join this group to post.", color = LoopiColors.Muted, fontSize = 13.sp)
                        }
                    }
                    val groupPosts = community.posts.filter { it.groupId == group.id }.sortedByDescending { it.postedAt }
                    if (groupPosts.isEmpty()) item { Text("No posts yet.", color = LoopiColors.Muted) }
                    items(groupPosts.size) { index -> PostCard(groupPosts[index], null) {} }
                }
            }
        }
    }
}

/* =========================================================
   FLOW 8 — HELP & SUPPORT
   ========================================================= */

private const val SUPPORT_EMAIL = "loopi.support@example.com"

private val helpFaqs = listOf(
    "Where is my study data saved?" to "Your profile, topics, resources and review history are stored on this device. This version does not sync to a server.",
    "How does the confidence check work?" to "Before Loopi shows the answer you rate how sure you are. Progress compares those ratings with your results, so you can spot topics you only think you know.",
    "What counts as a weak spot?" to "Any topic under 70% accuracy, and any card you answered incorrectly. Use Practice on the Progress screen to review them.",
    "How do I share a study pack?" to "Open Profile > My Study Packs, pick a pack and choose Export or Share. The file includes the topics and their flashcards, so a classmate can import it without an account.",
    "Can I use Loopi offline?" to "Yes. Everything works offline because the study library and progress live on your device."
)

private val appGuides = listOf(
    "Add a topic" to "Topics > + button. Fill in the title, description, category and number of cards, then add flashcards from the topic page.",
    "Run a review" to "Study > choose a topic > Start Quiz. Pick an answer, rate your confidence, then check the answer and read the explanation.",
    "Read your progress" to "Progress shows accuracy by week or month, your weakest topic and your confidence calibration score."
)

@Composable
fun HelpSupportScreen(navController: NavHostController) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar("Help & Support") { navController.popBackStack() } }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HelpAction(Icons.Rounded.BugReport, "Report a problem", Modifier.weight(1f)) {
                    navController.navigate("report_problem")
                }
                HelpAction(Icons.Rounded.Email, "Contact us", Modifier.weight(1f)) {
                    val email = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                        .putExtra(Intent.EXTRA_SUBJECT, "Loopi support")
                    runCatching { context.startActivity(email) }
                }
            }
            SectionTitle("Frequently asked questions")
            helpFaqs.forEach { (question, answer) -> ExpandableCard(question, answer, expanded == question) { expanded = if (expanded == question) null else question } }
            SectionTitle("App guides")
            appGuides.forEach { (title, body) ->
                ExpandableCard(title, body, expanded == title, icon = Icons.AutoMirrored.Rounded.MenuBook) { expanded = if (expanded == title) null else title }
            }
            SectionTitle("Community")
            ExpandableCard("Study groups", "Join a study group to share tips and packs with classmates.", false, icon = Icons.Rounded.Groups) {
                navController.navigate("community")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = LoopiColors.Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun HelpAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).background(LoopiColors.BlueSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = LoopiColors.Blue)
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    body: String,
    expanded: Boolean,
    icon: ImageVector = Icons.AutoMirrored.Rounded.Chat,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = LoopiColors.Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                LoopiChevron()
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(body, color = LoopiColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

private val problemTypes = listOf("App crash", "Wrong progress numbers", "Quiz or flashcards", "Study packs", "Other")

@Composable
fun ReportProblemScreen(navController: NavHostController) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf("") }
    var typeMenu by remember { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    var screenshot by rememberSaveable { mutableStateOf<String?>(null) }
    var showErrors by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> screenshot = uri?.toString() }

    val typeError = showErrors && type.isBlank()
    val descriptionError = showErrors && description.trim().length < 10

    Scaffold(
        containerColor = LoopiColors.Background,
        topBar = { LoopiSimpleTopBar("Report a Problem") { navController.popBackStack() } }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (saved) {
                Card(colors = CardDefaults.cardColors(containerColor = LoopiColors.TealSoft), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Report saved", color = LoopiColors.Teal, fontWeight = FontWeight.Bold)
                        Text(
                            "It's stored on this device. Send it by email so the team can see it.",
                            color = LoopiColors.Ink,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val body = "Type: $type\n\nWhat happened:\n$description\n\nSteps:\n${steps.ifBlank { "-" }}"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                this.type = if (screenshot != null) "image/*" else "text/plain"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                                putExtra(Intent.EXTRA_SUBJECT, "Loopi problem: $type")
                                putExtra(Intent.EXTRA_TEXT, body)
                                screenshot?.let {
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, "Send report")) }
                        }) { Text("Send by email") }
                    }
                }
            }

            Text("Problem type *", color = LoopiColors.Navy, fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(type.ifBlank { "Select a type" }, modifier = Modifier.weight(1f), color = if (type.isBlank()) LoopiColors.Muted else LoopiColors.Ink)
                    Text("⌄", color = LoopiColors.Muted)
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    problemTypes.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeMenu = false })
                    }
                }
            }
            if (typeError) Text("Choose what kind of problem it is", color = LoopiColors.Red, fontSize = 12.sp)

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(1000) },
                label = { Text("What happened? *") },
                isError = descriptionError,
                supportingText = { if (descriptionError) Text("Describe the problem in at least 10 characters") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it.take(1000) },
                label = { Text("Steps to reproduce (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (screenshot == null) "Attach a screenshot (optional)" else "Screenshot attached")
            }
            Button(
                onClick = {
                    showErrors = true
                    if (type.isNotBlank() && description.trim().length >= 10) {
                        saveProblemReport(context, type, description.trim(), steps.trim())
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Submit report", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun saveProblemReport(context: Context, type: String, description: String, steps: String) {
    val prefs = context.getSharedPreferences("loopi_support", Context.MODE_PRIVATE)
    val reports = runCatching { JSONArray(prefs.getString("reports", "[]")) }.getOrDefault(JSONArray())
    reports.put(
        JSONObject()
            .put("type", type)
            .put("description", description)
            .put("steps", steps)
            .put("createdAt", System.currentTimeMillis())
    )
    prefs.edit().putString("reports", reports.toString()).apply()
}

@Composable
fun LoopiSimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { LoopiBackIcon() }
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = LoopiColors.Navy,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(48.dp))
    }
}
