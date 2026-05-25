# swing-lib

Swing UI utilities and reusable components. Provides builder-pattern classes for
constructing Swing interfaces, layout helpers, thread-safe dialog utilities, and
a presentation-model event queue. No external Swing dependencies — uses only JDK
(`javax.swing`, `java.awt`) plus `lib/java-extras`.

Consumed by `game-core` and `game-headed`.

## Package Layout

```
org.triplea.swing/           – Builders, utility classes, custom components
org.triplea.swing.jpanel/    – JPanel layout builders (Flow, Border, Grid, BoxLayout, GridBag)
org.triplea.swing.key.binding/ – Key binding system with modifier support
org.triplea.swing.gestures/  – macOS magnification gesture support
org.triplea.ui.events.queue/ – Presentation-model event queue (MVVM-like)
org.triplea.awt/             – OpenFileUtility (desktop URL/file opener)
```

## Builder Pattern

All 20+ builder classes follow a consistent fluent API:

- Setter methods return `this` for chaining.
- Components are created lazily in `build()`.
- `Preconditions` checks validate state before building.
- Optional fields use `Optional.ofNullable()` for conditional application.

Component builders: `JButtonBuilder`, `JLabelBuilder`, `JTextFieldBuilder`,
`JTextAreaBuilder`, `JComboBoxBuilder<E>`, `JTableBuilder<T>`, `JCheckBoxBuilder`,
`JFrameBuilder`, `JDialogBuilder`, `JScrollPaneBuilder`, `JSplitPaneBuilder`,
`JTabbedPaneBuilder`, `JMenuBuilder`, `JMenuItemBuilder`, `JMenuItemCheckBoxBuilder`.

`DialogBuilder` uses a nested-builder pattern for type-safe confirmation/info dialogs.

`JPanelBuilder` delegates to layout-specific builders (`FlowLayoutBuilder`,
`BorderLayoutBuilder`, `GridLayoutBuilder`, `BoxLayoutBuilder`,
`GridBagLayoutBuilder`) selected by calling the corresponding method
(e.g., `JPanelBuilder.borderLayout()`).

## EDT and Thread Safety

Several classes handle Event Dispatch Thread (EDT) concerns:

- **SwingAction** — `invokeAndWait()`, `invokeAndWaitResult()`, and
  `invokeNowOrLater()` wrap EDT invocation. `of()` factory methods create
  `AbstractAction` instances.
- **EventThreadJOptionPane** — Shows blocking dialogs from non-EDT threads
  using `CountDownLatch`. Handles interrupt cleanup.
- **ViewControllerSwingEventQueue** — UI events publish off-EDT (spawns a new
  thread if called from EDT); controller events publish on-EDT via
  `SwingUtilities.invokeLater()`. Uses `CopyOnWriteArrayList` for listeners.

## Key Binding System (`swing/key/binding/`)

`SwingKeyBinding.addKeyBinding()` registers hotkeys on JFrame/JDialog/JComponent
using `WHEN_IN_FOCUSED_WINDOW` scope. `addKeyBindingWithMetaAndCtrlMasks()` binds
both CTRL+key and META+key for cross-platform support.

Key bindings automatically disable when a text component has focus (tracked via
`KeyboardFocusManager` + `AtomicBoolean` flag) to prevent hotkey triggers during
typing.

Supporting types: `KeyCode` (enum of VK_* constants), `ButtonDownMask` (modifier
enum), `KeyCombination` (immutable KeyCode + ButtonDownMask pair).

## Event Queue (`ui/events/queue/`)

Implements a presentation-model (MVVM-like) pattern with bidirectional
communication between View and Controller:

- **UI → Controller**: publishes full `ViewData` state snapshots.
- **Controller → UI**: publishes `UnaryOperator<ViewData>` mutation functions
  (partial updates without full state replacement).

Interfaces: `ViewClass<>` (UI side), `ViewClassController<>` (controller side),
`ViewData` (marker for immutable presentation models).

`ViewControllerSwingEventQueue` is the concrete implementation managing thread
dispatch.

## Notable Utility Classes

| Class | Purpose |
|---|---|
| `SwingComponents` | Static helpers: dialogs, combo/list models, progress bars, HTML panes, button groups |
| `FileChooser` | Native `FileDialog` wrapper (prefers OS-native over `JFileChooser`); auto-appends extensions, overwrite confirmation |
| `AutoCompletion` | Case-insensitive autocomplete for editable `JComboBox`; extends `PlainDocument` with Unicode normalization |
| `DocumentListenerBuilder` | Debounced (100ms) document change listener; ignores paste events |
| `MouseListenerBuilder` | Fluent builder for all 5 mouse events |
| `KeyTypeValidator` | Real-time text field validation with 200ms debounce; runs validation off-EDT, applies result on-EDT |
| `IntTextField` | Integer-only input with range validation and auto-correction on focus loss |
| `Toast` | Fade-out notification window (1s default); checks translucency support |
| `CollapsiblePanel` | Expandable/collapsible container with toggle button; Mac-specific UI tweaks |
| `WrapLayout` | `FlowLayout` subclass that wraps components to fit container width |
| `ScrollableJPanel` | `Scrollable` JPanel that stretches horizontally but not vertically (pairs with `WrapLayout`) |
| `ButtonColumn` | Renders a table column as clickable buttons (implements both `TableCellRenderer` and `TableCellEditor`) |
| `JEditorPaneWithClickableLinks` | HTML editor pane that opens hyperlinks via `OpenFileUtility` |
| `SettingPersistence` | Interface contract for boolean setting storage |
| `BorderBuilder` | `EmptyBorder` creation with top/left/bottom/right |
| `ProgressDialog` / `ProgressWindow` | Modal and non-modal indeterminate progress indicators |

## Platform-Specific Behavior

- `CollapsiblePanel` adjusts button appearance for native Mac look-and-feel.
- `FileChooser` uses `FileDialog` (native) instead of `JFileChooser`.
- `SwingKeyBinding` binds both CTRL and META modifiers for Mac compatibility.
- `Gestures` provides macOS magnification (pinch-to-zoom) listener support.
