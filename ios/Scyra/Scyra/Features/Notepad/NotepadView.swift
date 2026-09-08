import Combine
import SwiftUI
import UIKit

struct NotepadView: View {
    @StateObject private var editor = NotepadEditorController()
    @State private var searchVisible = false
    @State private var searchQuery = ""
    @State private var currentMatch = 0

    var body: some View {
        VStack(spacing: 0) {
            actionToolbar

            if searchVisible {
                searchBar
                    .transition(.move(edge: .top).combined(with: .opacity))
            }

            formattingToolbar

            NotepadTextEditor(controller: editor)
                .padding(.horizontal, 16)
                .padding(.bottom, 28)
                .accessibilityLabel("Notepad editor area")
                .accessibilityIdentifier("notepad-editor")
        }
        .background(ScyraColors.background)
        .animation(.easeInOut(duration: 0.18), value: searchVisible)
        .onChange(of: searchQuery) { _, _ in currentMatch = 0 }
    }

    private var actionToolbar: some View {
        HStack(spacing: 8) {
            toolbarButton("magnifyingglass", label: "Search", selected: searchVisible) {
                searchVisible.toggle()
                if !searchVisible { searchQuery = ""; currentMatch = 0 }
            }
            toolbarButton("arrow.uturn.backward", label: "Undo", enabled: editor.canUndo, action: editor.undo)
            toolbarButton("arrow.uturn.forward", label: "Redo", enabled: editor.canRedo, action: editor.redo)
            Spacer()
            toolbarButton("arrow.up.to.line", label: "Scroll to top", action: editor.scrollToTop)
            toolbarButton("arrow.down.to.line", label: "Scroll to bottom", action: editor.scrollToBottom)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Notepad actions")
    }

    private var searchBar: some View {
        HStack(spacing: 4) {
            TextField("Search notes", text: $searchQuery)
                .textFieldStyle(.plain)
                .submitLabel(.search)
                .onSubmit { selectMatch(offset: 0) }

            if !matches.isEmpty {
                Text("\(currentMatch + 1) / \(matches.count)")
                    .font(.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .monospacedDigit()
                toolbarButton("chevron.up", label: "Previous match") { selectMatch(offset: -1) }
                toolbarButton("chevron.down", label: "Next match") { selectMatch(offset: 1) }
            }

            toolbarButton("xmark", label: "Close search") {
                searchVisible = false
                searchQuery = ""
                currentMatch = 0
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 4)
        .frame(minHeight: 50)
        .overlay(
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .stroke(ScyraColors.textMuted, lineWidth: 1)
        )
        .padding(.horizontal, 12)
    }

    private var formattingToolbar: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                toolbarButton("bold", label: "Bold", selected: editor.bold, action: editor.toggleBold)
                toolbarButton("italic", label: "Italic", selected: editor.italic, action: editor.toggleItalic)
                toolbarButton("underline", label: "Underline", selected: editor.underline, action: editor.toggleUnderline)
                toolbarButton("strikethrough", label: "Strikethrough", selected: editor.strikethrough, action: editor.toggleStrikethrough)
                toolbarButton("textformat.subscript", label: "Subscript", selected: editor.subscripted, action: editor.toggleSubscript)
                toolbarButton("textformat.superscript", label: "Superscript", selected: editor.superscripted, action: editor.toggleSuperscript)
            }

            HStack(spacing: 8) {
                presetButton("H1", preset: .heading1)
                presetButton("H2", preset: .heading2)
                presetButton("Normal", preset: .normal)
                presetButton("Cursive", preset: .cursive)
                presetButton("Mono", preset: .monospace)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Notepad formatting")
    }

    private func toolbarButton(
        _ systemImage: String,
        label: String,
        selected: Bool = false,
        enabled: Bool = true,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            ScyraCanonicalIcon(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(selected ? ScyraColors.secondaryGold : ScyraColors.textSecondary)
                .frame(width: 40, height: 40)
                .background(selected ? ScyraColors.secondaryContainer.opacity(0.70) : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.35)
        .accessibilityLabel(label)
        .accessibilityValue(selected ? "On" : "Off")
    }

    private func presetButton(_ label: String, preset: NotepadTextPreset) -> some View {
        let selected = editor.preset == preset
        return Button { editor.apply(preset) } label: {
            Text(label)
                .font(.caption.weight(selected ? .semibold : .regular))
                .foregroundStyle(selected ? ScyraColors.secondaryGold : ScyraColors.textSecondary)
                .padding(.horizontal, 10)
                .frame(height: 40)
                .background(selected ? ScyraColors.secondaryContainer.opacity(0.70) : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityValue(selected ? "On" : "Off")
    }

    private var matches: [NSRange] { editor.matches(for: searchQuery) }

    private func selectMatch(offset: Int) {
        let found = matches
        guard !found.isEmpty else { return }
        currentMatch = (currentMatch + offset + found.count) % found.count
        editor.selectAndReveal(found[currentMatch])
    }
}

private struct NotepadTextEditor: UIViewRepresentable {
    @ObservedObject var controller: NotepadEditorController

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.delegate = controller
        view.allowsEditingTextAttributes = true
        view.alwaysBounceVertical = true
        view.keyboardDismissMode = .interactive
        view.textContainerInset = UIEdgeInsets(top: 14, left: 12, bottom: 14, right: 12)
        view.textContainer.lineFragmentPadding = 4
        view.adjustsFontForContentSizeCategory = true
        view.layer.cornerRadius = 20
        view.layer.masksToBounds = true
        controller.connect(view)
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        view.backgroundColor = UIColor(ScyraColors.surfaceVariant.opacity(0.35))
        view.tintColor = UIColor(ScyraColors.primary)
        if view.attributedText.length == 0, view.typingAttributes[.font] == nil {
            view.typingAttributes[.font] = UIFont.preferredFont(forTextStyle: .body)
            view.typingAttributes[.foregroundColor] = UIColor(ScyraColors.textPrimary)
        }
    }
}

private enum NotepadTextPreset: Equatable {
    case heading1, heading2, normal, cursive, monospace
}

/// Android's `NotepadRepository.DEFAULT_WELCOME_HTML`, expressed as native attributed text.
/// An explicitly saved empty document remains empty; this is only used before the first save.
@MainActor
enum NotepadWelcomeDocument {
    static let plainText = """
    SkratchPad

    Hi! Welcome to Scyra!

    This is your SkratchPad — a place to plan your next Flow,
    capture your thoughts, and record the progress you earn.

    Sketch what’s ahead.
    Reflect on what’s done.

    Write freely.
    Design your focus.
    Build your momentum.
    This is your time.
    """

    static func make() -> NSAttributedString {
        let result = NSMutableAttributedString(
            string: plainText,
            attributes: [
                .font: UIFont.preferredFont(forTextStyle: .body),
                .foregroundColor: UIColor(ScyraColors.textPrimary)
            ]
        )
        let source = plainText as NSString
        let heading = source.range(of: "SkratchPad")
        let greeting = source.range(of: "Hi! Welcome to Scyra!")
        result.addAttribute(
            .font,
            value: UIFont.monospacedSystemFont(ofSize: 20, weight: .semibold),
            range: heading
        )
        result.addAttribute(
            .font,
            value: UIFont(name: ScyraTypography.FontName.appTitle, size: 26)
                ?? UIFont.italicSystemFont(ofSize: 26),
            range: greeting
        )
        return result
    }
}

@MainActor
private final class NotepadEditorController: NSObject, ObservableObject, UITextViewDelegate {
    @Published private(set) var canUndo = false
    @Published private(set) var canRedo = false
    @Published private(set) var bold = false
    @Published private(set) var italic = false
    @Published private(set) var underline = false
    @Published private(set) var strikethrough = false
    @Published private(set) var subscripted = false
    @Published private(set) var superscripted = false
    @Published private(set) var preset: NotepadTextPreset = .normal
    @Published private(set) var plainText = ""

    private weak var textView: UITextView?
    private let storageKey = "scyra.notepad.rtf.v1"

    func connect(_ view: UITextView) {
        guard textView == nil else { return }
        textView = view
        if UserDefaults.standard.object(forKey: storageKey) != nil {
            if let data = UserDefaults.standard.data(forKey: storageKey),
               let document = try? NSAttributedString(
                   data: data,
                   options: [.documentType: NSAttributedString.DocumentType.rtf],
                   documentAttributes: nil
               ) {
                view.attributedText = document
            } else {
                view.attributedText = NSAttributedString(string: "", attributes: defaultAttributes)
            }
        } else {
            view.attributedText = NotepadWelcomeDocument.make()
        }
        view.typingAttributes = defaultAttributes
        refreshState()
        DispatchQueue.main.async { [weak self, weak view] in
            guard let self, let view else { return }
            self.scrollToBottom()
            view.undoManager?.removeAllActions()
            self.refreshState()
        }
    }

    func textViewDidChange(_ textView: UITextView) {
        persist()
        refreshState()
    }

    func textViewDidChangeSelection(_ textView: UITextView) {
        refreshState()
    }

    func undo() {
        textView?.undoManager?.undo()
        persistAndRefresh()
    }

    func redo() {
        textView?.undoManager?.redo()
        persistAndRefresh()
    }

    func scrollToTop() {
        guard let textView else { return }
        textView.setContentOffset(CGPoint(x: 0, y: -textView.adjustedContentInset.top), animated: true)
    }

    func scrollToBottom() {
        guard let textView else { return }
        let bottom = max(-textView.adjustedContentInset.top, textView.contentSize.height - textView.bounds.height + textView.adjustedContentInset.bottom)
        textView.setContentOffset(CGPoint(x: 0, y: bottom), animated: true)
    }

    func toggleBold() { toggleFontTrait(.traitBold) }
    func toggleItalic() { toggleFontTrait(.traitItalic) }
    func toggleUnderline() { toggleAttribute(.underlineStyle, onValue: NSUnderlineStyle.single.rawValue) }
    func toggleStrikethrough() { toggleAttribute(.strikethroughStyle, onValue: NSUnderlineStyle.single.rawValue) }

    func toggleSubscript() {
        setBaseline(subscripted ? 0 : -4)
    }

    func toggleSuperscript() {
        setBaseline(superscripted ? 0 : 4)
    }

    func apply(_ preset: NotepadTextPreset) {
        let font: UIFont
        switch preset {
        case .heading1:
            font = .systemFont(ofSize: 26, weight: .bold)
        case .heading2:
            font = .systemFont(ofSize: 20, weight: .semibold)
        case .normal:
            font = .preferredFont(forTextStyle: .body)
        case .cursive:
            font = UIFont(name: ScyraTypography.FontName.appTitle, size: 20) ?? .italicSystemFont(ofSize: 20)
        case .monospace:
            font = .monospacedSystemFont(ofSize: 16, weight: .regular)
        }
        applyAttributes([.font: font])
    }

    func matches(for query: String) -> [NSRange] {
        guard !query.isEmpty else { return [] }
        let source = plainText as NSString
        var searchRange = NSRange(location: 0, length: source.length)
        var result: [NSRange] = []
        while searchRange.length > 0 {
            let match = source.range(of: query, options: [.caseInsensitive], range: searchRange)
            guard match.location != NSNotFound else { break }
            result.append(match)
            let next = match.location + match.length
            searchRange = NSRange(location: next, length: source.length - next)
        }
        return result
    }

    func selectAndReveal(_ range: NSRange) {
        guard let textView else { return }
        textView.selectedRange = range
        textView.scrollRangeToVisible(range)
        textView.becomeFirstResponder()
    }

    private var defaultAttributes: [NSAttributedString.Key: Any] {
        [
            .font: UIFont.preferredFont(forTextStyle: .body),
            .foregroundColor: UIColor(ScyraColors.textPrimary)
        ]
    }

    private func toggleFontTrait(_ trait: UIFontDescriptor.SymbolicTraits) {
        guard let textView else { return }
        let attributes = attributesAtSelection(in: textView)
        let current = (attributes[.font] as? UIFont) ?? UIFont.preferredFont(forTextStyle: .body)
        var traits = current.fontDescriptor.symbolicTraits
        if traits.contains(trait) { traits.remove(trait) } else { traits.insert(trait) }
        let descriptor = current.fontDescriptor.withSymbolicTraits(traits) ?? current.fontDescriptor
        applyAttributes([.font: UIFont(descriptor: descriptor, size: current.pointSize)])
    }

    private func toggleAttribute(_ key: NSAttributedString.Key, onValue: Any) {
        guard let textView else { return }
        let attributes = attributesAtSelection(in: textView)
        let isOn = (attributes[key] as? Int ?? 0) != 0
        applyAttributes([key: isOn ? 0 : onValue])
    }

    private func setBaseline(_ value: CGFloat) {
        applyAttributes([.baselineOffset: value])
    }

    private func applyAttributes(_ attributes: [NSAttributedString.Key: Any]) {
        guard let textView else { return }
        if textView.selectedRange.length == 0 {
            var typing = textView.typingAttributes
            attributes.forEach { typing[$0.key] = $0.value }
            textView.typingAttributes = typing
        } else {
            textView.textStorage.beginEditing()
            attributes.forEach { textView.textStorage.addAttribute($0.key, value: $0.value, range: textView.selectedRange) }
            textView.textStorage.endEditing()
        }
        persistAndRefresh()
        textView.becomeFirstResponder()
    }

    private func attributesAtSelection(in view: UITextView) -> [NSAttributedString.Key: Any] {
        if view.selectedRange.length == 0 { return view.typingAttributes }
        guard view.attributedText.length > 0 else { return defaultAttributes }
        let location = min(view.selectedRange.location, view.attributedText.length - 1)
        return view.attributedText.attributes(at: location, effectiveRange: nil)
    }

    private func persistAndRefresh() {
        persist()
        refreshState()
    }

    private func persist() {
        guard let textView,
              let data = try? textView.attributedText.data(
                  from: NSRange(location: 0, length: textView.attributedText.length),
                  documentAttributes: [.documentType: NSAttributedString.DocumentType.rtf]
              ) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }

    private func refreshState() {
        guard let textView else { return }
        let attributes = attributesAtSelection(in: textView)
        let font = (attributes[.font] as? UIFont) ?? UIFont.preferredFont(forTextStyle: .body)
        let traits = font.fontDescriptor.symbolicTraits
        bold = traits.contains(.traitBold)
        italic = traits.contains(.traitItalic)
        underline = (attributes[.underlineStyle] as? Int ?? 0) != 0
        strikethrough = (attributes[.strikethroughStyle] as? Int ?? 0) != 0
        let baseline = attributes[.baselineOffset] as? CGFloat ?? 0
        subscripted = baseline < 0
        superscripted = baseline > 0
        preset = resolvePreset(font)
        plainText = textView.text ?? ""
        canUndo = textView.undoManager?.canUndo == true
        canRedo = textView.undoManager?.canRedo == true
    }

    private func resolvePreset(_ font: UIFont) -> NotepadTextPreset {
        if font.fontName == ScyraTypography.FontName.appTitle { return .cursive }
        if font.fontDescriptor.symbolicTraits.contains(.traitMonoSpace) { return .monospace }
        if font.pointSize >= 24, font.fontDescriptor.symbolicTraits.contains(.traitBold) { return .heading1 }
        if font.pointSize >= 18 { return .heading2 }
        return .normal
    }
}

#Preview { NotepadView() }
