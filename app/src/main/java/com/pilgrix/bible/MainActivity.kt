package com.pilgrix.bible

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101C2C)
private val InkSoft = Color(0xFF1A2B40)
private val Gold = Color(0xFFD9B56D)
private val Paper = Color(0xFFF7F2E8)
private val Muted = Color(0xFF9CA9B9)
private val White = Color(0xFFFFFCF6)
private data class Book(val name: String, val chapters: Int, val group: String)
private val books = listOf(
    Book("Genesis",50,"Old Testament"), Book("Exodus",40,"Old Testament"), Book("Leviticus",27,"Old Testament"),
    Book("Numbers",36,"Old Testament"), Book("Deuteronomy",34,"Old Testament"), Book("Joshua",24,"Old Testament"),
    Book("Judges",21,"Old Testament"), Book("Ruth",4,"Old Testament"), Book("1 Samuel",31,"Old Testament"),
    Book("2 Samuel",24,"Old Testament"), Book("1 Kings",22,"Old Testament"), Book("2 Kings",25,"Old Testament"),
    Book("1 Chronicles",29,"Old Testament"), Book("2 Chronicles",36,"Old Testament"), Book("Ezra",10,"Old Testament"),
    Book("Nehemiah",13,"Old Testament"), Book("Esther",10,"Old Testament"), Book("Job",42,"Old Testament"),
    Book("Psalms",150,"Old Testament"), Book("Proverbs",31,"Old Testament"), Book("Ecclesiastes",12,"Old Testament"),
    Book("Song of Solomon",8,"Old Testament"), Book("Isaiah",66,"Old Testament"), Book("Jeremiah",52,"Old Testament"),
    Book("Lamentations",5,"Old Testament"), Book("Ezekiel",48,"Old Testament"), Book("Daniel",12,"Old Testament"),
    Book("Hosea",14,"Old Testament"), Book("Joel",3,"Old Testament"), Book("Amos",9,"Old Testament"),
    Book("Obadiah",1,"Old Testament"), Book("Jonah",4,"Old Testament"), Book("Micah",7,"Old Testament"),
    Book("Nahum",3,"Old Testament"), Book("Habakkuk",3,"Old Testament"), Book("Zephaniah",3,"Old Testament"),
    Book("Haggai",2,"Old Testament"), Book("Zechariah",14,"Old Testament"), Book("Malachi",4,"Old Testament"),
    Book("Matthew",28,"New Testament"), Book("Mark",16,"New Testament"), Book("Luke",24,"New Testament"),
    Book("John",21,"New Testament"), Book("Acts",28,"New Testament"), Book("Romans",16,"New Testament"),
    Book("1 Corinthians",16,"New Testament"), Book("2 Corinthians",13,"New Testament"), Book("Galatians",6,"New Testament"),
    Book("Ephesians",6,"New Testament"), Book("Philippians",4,"New Testament"), Book("Colossians",4,"New Testament"),
    Book("1 Thessalonians",5,"New Testament"), Book("2 Thessalonians",3,"New Testament"), Book("1 Timothy",6,"New Testament"),
    Book("2 Timothy",4,"New Testament"), Book("Titus",3,"New Testament"), Book("Philemon",1,"New Testament"),
    Book("Hebrews",13,"New Testament"), Book("James",5,"New Testament"), Book("1 Peter",5,"New Testament"),
    Book("2 Peter",3,"New Testament"), Book("1 John",5,"New Testament"), Book("2 John",1,"New Testament"),
    Book("3 John",1,"New Testament"), Book("Jude",1,"New Testament"), Book("Revelation",22,"New Testament")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BibleApp(applicationContext) }
    }
}

