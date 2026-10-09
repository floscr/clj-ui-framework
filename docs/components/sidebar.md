# Sidebar

> An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.

- Type: Component (layout, App shell)
- Namespace: `ui.sidebar`
- Source: `src/ui/sidebar.cljc`
- Public fns: `sidebar-layout-class-list`, `sidebar-layout-classes`, `sidebar-layout`, `sidebar-layout-main`, `sidebar-class-list`, `sidebar-classes`, `sidebar`, `sidebar-header`, `sidebar-brand-class-list`, `sidebar-brand-classes`, `sidebar-brand`, `sidebar-search-class-list`, `sidebar-search-classes`, `sidebar-search`, `sidebar-content`, `sidebar-group-class-list`, `sidebar-group-classes`, `sidebar-group`, `sidebar-menu`, `sidebar-menu-item-class-list`, `sidebar-menu-item-classes`, `sidebar-menu-item`, `sidebar-collapsible`, `sidebar-mobile-toggle-class-list`, `sidebar-mobile-toggle-classes`, `sidebar-mobile-toggle`, `sidebar-overlay`, `sidebar-separator`, `sidebar-footer`, `sidebar-user`
- Keywords: sidebar, app shell, navigation, layout, mobile drawer, nav menu

## When to use

- App-level navigation between sections, with groups and an account block.
- Any page that needs the sidebar plus main layout with a mobile toggle.

## When not to use

- Use tabs or breadcrumb for navigation inside one page.
- Use panels when the sidebar must be resizable.

## Usage

```clojure
(require '[ui.sidebar :as sidebar])

(sidebar/sidebar-layout {}
  (sidebar/sidebar {}
    (sidebar/sidebar-header {} (sidebar/sidebar-brand {:title "Acme" :subtitle "v1.0" :icon "A"}))
    (sidebar/sidebar-content {}
      (sidebar/sidebar-group {:label "Pages"}
        (sidebar/sidebar-menu {}
          (sidebar/sidebar-menu-item {:href "/" :icon-name :home :active true} "Home")
          (sidebar/sidebar-menu-item {:href "/settings" :icon-name :settings :badge "2"} "Settings"))))
    (sidebar/sidebar-footer {} (sidebar/sidebar-user {:user-name "Ada" :email "ada@example.com" :avatar "AL"})))
  (sidebar/sidebar-overlay {})
  (sidebar/sidebar-layout-main {}
    (sidebar/sidebar-mobile-toggle {})
    page-content))
```

## API reference

### `sidebar-layout`

Flex row wrapping the sidebar, overlay and main area.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-layout-main`

Main content area.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar`

The sticky sidebar container (bg-0, right border).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-header`

Top area.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-brand`

Logo block with an icon badge, title and subtitle.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string` | – |  |
| `:subtitle` | `string` | – |  |
| `:icon` | `string` | – | One or two characters. |
| `:href` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-search`

Search input.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:placeholder` | `string` | – |  |
| `:value` | `string` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-content`

Scrollable middle area.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-group`

A labeled nav group.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-menu`

List of menu items.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-menu-item`

A nav row: <a> with :href, <button> otherwise.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:href` | `string` | – |  |
| `:active` | `boolean` | – |  |
| `:icon-name` | `icon keyword` | – |  |
| `:badge` | `string \| number` | – |  |
| `:on-click` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-collapsible`

Collapsible section using details and summary.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string` | – |  |
| `:open` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-mobile-toggle`

Hamburger button, hidden on desktop; toggles data-sidebar-open on the layout.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-overlay`

Backdrop for the mobile drawer; clicking closes it.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-separator`

Divider between groups.

### `sidebar-footer`

Bottom area.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sidebar-user`

Account block with avatar initials, name (:user-name) and email.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:email` | `string` | – |  |
| `:avatar` | `string` | – | One or two characters. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through menu items. |
| Enter / Space | Toggles collapsible sections and the mobile drawer. |

## Accessibility

- Menu items are links or buttons; mark the current one with aria-current via :attrs. Group labels are plain headings.

## Motion

- The mobile drawer slides in over --duration-standard; item hover tints over --duration-fast.

## Responsive behavior

- Below the breakpoint the sidebar becomes an off-canvas drawer with the overlay; the toggle sits in the main area.

## Targets

- **Hiccup (`:clj`)**: Works without JS for navigation; the mobile toggle uses a tiny inline handler in the runtime.
- **Replicant (`:cljs`)**: Pass :on-click on items for client-side routing.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Group labels are sentence case; keep groups to five or six items.
- Spend the accent on the active item only.

## Related

- [Resizable panels](panels.md): Panel groups whose panes trade space by dragging a separator, with collapse, keyboard resizing and persisted sizes.
- [Breadcrumb](breadcrumb.md): Shows where a page sits in a hierarchy; every item but the last is a link.
- [Theme toggle](theme-toggle.md): A three-way segmented control for light, system and dark appearance, hydrated by the JS runtime.
- [Command palette](command.md): A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select.

## Guidance for AI tools

Sidebar: An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
