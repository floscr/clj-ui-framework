import * as squint_core from 'squint-cljs/core.js';
import * as str from 'squint-cljs/src/squint/string.js';
import * as clojure_DOT_string from 'squint-cljs/src/squint/string.js';
import * as eu from 'eucalypt';
import * as button from 'ui.button';
import * as ui_DOT_button from 'ui.button';
import * as alert from 'ui.alert';
import * as ui_DOT_alert from 'ui.alert';
import * as badge from 'ui.badge';
import * as ui_DOT_badge from 'ui.badge';
import * as card from 'ui.card';
import * as ui_DOT_card from 'ui.card';
import * as accordion from 'ui.accordion';
import * as ui_DOT_accordion from 'ui.accordion';
import * as table from 'ui.table';
import * as ui_DOT_table from 'ui.table';
import * as dialog from 'ui.dialog';
import * as ui_DOT_dialog from 'ui.dialog';
import * as spinner from 'ui.spinner';
import * as ui_DOT_spinner from 'ui.spinner';
import * as skeleton from 'ui.skeleton';
import * as ui_DOT_skeleton from 'ui.skeleton';
import * as progress from 'ui.progress';
import * as ui_DOT_progress from 'ui.progress';
import * as switch$ from 'ui.switch';
import * as ui_DOT_switch from 'ui.switch';
import * as tooltip from 'ui.tooltip';
import * as ui_DOT_tooltip from 'ui.tooltip';
import * as breadcrumb from 'ui.breadcrumb';
import * as ui_DOT_breadcrumb from 'ui.breadcrumb';
import * as pagination from 'ui.pagination';
import * as ui_DOT_pagination from 'ui.pagination';
import * as form from 'ui.form';
import * as ui_DOT_form from 'ui.form';
import * as sidebar from 'ui.sidebar';
import * as ui_DOT_sidebar from 'ui.sidebar';
import * as icon from 'ui.icon';
import * as ui_DOT_icon from 'ui.icon';
import * as separator from 'ui.separator';
import * as ui_DOT_separator from 'ui.separator';
import * as calendar from 'ui.calendar';
import * as ui_DOT_calendar from 'ui.calendar';
import * as cal_events from 'ui.calendar-events';
import * as ui_DOT_calendar_events from 'ui.calendar-events';
import * as tag_input from 'ui.tag-input';
import * as ui_DOT_tag_input from 'ui.tag-input';
import * as markdown from 'ui.markdown';
import * as ui_DOT_markdown from 'ui.markdown';
var _BANG_page = squint_core.atom("components");
var toggle_theme_BANG_ = function (_e) {
const el1 = document.documentElement;
const current2 = el1.dataset.theme;
el1.dataset.noTransitions = "";
el1.dataset.theme = (((current2 === "dark")) ? ("light") : ("dark"));
return requestAnimationFrame((function () {
return el1.removeAttribute("data-no-transitions");

}));

};
var toggle_sidebar_BANG_ = function (_e) {
const temp__23127__auto__1 = document.querySelector(".sidebar-layout");
if (squint_core.truth_(temp__23127__auto__1)) {
const layout2 = temp__23127__auto__1;
return layout2.toggleAttribute("data-sidebar-open");
};

};
var close_sidebar_BANG_ = function (_e) {
const temp__23127__auto__1 = document.querySelector(".sidebar-layout");
if (squint_core.truth_(temp__23127__auto__1)) {
const layout2 = temp__23127__auto__1;
return layout2.removeAttribute("data-sidebar-open");
};

};
var section = (() => {
const f414 = (function (var_args) {
const args4151 = [];
const len__23317__auto__2 = arguments.length;
let i4163 = 0;
while(true){
if ((i4163 < len__23317__auto__2)) {
args4151.push((arguments[i4163]));
let G__4 = (i4163 + 1);
i4163 = G__4;
continue;
};break;
}
;
const argseq__23554__auto__5 = (((1 < args4151.length)) ? (args4151.slice(1)) : (null));
return f414.cljs$core$IFn$_invoke$arity$variadic((arguments[0]), argseq__23554__auto__5);

});
f414.cljs$core$IFn$_invoke$arity$variadic = (function (title, children) {
const id6 = title.toLowerCase();
return ["section", ({"id": id6, "style": ({"margin-bottom": "2.5rem"})}), ["h3", ({"style": ({"color": "var(--fg-1)", "margin-bottom": "1rem", "border-bottom": "var(--border-0)", "padding-bottom": "0.5rem"})}), title], squint_core.into(["div", ({"style": ({"display": "flex", "flex-direction": "column", "gap": "1rem"})})], children)];

});
f414.cljs$lang$maxFixedArity = 1;
return f414;

})();
var page_header = function (title, subtitle) {
return ["div", ({"style": ({"margin-bottom": "2rem"})}), ["h2", ({"style": ({"margin": "0 0 0.25rem", "color": "var(--fg-0)"})}), title], ((squint_core.truth_(subtitle)) ? (["p", ({"style": ({"margin": "0", "color": "var(--fg-2)", "font-size": "var(--font-sm)"})}), subtitle]) : (null))];

};
var button_variants = ["primary", "secondary", "ghost", "danger"];
var button_sizes = ["sm", "md", "lg"];
var button_demo = function () {
return section("Button", squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], squint_core.map((function (v) {
return button.button(({"variant": v, "on-click": (function (_) {
return console.log(`${"Clicked: "}${v??''}`);

})}), v);

}), button_variants)), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], squint_core.map((function (s) {
return button.button(({"variant": "primary", "size": s}), `${"size "}${s??''}`);

}), button_sizes)), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], squint_core.map((function (v) {
return button.button(({"variant": v, "disabled": true}), `${v??''}${" disabled"}`);

}), button_variants)), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], [button.button(({"variant": "primary", "href": "#"}), "Link primary"), button.button(({"variant": "secondary", "href": "#"}), "Link secondary"), button.button(({"variant": "link"}), "Link button"), button.button(({"variant": "link", "href": "https://example.com"}), "Link with href")]), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], [button.button(({"variant": "primary", "icon-left": "plus"}), "Add item"), button.button(({"variant": "secondary", "icon-right": "arrow-right"}), "Next"), button.button(({"variant": "primary", "icon-left": "download", "icon-right": "arrow-down"}), "Download"), button.button(({"variant": "ghost", "icon-left": "edit"}), "Edit")]), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.75rem", "flex-wrap": "wrap", "align-items": "center"})})], [button.button(({"variant": "primary", "icon": "plus"})), button.button(({"variant": "secondary", "icon": "search"})), button.button(({"variant": "ghost", "icon": "settings"})), button.button(({"variant": "danger", "icon": "trash"})), button.button(({"variant": "primary", "icon": "plus", "size": "sm"})), button.button(({"variant": "primary", "icon": "plus", "size": "lg"}))]));

};
var alert_demo = function () {
return section("Alert", alert.alert(({"variant": "success", "title": "Success!"}), "Your changes have been saved."), alert.alert(({"variant": "warning", "title": "Warning!"}), "Please review before continuing."), alert.alert(({"variant": "danger", "title": "Error!"}), "Something went wrong."), alert.alert(({"variant": "info", "title": "Info"}), "This is an informational alert."), alert.alert(({"title": "Neutral"}), "A neutral alert with no variant."));

};
var badge_demo = function () {
return section("Badge", squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.5rem", "flex-wrap": "wrap", "align-items": "center"})})], [badge.badge(({}), "Default"), badge.badge(({"variant": "secondary"}), "Secondary"), badge.badge(({"variant": "outline"}), "Outline"), badge.badge(({"variant": "success"}), "Success"), badge.badge(({"variant": "warning"}), "Warning"), badge.badge(({"variant": "danger"}), "Danger")]), squint_core.into(["div", ({"style": ({"display": "flex", "gap": "0.5rem", "flex-wrap": "wrap", "align-items": "center"})})], [badge.badge(({"icon-name": "check", "variant": "success"}), "Verified"), badge.badge(({"icon-name": "star"}), "Featured"), badge.badge(({"icon-name": "alert-triangle", "variant": "warning"}), "Caution"), badge.badge(({"icon-name": "clock", "variant": "secondary"}), "Pending")]));

};
var card_demo = function () {
return section("Card", card.card(({}), card.card_header(({}), ["h4", "Card Title"], ["p", "Card description goes here."]), card.card_body(({}), ["p", "This is the card content. It can contain any HTML."]), card.card_footer(({}), button.button(({"variant": "secondary", "size": "sm"}), "Cancel"), button.button(({"variant": "primary", "size": "sm"}), "Save"))), ["h5", "Card List (full dividers)"], card.card_list(({}), card.card_list_item(({}), "Notifications"), card.card_list_item(({}), "Privacy"), card.card_list_item(({}), "Appearance"), card.card_list_item(({}), "Accessibility")), ["h5", "Card List (inset dividers)"], card.card_list(({"divider": "inset"}), card.card_list_item(({}), "Notifications"), card.card_list_item(({}), "Privacy"), card.card_list_item(({}), "Appearance"), card.card_list_item(({}), "Accessibility")));

};
var accordion_demo = function () {
return section("Accordion", ["div", ({"class": "accordion-group"}), accordion.accordion(({"title": "What is this framework?"}), "A cross-target component library."), accordion.accordion(({"title": "How do I use it?", "open": true}), "Just require the namespace and call functions."), accordion.accordion(({"title": "Is it accessible?"}), "Yes, follows ARIA best practices.")]);

};
var table_demo = function () {
return section("Table", table.table(({"headers": ["Name", "Email", "Role", "Status"], "rows": [["Alice Johnson", "alice@example.com", "Admin", "Active"], ["Bob Smith", "bob@example.com", "Editor", "Active"], ["Carol White", "carol@example.com", "Viewer", "Pending"]]})));

};
var dialog_demo = function () {
return section("Dialog", ["p", ({"style": ({"color": "var(--fg-2)", "font-size": "var(--font-sm)"})}), "Click button to open dialog."], button.button(({"variant": "primary", "on-click": (function (_) {
const temp__23127__auto__1 = document.getElementById("demo-dialog");
if (squint_core.truth_(temp__23127__auto__1)) {
const el2 = temp__23127__auto__1;
return el2.showModal();
};

})}), "Open dialog"), dialog.dialog(({"id": "demo-dialog"}), dialog.dialog_header(({}), ["h3", "Dialog Title"], ["p", "Are you sure you want to continue?"]), dialog.dialog_body(({}), ["p", "This action cannot be undone."]), dialog.dialog_footer(({}), button.button(({"variant": "secondary", "size": "sm", "on-click": (function (_) {
return document.getElementById("demo-dialog").close();

})}), "Cancel"), button.button(({"variant": "primary", "size": "sm", "on-click": (function (_) {
return document.getElementById("demo-dialog").close();

})}), "Confirm"))));

};
var spinner_demo = function () {
return section("Spinner", ["div", ({"style": ({"display": "flex", "gap": "1.5rem", "align-items": "center"})}), spinner.spinner(({"size": "sm"})), spinner.spinner(({})), spinner.spinner(({"size": "lg"}))]);

};
var skeleton_demo = function () {
return section("Skeleton", ["div", ({"style": ({"max-width": "400px"})}), skeleton.skeleton(({"variant": "heading"})), skeleton.skeleton(({"variant": "line"})), skeleton.skeleton(({"variant": "line"})), ["div", ({"style": ({"display": "flex", "gap": "1rem", "margin-top": "var(--size-3)"})}), skeleton.skeleton(({"variant": "circle"})), ["div", ({"style": ({"flex": "1"})}), skeleton.skeleton(({"variant": "line"})), skeleton.skeleton(({"variant": "line"}))]]]);

};
var progress_demo = function () {
return section("Progress", progress.progress(({"value": 25})), progress.progress(({"value": 50, "variant": "success"})), progress.progress(({"value": 75, "variant": "warning"})), progress.progress(({"value": 90, "variant": "danger"})));

};
var switch_demo = function () {
return section("Switch", ["div", ({"style": ({"display": "flex", "flex-direction": "column", "gap": "0.75rem"})}), switch$.switch_toggle(({"label": "Notifications", "checked": false})), switch$.switch_toggle(({"label": "Dark mode", "checked": true})), switch$.switch_toggle(({"label": "Disabled off", "disabled": true})), switch$.switch_toggle(({"label": "Disabled on", "checked": true, "disabled": true}))]);

};
var tooltip_demo = function () {
return section("Tooltip", ["div", ({"style": ({"display": "flex", "gap": "1.5rem", "padding-top": "2rem"})}), tooltip.tooltip(({"text": "Save your changes"}), button.button(({"variant": "primary"}), "Save")), tooltip.tooltip(({"text": "Delete this item"}), button.button(({"variant": "danger"}), "Delete")), tooltip.tooltip(({"text": "View profile"}), ["a", ({"href": "#", "style": ({"color": "var(--accent)"})}), "Profile"])]);

};
var breadcrumb_demo = function () {
return section("Breadcrumb", breadcrumb.breadcrumb(({"items": [({"label": "Home", "href": "#"}), ({"label": "Projects", "href": "#"}), ({"label": "Oat Docs", "href": "#"}), ({"label": "Components"})]})));

};
var pagination_demo = function () {
return section("Pagination", pagination.pagination(({"current": 3, "total": 5, "on-click": (function (p) {
return console.log(`${"Page: "}${p??''}`);

})})));

};
var separator_demo = function () {
return section("Separator", ["div", ({"style": ({"max-width": "24rem"})}), ["div", ({"style": ({"display": "flex", "flex-direction": "column", "gap": "0.375rem"})}), ["div", ({"style": ({"font-weight": "500", "line-height": "1"})}), "Clojure UI"], ["div", ({"style": ({"color": "var(--fg-2)", "font-size": "var(--font-sm)"})}), "A cross-target component library"]], ["div", ({"style": ({"margin": "1rem 0"})}), separator.separator(({}))], ["p", ({"style": ({"font-size": "var(--font-sm)"})}), "Build once, render everywhere — Hiccup, Replicant, and Squint."]], ["div", ({"style": ({"display": "flex", "align-items": "center", "gap": "1rem", "height": "1.25rem"})}), ["span", ({"style": ({"font-size": "var(--font-sm)"})}), "Blog"], separator.separator(({"orientation": "vertical"})), ["span", ({"style": ({"font-size": "var(--font-sm)"})}), "Docs"], separator.separator(({"orientation": "vertical"})), ["span", ({"style": ({"font-size": "var(--font-sm)"})}), "Source"]]);

};
var form_demo = function () {
return section("Form", ["form", ({"style": ({"max-width": "480px"})}), form.form_field(({"label": "Name"}), form.form_input(({"type": "text", "placeholder": "Enter your name"}))), form.form_field(({"label": "Email"}), form.form_input(({"type": "email", "placeholder": "you@example.com"}))), form.form_field(({"label": "Password", "hint": "At least 8 characters"}), form.form_input(({"type": "password", "placeholder": "Password"}))), form.form_field(({"label": "Select"}), form.form_select(({"placeholder": "Select an option", "options": [({"value": "a", "label": "Option A"}), ({"value": "b", "label": "Option B"}), ({"value": "c", "label": "Option C"})]}))), form.form_field(({"label": "Message"}), form.form_textarea(({"placeholder": "Your message..."}))), form.form_field(({"label": "Disabled"}), form.form_input(({"type": "text", "placeholder": "Disabled", "disabled": true}))), form.form_field(({"label": "File"}), form.form_file(({}))), form.form_field(({"label": "Date and time"}), form.form_input(({"type": "datetime-local"}))), form.form_field(({"label": "Date"}), form.form_input(({"type": "date"}))), form.form_field(({"label": "Search (icon left)"}), form.form_input(({"type": "text", "placeholder": "Search...", "icon-left": "search"}))), form.form_field(({"label": "URL (both icons)"}), form.form_input(({"type": "text", "placeholder": "example.com", "icon-left": "globe", "icon-right": "check"}))), form.form_checkbox(({"label": "I agree to the terms"})), form.form_radio_group(({"label": "Preference", "radio-name": "pref", "options": [({"value": "a", "label": "Option A"}), ({"value": "b", "label": "Option B"}), ({"value": "c", "label": "Option C"})]})), form.form_field(({"label": "Volume"}), form.form_range(({"min": 0, "max": 100, "value": 50}))), button.button(({"variant": "primary", "attrs": ({"type": "submit"})}), "Submit")], ["div", ({"style": ({"max-width": "480px", "margin-top": "1.5rem"})}), ["h4", ({"style": ({"margin-bottom": "0.75rem"})}), "Input group"], form.form_group(({}), form.form_group_addon(({}), "https://"), form.form_input(({"placeholder": "subdomain"})), button.button(({"variant": "primary", "size": "sm"}), "Go"))], ["div", ({"style": ({"max-width": "480px", "margin-top": "1.5rem"})}), ["h4", ({"style": ({"margin-bottom": "0.75rem"})}), "Validation error"], form.form_field(({"label": "Email", "error": "Please enter a valid email address."}), form.form_input(({"type": "email", "error": true, "value": "invalid-email"})))]);

};
var all_tags = [({"label": "React", "value": "react"}), ({"label": "Next.js", "value": "nextjs"}), ({"label": "TypeScript", "value": "typescript"}), ({"label": "Clojure", "value": "clojure"}), ({"label": "ClojureScript", "value": "clojurescript"}), ({"label": "Squint", "value": "squint"}), ({"label": "Babashka", "value": "babashka"}), ({"label": "Replicant", "value": "replicant"})];
var _BANG_tag_state = squint_core.atom(({"tags": [], "input-value": "", "open": false, "active-index": 0}));
var tag_input_demo = function () {
const map__12 = squint_core.deref(_BANG_tag_state);
const tags3 = squint_core.get(map__12, "tags");
const input_value4 = squint_core.get(map__12, "input-value");
const open5 = squint_core.get(map__12, "open");
const active_index6 = squint_core.get(map__12, "active-index");
const filtered7 = squint_core.vec(tag_input.filter_tags(all_tags, tags3, input_value4));
return section("Tag Input", ["div", ({"style": ({"max-width": "480px"})}), tag_input.tag_input(({"open": open5, "active-index": active_index6, "tags": tags3, "on-clear": (function () {
if (squint_core.truth_(squint_core.seq(input_value4))) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.assoc, "input-value", "", "active-index", 0)} else {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.assoc, "tags", [], "active-index", 0)};
return render_BANG_();

}), "placeholder": "Add frameworks...", "on-focus": (function (_) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.assoc, "open", true);
return render_BANG_();

}), "on-blur": (function (_) {
return setTimeout((function () {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.assoc, "open", false);
return render_BANG_();

}), 150);

}), "on-backspace": (function () {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.update, "tags", (function (ts) {
if (squint_core.truth_(squint_core.seq(ts))) {
return squint_core.vec(squint_core.butlast(ts))} else {
return ts};

}));
return render_BANG_();

}), "on-select": (function (item) {
squint_core.swap_BANG_(_BANG_tag_state, (function (s) {
return squint_core.assoc(squint_core.update(s, "tags", squint_core.conj, item), "input-value", "", "active-index", 0);

}));
return render_BANG_();

}), "on-input": (function (v) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.assoc, "input-value", v, "open", true, "active-index", 0);
return render_BANG_();

}), "on-key-down": (function (key) {
const map__89 = squint_core.deref(_BANG_tag_state);
const active_index10 = squint_core.get(map__89, "active-index");
const filtered11 = squint_core.vec(tag_input.filter_tags(all_tags, squint_core.get(squint_core.deref(_BANG_tag_state), "tags"), squint_core.get(squint_core.deref(_BANG_tag_state), "input-value")));
if ((key === "ArrowDown")) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.update, "active-index", (function (i) {
return squint_core.min((squint_core.count(filtered11) - 1), (i + 1));

}))} else {
if ((key === "ArrowUp")) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.update, "active-index", (function (i) {
return squint_core.max(0, (i - 1));

}))} else {
if ((key === "Enter")) {
const temp__23127__auto__12 = squint_core.get(filtered11, active_index10);
if (squint_core.truth_(temp__23127__auto__12)) {
const item13 = temp__23127__auto__12;
squint_core.swap_BANG_(_BANG_tag_state, (function (s) {
return squint_core.assoc(squint_core.update(s, "tags", squint_core.conj, item13), "input-value", "", "active-index", 0);

}))}} else {
}}};
return render_BANG_();

}), "filtered-items": filtered7, "on-remove": (function (tag) {
squint_core.swap_BANG_(_BANG_tag_state, squint_core.update, "tags", (function (ts) {
return squint_core.vec(squint_core.remove((function (_PERCENT_1) {
return squint_core._EQ_(squint_core.get(_PERCENT_1, "label"), squint_core.get(tag, "label"));

}), ts));

}));
return render_BANG_();

}), "input-value": input_value4}))], ["p", ({"style": ({"color": "var(--fg-2)", "font-size": "var(--font-sm)", "margin-top": "0.5rem"})}), `${"Selected: "}${str.join(", ", squint_core.map("label", tags3))??''}`]);

};
var _BANG_cal_state = squint_core.atom(({"year": 2026, "month": 3, "selected-date": null}));
var sample_calendar_events = [({"title": "Team standup", "date": "2026-03-29", "time-start": "09:00", "time-end": "09:30", "color": "accent"}), ({"title": "Lunch with Alex", "date": "2026-03-29", "time-start": "12:00", "time-end": "13:00", "color": "success"}), ({"title": "Deploy v2.0", "date": "2026-03-29", "time-start": "15:00", "color": "danger"}), ({"title": "Design review", "date": "2026-03-30", "time-start": "10:00", "color": "warning"}), ({"title": "All-day planning", "date": "2026-03-31", "color": null, "done?": true}), ({"title": "Sprint retro", "date": "2026-04-01", "time-start": "14:00", "time-end": "15:00", "color": "accent"}), ({"title": "1:1 with manager", "date": "2026-04-02", "time-start": "11:00", "color": "success"}), ({"title": "Release party", "date": "2026-04-03", "time-start": "17:00", "color": "danger"})];
var calendar_demo = function () {
const map__12 = squint_core.deref(_BANG_cal_state);
const year3 = squint_core.get(map__12, "year");
const month4 = squint_core.get(map__12, "month");
const selected_date5 = squint_core.get(map__12, "selected-date");
const today_str6 = "2026-03-29";
return section("Calendar", ["h5", "Date Picker (interactive)"], ["div", ({"style": ({"display": "flex", "gap": "1.5rem", "flex-wrap": "wrap"})}), calendar.calendar(({"year": year3, "month": month4, "today-str": today_str6, "selected-date": selected_date5, "on-select": (function (d) {
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "selected-date", d);
return render_BANG_();

}), "on-prev-month": (function (_) {
const vec__710 = calendar.prev_month(year3, month4);
const ny11 = squint_core.nth(vec__710, 0, null);
const nm12 = squint_core.nth(vec__710, 1, null);
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "year", ny11, "month", nm12);
return render_BANG_();

}), "on-next-month": (function (_) {
const vec__1316 = calendar.next_month(year3, month4);
const ny17 = squint_core.nth(vec__1316, 0, null);
const nm18 = squint_core.nth(vec__1316, 1, null);
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "year", ny17, "month", nm18);
return render_BANG_();

})}))], ((squint_core.truth_(selected_date5)) ? (["p", ({"style": ({"color": "var(--fg-1)", "font-size": "var(--font-sm)"})}), `${"Selected: "}${selected_date5??''}`]) : (null)), ["h5", "Event Grid"], cal_events.calendar_event_grid(({"events": sample_calendar_events, "month": month4, "on-next-month": (function (_) {
const vec__1922 = calendar.next_month(year3, month4);
const ny23 = squint_core.nth(vec__1922, 0, null);
const nm24 = squint_core.nth(vec__1922, 1, null);
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "year", ny23, "month", nm24);
return render_BANG_();

}), "on-event-click": (function (evt) {
return console.log("Event clicked:", squint_core.get(evt, "title"));

}), "year": year3, "today-str": today_str6, "on-select": (function (d) {
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "selected-date", d);
return render_BANG_();

}), "selected-date": selected_date5, "on-prev-month": (function (_) {
const vec__2528 = calendar.prev_month(year3, month4);
const ny29 = squint_core.nth(vec__2528, 0, null);
const nm30 = squint_core.nth(vec__2528, 1, null);
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "year", ny29, "month", nm30);
return render_BANG_();

})})), ["h5", "Day Ticker"], cal_events.ticker_strip(({"days": [({"date": "2026-03-27", "day-num": 27, "day-label": "Fr"}), ({"date": "2026-03-28", "day-num": 28, "day-label": "Sa"}), ({"date": "2026-03-29", "day-num": 29, "day-label": "Su"}), ({"date": "2026-03-30", "day-num": 30, "day-label": "Mo"}), ({"date": "2026-03-31", "day-num": 31, "day-label": "Tu"}), ({"date": "2026-04-01", "day-num": 1, "day-label": "We"}), ({"date": "2026-04-02", "day-num": 2, "day-label": "Th"}), ({"date": "2026-04-03", "day-num": 3, "day-label": "Fr"})], "today-str": today_str6, "selected": (() => {
const or__23542__auto__31 = selected_date5;
if (squint_core.truth_(or__23542__auto__31)) {
return or__23542__auto__31} else {
return today_str6};

})(), "events": sample_calendar_events, "on-select": (function (d) {
squint_core.swap_BANG_(_BANG_cal_state, squint_core.assoc, "selected-date", d);
return render_BANG_();

})})), ["h5", "Agenda List"], cal_events.agenda_list(({"days": [({"date": "2026-03-29", "label": "Today"}), ({"date": "2026-03-30", "label": "Tomorrow"}), ({"date": "2026-03-31", "label": "Tue"}), ({"date": "2026-04-01", "label": "Wed"}), ({"date": "2026-04-02", "label": "Thu"}), ({"date": "2026-04-03", "label": "Fri"})], "events": sample_calendar_events, "on-event-click": (function (evt) {
return console.log("Agenda event:", squint_core.get(evt, "title"));

})})));

};
var components_page = function () {
return ["div", page_header("Components", "All UI components at a glance."), button_demo(), alert_demo(), badge_demo(), card_demo(), accordion_demo(), table_demo(), dialog_demo(), spinner_demo(), skeleton_demo(), progress_demo(), switch_demo(), tooltip_demo(), breadcrumb_demo(), pagination_demo(), separator_demo(), form_demo(), tag_input_demo()];

};
var icon_categories = [["Navigation", ["home", "menu", "x", "chevron-down", "chevron-up", "chevron-left", "chevron-right", "arrow-down", "arrow-up", "arrow-left", "arrow-right", "external-link"]], ["Actions", ["search", "plus", "minus", "check", "edit", "trash", "download", "upload", "copy", "filter", "link", "refresh"]], ["Objects", ["file", "folder", "image", "mail", "bell", "calendar", "clock", "bookmark", "star", "heart", "inbox", "layers", "package"]], ["UI & System", ["settings", "user", "users", "log-out", "log-in", "eye", "eye-off", "lock", "grid", "list", "layout-dashboard", "monitor", "moon", "sun"]], ["Status", ["alert-triangle", "alert-circle", "info", "circle-check", "circle-x"]], ["Dev & Technical", ["code", "terminal", "database", "globe", "shield", "zap", "book-open", "map-pin"]]];
var icon_card = function (n) {
return ["div", ({"style": ({"display": "flex", "flex-direction": "column", "align-items": "center", "gap": "var(--size-2)", "padding": "var(--size-3)", "border-radius": "var(--radius-md)", "border": "var(--border-0)"})}), icon.icon(({"icon-name": n})), ["span", ({"style": ({"font-size": "var(--font-xs)", "color": "var(--fg-2)", "text-align": "center", "word-break": "break-all"})}), n]];

};
var icon_category_section = function (entry) {
const cat_name1 = squint_core.first(entry);
const icons2 = squint_core.second(entry);
return section(cat_name1, squint_core.into(["div", ({"style": ({"display": "grid", "grid-template-columns": "repeat(auto-fill, minmax(5rem, 1fr))", "gap": "var(--size-4)"})})], squint_core.map(icon_card, icons2)));

};
var _BANG_calendar_docs = squint_core.atom(null);
var load_calendar_docs_BANG_ = function () {
if (squint_core.truth_(squint_core.deref(_BANG_calendar_docs))) {
return null} else {
return fetch("/calendar.md").then((function (r) {
return r.text();

})).then((function (text) {
squint_core.reset_BANG_(_BANG_calendar_docs, text);
return render_BANG_();

}));
};

};
var calendar_page = function () {
load_calendar_docs_BANG_();
return ["div", page_header("Calendar", "Date picker, event grid, ticker strip, and agenda list."), (() => {
const temp__23127__auto__1 = squint_core.deref(_BANG_calendar_docs);
if (squint_core.truth_(temp__23127__auto__1)) {
const md2 = temp__23127__auto__1;
return squint_core.into(["div", ({"class": "md-docs"})], markdown.markdown__GT_hiccup(md2));
};

})(), calendar_demo()];

};
var icons_page = function () {
return ["div", page_header("Icons", `${squint_core.count(icon.icon_names)??''}${" icons based on Lucide. All render as inline SVG with stroke=\"currentColor\"."}`), section("Sizes", squint_core.into(["div", ({"style": ({"display": "flex", "gap": "1.5rem", "align-items": "end"})})], squint_core.map((function (pair) {
const s1 = squint_core.first(pair);
const label2 = squint_core.second(pair);
return ["div", ({"style": ({"display": "flex", "flex-direction": "column", "align-items": "center", "gap": "var(--size-2)"})}), icon.icon(({"icon-name": "star", "size": s1})), ["span", ({"style": ({"font-size": "var(--font-xs)", "color": "var(--fg-2)"})}), label2]];

}), [["sm", "sm"], ["md", "md (default)"], ["lg", "lg"], ["xl", "xl"]]))), squint_core.into(["div"], squint_core.map(icon_category_section, icon_categories))];

};
var sidebar_page = function () {
return ["div", page_header("Sidebar", "A composable sidebar with brand, search, grouped navigation, collapsible sections, and user footer."), section("Example", sidebar.sidebar_layout(({"attrs": ({"style": "border: var(--border-0); border-radius: var(--radius-lg); overflow: hidden; height: 500px;"})}), sidebar.sidebar(({"attrs": ({"style": "height: 100%; position: static;"})}), sidebar.sidebar_header(({}), sidebar.sidebar_brand(({"title": "Acme Inc.", "subtitle": "Enterprise", "icon": "A"})), sidebar.sidebar_search(({"placeholder": "Search..."}))), sidebar.sidebar_content(({}), sidebar.sidebar_group(({"label": "Getting Started"}), sidebar.sidebar_menu(({}), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "download"}), "Installation"), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "folder", "active": true}), "Project Structure"))), sidebar.sidebar_group(({"label": "Building"}), sidebar.sidebar_menu(({}), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "globe"}), "Routing"), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "database", "badge": "New"}), "Data Fetching"), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "layers"}), "Rendering"), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "zap"}), "Caching"), sidebar.sidebar_menu_item(({"href": "#", "icon-name": "eye"}), "Styling"))), sidebar.sidebar_group(({"label": "API Reference"}), sidebar.sidebar_collapsible(({"title": "Components", "open": true}), sidebar.sidebar_menu(({}), sidebar.sidebar_menu_item(({"href": "#"}), "Button"), sidebar.sidebar_menu_item(({"href": "#"}), "Card"), sidebar.sidebar_menu_item(({"href": "#"}), "Dialog"))), sidebar.sidebar_collapsible(({"title": "Functions"}), sidebar.sidebar_menu(({}), sidebar.sidebar_menu_item(({"href": "#"}), "fetch"), sidebar.sidebar_menu_item(({"href": "#"}), "redirect"))))), sidebar.sidebar_footer(({}), sidebar.sidebar_user(({"user-name": "Alice Johnson", "email": "alice@example.com"})))), sidebar.sidebar_layout_main(({}), ["div", ({"style": ({"padding": "2rem"})}), ["h3", ({"style": ({"margin": "0 0 1rem", "color": "var(--fg-0)"})}), "Dashboard"], ["div", ({"style": ({"display": "grid", "grid-template-columns": "repeat(3, 1fr)", "gap": "1rem"})}), ["div", ({"style": ({"aspect-ratio": "16/9", "background": "var(--bg-1)", "border-radius": "var(--radius-lg)", "border": "var(--border-0)"})})], ["div", ({"style": ({"aspect-ratio": "16/9", "background": "var(--bg-1)", "border-radius": "var(--radius-lg)", "border": "var(--border-0)"})})], ["div", ({"style": ({"aspect-ratio": "16/9", "background": "var(--bg-1)", "border-radius": "var(--radius-lg)", "border": "var(--border-0)"})})]], ["div", ({"style": ({"margin-top": "1rem", "min-height": "120px", "background": "var(--bg-1)", "border-radius": "var(--radius-lg)", "border": "var(--border-0)"})})]])))];

};
var component_nav = [({"title": "General", "items": [({"label": "Button", "anchor": "button"}), ({"label": "Badge", "anchor": "badge"}), ({"label": "Card", "anchor": "card"})]}), ({"title": "Forms", "items": [({"label": "Form", "anchor": "form"}), ({"label": "Tag Input", "anchor": "tag-input"}), ({"label": "Switch", "anchor": "switch"})]}), ({"title": "Data Display", "items": [({"label": "Table", "anchor": "table"}), ({"label": "Accordion", "anchor": "accordion"}), ({"label": "Progress", "anchor": "progress"})]}), ({"title": "Feedback", "items": [({"label": "Alert", "anchor": "alert"}), ({"label": "Dialog", "anchor": "dialog"}), ({"label": "Spinner", "anchor": "spinner"}), ({"label": "Skeleton", "anchor": "skeleton"}), ({"label": "Tooltip", "anchor": "tooltip"})]}), ({"title": "Layout", "items": [({"label": "Separator", "anchor": "separator"})]}), ({"title": "Navigation", "items": [({"label": "Breadcrumb", "anchor": "breadcrumb"}), ({"label": "Pagination", "anchor": "pagination"})]})];
var nav_items = [({"id": "components", "label": "Components", "icon-name": "package"}), ({"id": "calendar", "label": "Calendar", "icon-name": "calendar"}), ({"id": "icons", "label": "Icons", "icon-name": "image"}), ({"id": "sidebar", "label": "Sidebar", "icon-name": "layout-dashboard"})];
var navigate_BANG_ = function (page_id) {
return function (_e) {
squint_core.reset_BANG_(_BANG_page, page_id);
return render_BANG_();

};

};
var navigate_to_section_BANG_ = function (anchor) {
return function (_e) {
if (!(squint_core.deref(_BANG_page) === "components")) {
squint_core.reset_BANG_(_BANG_page, "components");
render_BANG_()};
return setTimeout((function () {
const temp__23127__auto__1 = document.getElementById(anchor);
if (squint_core.truth_(temp__23127__auto__1)) {
const el2 = temp__23127__auto__1;
return el2.scrollIntoView(({"behavior": "smooth", "block": "start"}));
};

}), 50);

};

};
var own_port = function () {
const p1 = parseInt(window.location.port, 10);
if (squint_core.truth_(isNaN(p1))) {
return 3002} else {
return p1};

};
var make_targets = function () {
const port1 = own_port();
const base2 = (port1 - 2);
return [({"label": "Hiccup", "port": (base2 + 3)}), ({"label": "Replicant", "port": (base2 + 1)}), ({"label": "Squint", "port": (base2 + 2), "active": true})];

};
var app_sidebar = function (active_page) {
return sidebar.sidebar(({}), sidebar.sidebar_header(({}), sidebar.sidebar_brand(({"title": "Clojure UI Framework", "subtitle": "Squint", "icon": "U"}))), sidebar.sidebar_content(({}), sidebar.sidebar_group(({"label": "Pages"}), squint_core.into(sidebar.sidebar_menu(({})), squint_core.map((function (p__418) {
const map__12 = p__418;
const id3 = squint_core.get(map__12, "id");
const label4 = squint_core.get(map__12, "label");
const icon_name5 = squint_core.get(map__12, "icon-name");
return sidebar.sidebar_menu_item(({"icon-name": icon_name5, "active": squint_core._EQ_(id3, active_page), "on-click": navigate_BANG_(id3)}), label4);

}), nav_items))), sidebar.sidebar_separator(), squint_core.into(sidebar.sidebar_group(({"label": "Components"})), squint_core.map((function (p__419) {
const map__67 = p__419;
const title8 = squint_core.get(map__67, "title");
const items9 = squint_core.get(map__67, "items");
return sidebar.sidebar_collapsible(({"title": title8, "open": true}), squint_core.into(sidebar.sidebar_menu(({})), squint_core.map((function (p__420) {
const map__1011 = p__420;
const label12 = squint_core.get(map__1011, "label");
const anchor13 = squint_core.get(map__1011, "anchor");
return sidebar.sidebar_menu_item(({"on-click": navigate_to_section_BANG_(anchor13)}), label12);

}), items9)));

}), component_nav)), sidebar.sidebar_separator(), sidebar.sidebar_group(({"label": "Targets"}), squint_core.into(sidebar.sidebar_menu(({})), squint_core.map((function (p__421) {
const map__1415 = p__421;
const label16 = squint_core.get(map__1415, "label");
const port17 = squint_core.get(map__1415, "port");
const active18 = squint_core.get(map__1415, "active");
return sidebar.sidebar_menu_item(({"href": `${"http://localhost:"}${port17??''}`, "icon-name": "monitor", "active": active18}), label16);

}), make_targets()))), sidebar.sidebar_separator(), sidebar.sidebar_group(({"label": "Theme"}), sidebar.sidebar_menu(({}), sidebar.sidebar_menu_item(({"icon-name": "sun", "on-click": toggle_theme_BANG_}), "Toggle Dark Mode")))), sidebar.sidebar_footer(({}), sidebar.sidebar_user(({"user-name": "Dev Mode", "email": `${"squint · port "}${own_port()??''}`, "avatar": "sq"}))));

};
var app = function () {
const active_page1 = squint_core.deref(_BANG_page);
return sidebar.sidebar_layout(({}), app_sidebar(active_page1), sidebar.sidebar_overlay(({"on-click": close_sidebar_BANG_})), sidebar.sidebar_layout_main(({}), ["div", ({"style": ({"padding": "2rem", "max-width": "960px"})}), ["div", ({"style": ({"display": "flex", "align-items": "center", "gap": "0.75rem", "margin-bottom": "1rem"})}), sidebar.sidebar_mobile_toggle(({"on-click": toggle_sidebar_BANG_}))], (() => {
const G__4222 = active_page1;
switch (G__4222) {case "components":
return components_page();

break;
case "calendar":
return calendar_page();

break;
case "icons":
return icons_page();

break;
case "sidebar":
return sidebar_page();

break;
default:
return components_page()};

})()]));

};
var render_BANG_ = function () {
return eu.render(app(), document.getElementById("app"));

};
var init_BANG_ = function () {
return render_BANG_();

};
var reload_BANG_ = function () {
return render_BANG_();

};
init_BANG_();

export { breadcrumb_demo, reload_BANG_, _BANG_tag_state, close_sidebar_BANG_, spinner_demo, all_tags, tooltip_demo, skeleton_demo, _BANG_calendar_docs, components_page, sample_calendar_events, toggle_sidebar_BANG_, form_demo, button_demo, _BANG_cal_state, card_demo, icons_page, calendar_page, switch_demo, pagination_demo, separator_demo, badge_demo, page_header, section, calendar_demo, make_targets, navigate_to_section_BANG_, nav_items, dialog_demo, component_nav, init_BANG_, app, render_BANG_, button_variants, table_demo, tag_input_demo, button_sizes, load_calendar_docs_BANG_, progress_demo, alert_demo, toggle_theme_BANG_, own_port, app_sidebar, sidebar_page, navigate_BANG_, accordion_demo, icon_categories, _BANG_page }