@Composable
private fun BibleApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("niv_bible", Context.MODE_PRIVATE) }
    val bibleApi = remember { YouVersionApi(BuildConfig.YVP_APP_KEY) }
    var tab by rememberSaveable { mutableStateOf("Home") }
    var book by rememberSaveable { mutableStateOf(prefs.getString("book", "John") ?: "John") }
    var chapter by rememberSaveable { mutableIntStateOf(prefs.getInt("chapter", 3)) }
    var search by rememberSaveable { mutableStateOf("") }
    var fontSize by rememberSaveable { mutableIntStateOf(prefs.getInt("fontSize", 20)) }
    var darkReader by rememberSaveable { mutableStateOf(prefs.getBoolean("darkReader", false)) }
    var bookmarks by remember { mutableStateOf(prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet()) }
    var picker by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = InkSoft, onBackground = White, onSurface = White, onPrimary = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize()) {
                when (tab) {
                    "Read" -> Reader(book, chapter, fontSize, darkReader, bibleApi, onBack = { tab = "Home" },
                        onPick = { picker = true },
                        onPrev = {
                            val current = books.indexOfFirst { it.name == book }.coerceAtLeast(0)
                            if (chapter > 1) chapter-- else if (current > 0) {
                                book = books[current - 1].name
                                chapter = books[current - 1].chapters
                            }
                            prefs.edit().putString("book", book).putInt("chapter", chapter).apply()
                        },
                        onNext = {
                            val current = books.indexOfFirst { it.name == book }.coerceAtLeast(0)
                            if (chapter < books[current].chapters) chapter++ else if (current < books.lastIndex) {
                                book = books[current + 1].name
                                chapter = 1
                            }
                            prefs.edit().putString("book", book).putInt("chapter", chapter).apply()
                            prefs.edit().putInt("progress", (prefs.getInt("progress", 12) + 1).coerceAtMost(100)).apply()
                        },
                        onFont = { fontSize = it.coerceIn(16,30); prefs.edit().putInt("fontSize",fontSize).apply() },
                        onTheme = { darkReader = it; prefs.edit().putBoolean("darkReader",it).apply() },
                        saved = bookmarks.contains("$book $chapter"),
                        onSave = {
                            val key = "$book $chapter"
                            bookmarks = if (key in bookmarks) bookmarks - key else bookmarks + key
                            prefs.edit().putStringSet("bookmarks",bookmarks).apply()
                        })
                    "Bookmarks" -> SavedScreen(bookmarks) { key ->
                        val parts = key.split(" ")
                        book = parts.dropLast(1).joinToString(" ").ifBlank { "John" }
                        chapter = parts.lastOrNull()?.toIntOrNull() ?: 1
                        prefs.edit().putString("book",book).putInt("chapter",chapter).apply()
                        tab = "Read"
                    }
                    "Plans" -> PlansScreen { tab = "Read" }
                    "Search" -> SearchScreen(search, { search = it }) { chosen ->
                        book = chosen; chapter = 1
                        prefs.edit().putString("book",book).putInt("chapter",chapter).apply()
                        tab = "Read"
                    }
                    else -> HomeScreen(
                        onRead = { tab = "Read" }, onPick = { picker = true },
                        onPlans = { tab = "Plans" }, onSearch = { tab = "Search" },
                        progress = prefs.getInt("progress", 12)
                    )
                }
                if (tab != "Read") BottomBar(tab) { tab = it }
            }
        }
    }

    if (picker) {
        var filter by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { picker = false }, containerColor = InkSoft,
            title = { Text("Choose a book", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 440.dp)) {
                    OutlinedTextField(value = filter, onValueChange = { filter = it }, singleLine = true,
                        placeholder = { Text("Find a book") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    LazyColumn {
                        items(books.filter { it.name.contains(filter, true) }) { item ->
                            Row(Modifier.fillMaxWidth().clickable {
                                book = item.name; chapter = 1
                                prefs.edit().putString("book",book).putInt("chapter",chapter).apply()
                                picker = false; tab = "Read"
                            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = White, fontWeight = FontWeight.SemiBold)
                                    Text(item.chapters.toString() + " chapters · " + item.group, color = Muted, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Gold)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = false }) { Text("Done", color = Gold) } }
        )
    }
}

@Composable
private fun HomeScreen(onRead: () -> Unit, onPick: () -> Unit, onPlans: () -> Unit, onSearch: () -> Unit, progress: Int) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF263B50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoStories, null, tint = Gold, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("THE WORD", color = Gold, fontSize = 10.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                    Text("Holy Bible", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = White) }
            }
        }
        item {
            Text("A quieter place to meet God.", color = White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text("Read slowly. Reflect deeply. Grow daily.", color = Muted, fontSize = 14.sp)
        }
        item {
            Box(Modifier.fillMaxWidth().height(245.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(Color(0x33D9B56D), Color.Transparent)), radius = size.minDimension * .48f, center = Offset(size.width * .5f, size.height * .52f))
                }
                val transition = rememberInfiniteTransition(label = "book-float")
                val y by transition.animateFloat(-4f, 4f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float-y")
                ThreeDBible(Modifier.offset(y = y.dp).size(220.dp, 230.dp).clickable(onClick = onRead))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRead, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                    Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp))
                    Text("Continue reading", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onPick, modifier = Modifier.size(54.dp), shape = RoundedCornerShape(17.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF45556A)), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.GridView, "Browse books", tint = White)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR JOURNEY", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp)); Text("Small steps, lasting faith.", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(progress.toString() + "%", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Gold, trackColor = Color(0xFF2B3A4C))
        }
        item {
            Text("VERSE FOR TODAY", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(InkSoft).padding(20.dp)) {
                Icon(Icons.Default.FormatQuote, null, tint = Gold, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(8.dp))
                Text("“The Lord is my shepherd, I lack nothing.”", color = White, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Text("PSALM 23:1 · NIV", color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Scripture taken from the Holy Bible, NEW INTERNATIONAL VERSION®, NIV®. Copyright © 1973, 1978, 1984, 2011 by Biblica, Inc.® Used by permission. All rights reserved worldwide.", color = Muted, fontSize = 9.sp, lineHeight = 12.sp)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MAKE IT A HABIT", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("A little time in the Word", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onPlans) { Text("View plans", color = Gold) }
            }
        }
    }
}

