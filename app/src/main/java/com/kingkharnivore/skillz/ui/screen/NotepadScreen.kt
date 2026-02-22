@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kingkharnivore.skillz.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ListMode { NONE, BULLET, NUMBERED }

@Stable
private data class NotepadEditorPrefs(
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,

    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val sub: Boolean = false,
    val sup: Boolean = false,

    val listMode: Int = 0, // 0 none, 1 bullet, 2 numbered

    // 0 normal, 1 H1, 2 H2, 3 cursive, 4 mono (UI heuristic only)
    val preset: Int = 0
)

private val NotepadEditorPrefsSaver = Saver<NotepadEditorPrefs, Map<String, Any>>(
    save = { p ->
        mapOf(
            "ss" to p.selectionStart,
            "se" to p.selectionEnd,
            "b" to p.bold,
            "i" to p.italic,
            "u" to p.underline,
            "st" to p.strike,
            "sub" to p.sub,
            "sup" to p.sup,
            "lm" to p.listMode,
            "pr" to p.preset
        )
    },
    restore = { m ->
        NotepadEditorPrefs(
            selectionStart = (m["ss"] as? Int) ?: 0,
            selectionEnd = (m["se"] as? Int) ?: 0,
            bold = (m["b"] as? Boolean) ?: false,
            italic = (m["i"] as? Boolean) ?: false,
            underline = (m["u"] as? Boolean) ?: false,
            strike = (m["st"] as? Boolean) ?: false,
            sub = (m["sub"] as? Boolean) ?: false,
            sup = (m["sup"] as? Boolean) ?: false,
            listMode = (m["lm"] as? Int) ?: 0,
            preset = (m["pr"] as? Int) ?: 0
        )
    }
)

@Composable
fun NotepadScreen(
    text: String,                  // stored string: HTML or HTML + Scyra meta comment
    onTextChange: (String) -> Unit, // store the same persisted string
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var prefs by rememberSaveable(stateSaver = NotepadEditorPrefsSaver) {
        mutableStateOf(NotepadEditorPrefs())
    }

    val state = rememberRichTextState()
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    var showListMenu by remember { mutableStateOf(false) }

    // ---------------- Scyra font meta persistence ----------------

    // We store meta inside the persisted string as an HTML comment so it never renders.
    // Example: <!--SCYRA_FONTS:C:0-5,10-12|M:30-40-->
    val META_PREFIX = "<!--SCYRA_FONTS:"
    val META_SUFFIX = "-->"

    data class FontMeta(
        val cursive: List<IntRange> = emptyList(),
        val mono: List<IntRange> = emptyList()
    )

    fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val out = ArrayList<IntRange>()
        var cur = sorted.first()
        for (r in sorted.drop(1)) {
            cur = if (r.first <= cur.last + 1) {
                cur.first..maxOf(cur.last, r.last)
            } else {
                out.add(cur); r
            }
        }
        out.add(cur)
        return out
    }

    fun encodeMeta(meta: FontMeta): String {
        fun enc(list: List<IntRange>): String =
            list.joinToString(",") { "${it.first}-${it.last}" }

        val c = enc(meta.cursive)
        val m = enc(meta.mono)
        return "C:$c|M:$m"
    }

    fun decodeMeta(raw: String): FontMeta {
        // raw format: C:0-5,10-12|M:30-40
        fun parseRanges(part: String): List<IntRange> {
            val payload = part.substringAfter(":", "")
            if (payload.isBlank()) return emptyList()
            return payload.split(",")
                .mapNotNull { token ->
                    val a = token.substringBefore("-", "").toIntOrNull()
                    val b = token.substringAfter("-", "").toIntOrNull()
                    if (a == null || b == null) null else a..b
                }
        }

        val parts = raw.split("|")
        val cPart = parts.firstOrNull { it.startsWith("C:") } ?: "C:"
        val mPart = parts.firstOrNull { it.startsWith("M:") } ?: "M:"
        return FontMeta(
            cursive = mergeRanges(parseRanges(cPart)),
            mono = mergeRanges(parseRanges(mPart))
        )
    }

    fun splitPersisted(persisted: String): Pair<String, FontMeta?> {
        val idx = persisted.lastIndexOf(META_PREFIX)
        if (idx < 0) return persisted to null
        val end = persisted.indexOf(META_SUFFIX, startIndex = idx)
        if (end < 0) return persisted to null

        val metaRaw = persisted.substring(idx + META_PREFIX.length, end).trim()
        val html = (persisted.removeRange(idx, end + META_SUFFIX.length)).trimEnd()
        return html to runCatching { decodeMeta(metaRaw) }.getOrNull()
    }

    fun buildPersisted(html: String, meta: FontMeta): String {
        return html.trimEnd() + "\n" + META_PREFIX + encodeMeta(meta) + META_SUFFIX
    }

    fun captureFontMetaFromState(): FontMeta {
        val spans = state.annotatedString.spanStyles
        val cursive = spans
            .filter { it.item.fontFamily == FontFamily.Cursive }
            .map { it.start..(it.end - 1).coerceAtLeast(it.start) }
        val mono = spans
            .filter { it.item.fontFamily == FontFamily.Monospace }
            .map { it.start..(it.end - 1).coerceAtLeast(it.start) }

        return FontMeta(
            cursive = mergeRanges(cursive),
            mono = mergeRanges(mono)
        )
    }

    suspend fun applyFontMetaToState(meta: FontMeta) {
        // Apply by selecting ranges + toggling the fontFamily.
        // Do this while suppressHistory=true so it doesn't create extra history entries.
        val prevSel = state.selection

        fun applyRange(r: IntRange, family: FontFamily) {
            val start = r.first
            val endExclusive = r.last + 1
            if (start < 0 || endExclusive <= start) return
            state.selection = TextRange(start, endExclusive)
            state.toggleSpanStyle(SpanStyle(fontFamily = family))
        }

        // Apply cursive then mono (order doesn't matter; they shouldn't overlap)
        meta.cursive.forEach { applyRange(it, FontFamily.Cursive) }
        meta.mono.forEach { applyRange(it, FontFamily.Monospace) }

        state.selection = prevSel
    }

    // ---------------- Undo / Redo (persisted snapshots) ----------------
    val history = remember { DocHistory(maxSize = 120) }
    var suppressHistory by remember { mutableStateOf(false) }

    fun snapshotPersisted(): String {
        val html = state.toHtml()
        val meta = captureFontMetaFromState()
        return buildPersisted(html, meta)
    }

    fun setFromPersisted(persisted: String) {
        val (html, meta) = splitPersisted(persisted)
        suppressHistory = true
        state.setHtml(html)
        if (html.contains("Hi! Welcome to Scyra!")) {
            suppressHistory = true

            // H1 line → make cursive
            val text = state.annotatedString.text
            val h1Start = text.indexOf("Hi! Welcome to Scyra!")
            if (h1Start >= 0) {
                state.selection = TextRange(h1Start, h1Start + "Hi! Welcome to Scyra!".length)
                state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Cursive))
            }

            // Flow Log → mono
            val h2Start = text.indexOf("SkratchPad")
            if (h2Start >= 0) {
                state.selection = TextRange(h2Start, h2Start + "SkratchPad".length)
                state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            }

            suppressHistory = false
        }
        scope.launch {
            // Let editor apply HTML first
            delay(30)
            meta?.let { applyFontMetaToState(it) }
            // Restore caret clamp from prefs after re-apply
            val len = state.annotatedString.text.length
            val s = prefs.selectionStart.coerceIn(0, len)
            val e = prefs.selectionEnd.coerceIn(0, len)
            state.selection = TextRange(s, e)
            delay(30)
            suppressHistory = false
        }
    }

    fun undo() {
        val prev = history.undo() ?: return
        setFromPersisted(prev)
    }

    fun redo() {
        val next = history.redo() ?: return
        setFromPersisted(next)
    }

    // ---------------- Load once + start at bottom ----------------
    var didLoadOnce by rememberSaveable { mutableStateOf(false) }
    var didInitialScrollToBottom by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow {
            val cs = state.currentSpanStyle
            val deco = cs.textDecoration
            val sel = state.selection

            val bold = cs.fontWeight == FontWeight.Bold
            val italic = cs.fontStyle == FontStyle.Italic
            val underline = deco?.contains(TextDecoration.Underline) == true
            val strike = deco?.contains(TextDecoration.LineThrough) == true
            val sub = cs.baselineShift == BaselineShift.Subscript
            val sup = cs.baselineShift == BaselineShift.Superscript

            val listMode =
                when {
                    state.isOrderedList -> 2
                    state.isUnorderedList -> 1
                    else -> 0
                }

            Triple(sel, Pair(listMode, cs), listOf(bold, italic, underline, strike, sub, sup))
        }.collect {
            val sel = it.first
            val listMode = it.second.first
            val cs = it.second.second
            val toolFlags = it.third

            val size = cs.fontSize
            val h1On = (size.value >= 24f) && (cs.fontWeight == FontWeight.Bold)
            val h2On = (size.value in 18f..23.99f) && (cs.fontWeight == FontWeight.SemiBold || cs.fontWeight == FontWeight.Medium)
            val monoOn = cs.fontFamily == FontFamily.Monospace
            val cursiveOn = cs.fontFamily == FontFamily.Cursive

            val preset = when {
                h1On -> 1
                h2On -> 2
                cursiveOn -> 3
                monoOn -> 4
                else -> 0
            }

            prefs = prefs.copy(
                selectionStart = sel.start,
                selectionEnd = sel.end,
                listMode = listMode,
                bold = toolFlags[0],
                italic = toolFlags[1],
                underline = toolFlags[2],
                strike = toolFlags[3],
                sub = toolFlags[4],
                sup = toolFlags[5],
                preset = preset
            )
        }
    }

    LaunchedEffect(Unit) {
        if (didLoadOnce) return@LaunchedEffect

        suppressHistory = true

        val (html, meta) = splitPersisted(text)
        state.setHtml(html)
        if (html.contains("Hi! Welcome to Scyra!")) {
            suppressHistory = true

            // H1 line → make cursive
            val text = state.annotatedString.text
            val h1Start = text.indexOf("Hi! Welcome to Scyra!")
            if (h1Start >= 0) {
                state.selection = TextRange(h1Start, h1Start + "Hi! Welcome to Scyra!".length)
                state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Cursive))
            }

            // SkratchPad → mono
            val h2Start = text.indexOf("SkratchPad")
            if (h2Start >= 0) {
                state.selection = TextRange(h2Start, h2Start + "SkratchPad".length)
                state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            }

            suppressHistory = false
        }
        history.resetWith(buildPersisted(html, meta ?: FontMeta()))

        delay(40)
        meta?.let { applyFontMetaToState(it) }

        // restore caret
        val len = state.annotatedString.text.length
        state.selection = TextRange(
            prefs.selectionStart.coerceIn(0, len),
            prefs.selectionEnd.coerceIn(0, len)
        )

        suppressHistory = false
        didLoadOnce = true
    }

    LaunchedEffect(didLoadOnce) {
        if (!didLoadOnce || didInitialScrollToBottom) return@LaunchedEffect

        snapshotFlow { scrollState.maxValue }
            .filter { it >= 0 }
            .first()

        delay(16)
        scrollState.scrollTo(scrollState.maxValue)
        didInitialScrollToBottom = true
    }

    // ---------------- Smooth save (debounced) + coalesced history ----------------
    var saveJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(state) {
        snapshotFlow { state.annotatedString.text }
            .distinctUntilChanged()
            .collect {
                if (suppressHistory) return@collect

                saveJob?.cancel()
                saveJob = scope.launch {
                    delay(450)
                    val persisted = snapshotPersisted()
                    history.push(persisted)
                    history.clearRedo()
                    onTextChange(persisted)
                }
            }
    }

    // ✅ Flush on stop so last keystroke + last formatting survives app death
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                saveJob?.cancel()
                val persisted = snapshotPersisted()
                history.push(persisted)
                history.clearRedo()
                onTextChange(persisted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun runFormatting(action: () -> Unit) {
        if (!suppressHistory) {
            history.push(snapshotPersisted())
            history.clearRedo()
        }
        action()

        saveJob?.cancel()
        saveJob = scope.launch {
            delay(220)
            val persisted = snapshotPersisted()
            history.push(persisted)
            history.clearRedo()
            onTextChange(persisted)
        }
    }

    fun applyH1() = runFormatting {
        state.toggleSpanStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
    }

    fun applyH2() = runFormatting {
        state.toggleSpanStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold))
    }

    fun setFontFamilyExclusive(family: FontFamily?) = runFormatting {
        val ff = state.currentSpanStyle.fontFamily
        if (ff == FontFamily.Cursive) state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Cursive))
        if (ff == FontFamily.Monospace) state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Monospace))
        if (family != null) state.toggleSpanStyle(SpanStyle(fontFamily = family))
    }

    fun applyCursive() = setFontFamilyExclusive(FontFamily.Cursive)
    fun applyMonospace() = setFontFamilyExclusive(FontFamily.Monospace)
    fun applyNormal() = setFontFamilyExclusive(null)

    fun toggleBulletList() = runFormatting { state.toggleUnorderedList() }
    fun toggleNumberedList() = runFormatting { state.toggleOrderedList() }

    // ---------------- Search ----------------
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var matchIndex by remember { mutableStateOf(0) }

    val plainText = state.annotatedString.text
    val matches = remember(searchQuery, plainText) {
        if (searchQuery.isBlank()) emptyList()
        else Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
            .findAll(plainText)
            .map { it.range }
            .toList()
    }

    LaunchedEffect(matches.size) {
        matchIndex = if (matches.isEmpty()) 0 else matchIndex.coerceIn(0, matches.lastIndex)
    }

    fun jumpToMatch(index: Int) {
        if (matches.isEmpty()) return
        val i = index.coerceIn(0, matches.lastIndex)
        matchIndex = i
        val r = matches[i]
        state.selection = TextRange(r.first, r.last + 1)
        focusRequester.requestFocus()
    }

    // ---------------- Active toggle states ----------------
    val cs = state.currentSpanStyle
    val deco = cs.textDecoration

    val boldOn = cs.fontWeight == FontWeight.Bold
    val italicOn = cs.fontStyle == FontStyle.Italic
    val underlineOn = deco?.contains(TextDecoration.Underline) == true
    val strikeOn = deco?.contains(TextDecoration.LineThrough) == true
    val subOn = cs.baselineShift == BaselineShift.Subscript
    val superOn = cs.baselineShift == BaselineShift.Superscript

    val size = cs.fontSize
    val h1On = (size.value >= 24f) && (cs.fontWeight == FontWeight.Bold)
    val h2On = (size.value in 18f..23.99f) && (cs.fontWeight == FontWeight.SemiBold || cs.fontWeight == FontWeight.Medium)

    val cursiveOn = cs.fontFamily == FontFamily.Cursive
    val monoOn = cs.fontFamily == FontFamily.Monospace
    val normalOn = !h1On && !h2On && !cursiveOn && !monoOn

    val listModeUi: ListMode = when {
        state.isOrderedList -> ListMode.NUMBERED
        state.isUnorderedList -> ListMode.BULLET
        else -> ListMode.NONE
    }
    val listOn = listModeUi != ListMode.NONE
    val listIcon = when (listModeUi) {
        ListMode.NUMBERED -> Icons.Default.FormatListNumbered
        else -> Icons.Default.FormatListBulleted
    }

    // ---------------- UI ----------------
    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { searchMode = !searchMode }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }

            IconButton(enabled = history.canUndo, onClick = { undo() }) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }

            IconButton(enabled = history.canRedo, onClick = { redo() }) {
                Icon(Icons.Default.Redo, contentDescription = "Redo")
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                enabled = scrollState.value > 0,
                onClick = { scope.launch { scrollState.animateScrollTo(0) } }
            ) { Icon(Icons.Default.VerticalAlignTop, contentDescription = "Scroll to top") }

            IconButton(
                enabled = scrollState.value < scrollState.maxValue,
                onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
            ) { Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Scroll to bottom") }
        }

        AnimatedVisibility(visible = searchMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; matchIndex = 0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                singleLine = true,
                placeholder = { Text("Search…") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (matches.isNotEmpty()) {
                            Text("${matchIndex + 1}/${matches.size}", style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { jumpToMatch((matchIndex - 1 + matches.size) % matches.size) }) {
                                Icon(Icons.Default.KeyboardArrowUp, null)
                            }
                            IconButton(onClick = { jumpToMatch((matchIndex + 1) % matches.size) }) {
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                        }
                        IconButton(onClick = { searchMode = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatIconToggle(
                    checked = boldOn,
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    onToggle = { runFormatting { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) } }
                )
                FormatIconToggle(
                    checked = italicOn,
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    onToggle = { runFormatting { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) } }
                )
                FormatIconToggle(
                    checked = underlineOn,
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline",
                    onToggle = { runFormatting { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) } }
                )
                FormatIconToggle(
                    checked = strikeOn,
                    icon = Icons.Default.StrikethroughS,
                    contentDescription = "Strikethrough",
                    onToggle = { runFormatting { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) } }
                )
                FormatIconToggle(
                    checked = subOn,
                    icon = Icons.Default.Subscript,
                    contentDescription = "Subscript",
                    onToggle = {
                        runFormatting {
                            state.toggleSpanStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp))
                        }
                    }
                )
                FormatIconToggle(
                    checked = superOn,
                    icon = Icons.Default.Superscript,
                    contentDescription = "Superscript",
                    onToggle = {
                        runFormatting {
                            state.toggleSpanStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp))
                        }
                    }
                )

                Box {
                    Surface(
                        onClick = { showListMenu = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (listOn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = listIcon,
                                contentDescription = "Lists",
                                tint = if (listOn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showListMenu,
                        onDismissRequest = { showListMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.FormatListBulleted, null) } },
                            onClick = { toggleBulletList(); showListMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.FormatListNumbered, null) } },
                            onClick = { toggleNumberedList(); showListMenu = false }
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatTextToggle(checked = h1On, label = "H1") { applyH1() }
                FormatTextToggle(checked = h2On, label = "H2") { applyH2() }
                FormatTextToggle(checked = normalOn, label = "Normal") { applyNormal() }
                FormatTextToggle(checked = cursiveOn, label = "Cursive") { applyCursive() }
                FormatTextToggle(checked = monoOn, label = "Mono") { applyMonospace() }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                RichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = Int.MAX_VALUE,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun FormatTextToggle(
    checked: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val bg = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)

    val tint = if (checked) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onToggle,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun FormatIconToggle(
    checked: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)

    val tint = if (checked) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onToggle,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

@Stable
private class DocHistory(private val maxSize: Int) {
    private val undo = ArrayDeque<String>()
    private val redo = ArrayDeque<String>()

    val canUndo: Boolean get() = undo.size > 1
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun resetWith(doc: String) {
        undo.clear()
        redo.clear()
        undo.addLast(doc)
    }

    fun clearRedo() = redo.clear()

    fun push(doc: String) {
        if (undo.lastOrNull() == doc) return
        undo.addLast(doc)
        if (undo.size > maxSize) undo.removeFirst()
    }

    fun undo(): String? {
        if (undo.size <= 1) return null
        val current = undo.removeLast()
        redo.addLast(current)
        return undo.lastOrNull()
    }

    fun redo(): String? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(next)
        return next
    }
}