@Composable
private fun ThreeDBible(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val l = w * .13f; val t = h * .10f; val r = w * .78f; val b = h * .88f
        val d = w * .14f; val s = w * .13f
        val top = Path().apply { moveTo(l,t+h*.07f); lineTo(r,t); lineTo(r+d,t+h*.05f); lineTo(l+s,t+h*.13f); close() }
        val pages = Path().apply { moveTo(r,t); lineTo(r+d,t+h*.05f); lineTo(r+d,b-h*.04f); lineTo(r,b); close() }
        val spine = Path().apply { moveTo(l,t+h*.07f); lineTo(l+s,t+h*.13f); lineTo(l+s,b+h*.01f); lineTo(l,b-h*.06f); close() }
        val cover = Path().apply { moveTo(l+s,t+h*.13f); lineTo(r+d,t+h*.05f); lineTo(r+d,b-h*.04f); lineTo(l+s,b+h*.01f); close() }
        drawRoundRect(Color(0x55000000), topLeft = Offset(l-2,b-h*.02f), size = Size(r+d-l+12,h*.06f))
        drawPath(pages, Brush.horizontalGradient(listOf(Color(0xFFB8A98B), Color(0xFFFFF5DD), Color(0xFFB9A782))))
        for (i in 0..9) {
            val y = t + h*.09f + i*h*.075f
            drawLine(Color(0x6687775B), Offset(r+2,y), Offset(r+d-1,y+h*.004f), strokeWidth = 1f)
        }
        drawPath(top, Brush.linearGradient(listOf(Color(0xFFF8EACB), Color(0xFFBCA77C))))
        drawPath(spine, Brush.horizontalGradient(listOf(Color(0xFF14283B), Color(0xFF263F56))))
        drawPath(cover, Brush.linearGradient(listOf(Color(0xFF263F56), Color(0xFF0E1B2A), Color(0xFF1B3044)), start = Offset(l,t), end = Offset(r,b)))
        drawPath(cover, Gold, style = Stroke(width = 2.2f))
        val inset = Path().apply { moveTo(l+s+w*.045f,t+h*.19f); lineTo(r+d-w*.04f,t+h*.13f); lineTo(r+d-w*.04f,b-h*.12f); lineTo(l+s+w*.045f,b-h*.08f); close() }
        drawPath(inset, Color(0x77D9B56D), style = Stroke(width = 1.2f))
        drawCircle(Gold, radius = w*.035f, center = Offset(w*.51f,h*.42f))
        drawLine(Gold, Offset(w*.51f,h*.46f), Offset(w*.51f,h*.57f), strokeWidth = 2f)
        drawLine(Gold, Offset(w*.46f,h*.50f), Offset(w*.56f,h*.50f), strokeWidth = 2f)
    }
}

@Composable
private fun Reader(
    book: String, chapter: Int, fontSize: Int, dark: Boolean, api: YouVersionApi,
    onBack: () -> Unit, onPick: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onFont: (Int) -> Unit, onTheme: (Boolean) -> Unit, saved: Boolean, onSave: () -> Unit
) {
    val bg = if (dark) Ink else Paper
    val fg = if (dark) White else Color(0xFF26313D)
    val uriHandler = LocalUriHandler.current
    var passage by remember(book, chapter) { mutableStateOf<BiblePassage?>(null) }
    var error by remember(book, chapter) { mutableStateOf<YouVersionApiException?>(null) }
    var loading by remember(book, chapter) { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }
    val currentIndex = books.indexOfFirst { it.name == book }.coerceAtLeast(0)
    val currentBook = books[currentIndex]
    val previousAvailable = chapter > 1 || currentIndex > 0
    val nextAvailable = chapter < currentBook.chapters || currentIndex < books.lastIndex

    LaunchedEffect(book, chapter, api.isConfigured, retryToken) {
        passage = null
        error = null
        if (!api.isConfigured) {
            error = YouVersionApiException(YouVersionFailure.MISSING_KEY,
                "The NIV text connection needs a YouVersion app key. Register this app, make sure NIV access is approved, add YVP_APP_KEY to your Gradle user properties, and rebuild.")
            return@LaunchedEffect
        }
        loading = true
        try {
            passage = api.getChapter(book, chapter)
        } catch (failure: YouVersionApiException) {
            error = failure
        } catch (_: Exception) {
            error = YouVersionApiException(YouVersionFailure.NETWORK, "Something went wrong while loading this chapter. Please try again.")
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = fg) }
            Column(Modifier.weight(1f).clickable(onClick = onPick)) {
                Text("$book $chapter", color = fg, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("NEW INTERNATIONAL VERSION", color = if (dark) Gold else Color(0xFF88734B), fontSize = 9.sp, letterSpacing = 1.2.sp)
            }
            IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark chapter", tint = Gold) }
            var tools by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { tools = true }) { Icon(Icons.Default.TextFields, "Reading settings", tint = fg) }
                DropdownMenu(expanded = tools, onDismissRequest = { tools = false }) {
                    DropdownMenuItem(text = { Text("Smaller text") }, onClick = { onFont(fontSize - 2); tools = false })
                    DropdownMenuItem(text = { Text("Larger text") }, onClick = { onFont(fontSize + 2); tools = false })
                    DropdownMenuItem(text = { Text(if (dark) "Light page" else "Dark page") }, onClick = { onTheme(!dark); tools = false })
                }
            }
        }
        HorizontalDivider(color = if (dark) Color(0xFF2A3A4D) else Color(0xFFEAE0CC))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(book.uppercase(), color = if (dark) Gold else Color(0xFF8C744A), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("Chapter $chapter", color = fg, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            when {
                loading -> Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Gold)
                    Spacer(Modifier.height(14.dp))
                    Text("Opening your chapter…", color = if (dark) Muted else Color(0xFF59616A), fontSize = 14.sp)
                }
                passage != null -> {
                    Text(passage!!.content, color = fg, fontSize = fontSize.sp, lineHeight = (fontSize + 10).sp)
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = if (dark) Color(0xFF2A3A4D) else Color(0xFFD8CDB8))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Scripture taken from the Holy Bible, NEW INTERNATIONAL VERSION®, NIV®. Copyright © 1973, 1978, 1984, 2011 by Biblica, Inc.® Used by permission. All rights reserved worldwide.",
                        color = if (dark) Muted else Color(0xFF756D60), fontSize = 10.sp, lineHeight = 14.sp
                    )
                }
                error != null -> Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (dark) InkSoft else Color(0xFFEFE6D6)).padding(20.dp)
                ) {
                    Icon(
                        when (error!!.failure) {
                            YouVersionFailure.MISSING_KEY, YouVersionFailure.NIV_NOT_ENABLED, YouVersionFailure.UNAUTHORIZED -> Icons.Default.VpnKey
                            YouVersionFailure.NETWORK -> Icons.Default.WifiOff
                            else -> Icons.Default.MenuBook
                        },
                        contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (error!!.failure) {
                            YouVersionFailure.MISSING_KEY -> "Connect your NIV Bible"
                            YouVersionFailure.UNAUTHORIZED -> "Check the app key"
                            YouVersionFailure.NIV_NOT_ENABLED -> "NIV access needs approval"
                            YouVersionFailure.PASSAGE_NOT_FOUND -> "Chapter unavailable"
                            YouVersionFailure.RATE_LIMITED -> "Let's pause for a moment"
                            YouVersionFailure.SERVER -> "Bible service unavailable"
                            YouVersionFailure.NETWORK -> "You're not connected"
                            YouVersionFailure.INVALID_RESPONSE -> "Unexpected response"
                        },
                        color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(error!!.message, color = if (dark) Muted else Color(0xFF59616A), fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(14.dp))
                    if (error!!.failure in setOf(YouVersionFailure.MISSING_KEY, YouVersionFailure.UNAUTHORIZED, YouVersionFailure.NIV_NOT_ENABLED)) {
                        Button(
                            onClick = { uriHandler.openUri("https://platform.youversion.com/") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
                        ) {
                            Text("Open YouVersion Platform", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        OutlinedButton(onClick = { retryToken++ }, shape = RoundedCornerShape(14.dp)) {
                            Text("Try again")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("TAKE A MOMENT", color = if (dark) Gold else Color(0xFF8C744A), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("What stands out to you in this chapter? What could you put into practice today?", color = fg, fontSize = fontSize.sp, lineHeight = (fontSize + 9).sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f), enabled = previousAvailable, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.ChevronLeft, null); Spacer(Modifier.width(4.dp)); Text("Previous")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f), enabled = nextAvailable, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                Text("Next", fontWeight = FontWeight.Bold); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}
@Composable
private fun SavedScreen(saved: Set<String>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Text("Saved for later", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Your bookmarked chapters", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        if (saved.isEmpty()) EmptyState(Icons.Default.BookmarkBorder, "Your bookmarks live here", "Open a chapter and tap the bookmark icon to save it.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(saved.toList().sorted()) { key ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(InkSoft).clickable { onOpen(key) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, null, tint = Gold); Spacer(Modifier.width(12.dp))
                    Text(key, color = White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun PlansScreen(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("Grow a little every day", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Simple reading rhythms for real life.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        PlanCard("01","Meet Jesus","7 days · Start with John","Get to know Jesus through His words and actions.",onStart)
        Spacer(Modifier.height(12.dp)); PlanCard("02","Peace in the middle","5 days · Psalms & Philippians","Make room for prayer, peace, and trust.",onStart)
        Spacer(Modifier.height(12.dp)); PlanCard("03","Faith that grows","7 days · James & Proverbs","Explore how faith shapes everyday choices.",onStart)
    }
}

@Composable
private fun PlanCard(number: String, title: String, meta: String, detail: String, onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(InkSoft).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF304359)), contentAlignment = Alignment.Center) { Text(number, color = Gold, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(meta, color = Gold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(12.dp)); Text(detail, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp)); TextButton(onClick = onStart, contentPadding = PaddingValues(0.dp)) { Text("Start reading  →", color = Gold, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SearchScreen(query: String, onQuery: (String) -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Text("Find your next passage", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search books (e.g. John)") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White,
                focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF45556A),
                focusedLeadingIconColor = Gold, unfocusedLeadingIconColor = Muted,
                focusedPlaceholderColor = Muted, unfocusedPlaceholderColor = Muted))
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books.filter { it.name.contains(query, true) }) { item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(InkSoft).clickable { onOpen(item.name) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = Gold); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(item.name, color = White, fontWeight = FontWeight.SemiBold); Text(item.chapters.toString()+" chapters", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(InkSoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Gold, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(16.dp)); Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp)); Text(detail, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
    }
}

@Composable
private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
    val items = listOf(Triple("Home",Icons.Default.Home,"Home"), Triple("Read",Icons.Default.MenuBook,"Read"), Triple("Plans",Icons.Default.CalendarMonth,"Plans"), Triple("Bookmarks",Icons.Default.BookmarkBorder,"Saved"))
    Row(Modifier.fillMaxWidth().background(Color(0xFF0B1522)).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            val active = selected == item.first
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onSelect(item.first) }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(item.second, null, tint = if (active) Gold else Muted, modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(4.dp)); Text(item.third, color = if (active) Gold else Muted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
