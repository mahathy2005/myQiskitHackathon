from graphviz import Digraph

dot = Digraph("SheShield_Proper_Architecture", format="png")

# PAGE SETTINGS
dot.attr(
    rankdir="TB",
    bgcolor="white",
    pad="0.5",
    nodesep="0.45",
    ranksep="0.7",
    splines="ortho"
)

# DEFAULT NODE
dot.attr(
    "node",
    shape="box",
    style="rounded,filled",
    fillcolor="white",
    color="black",
    fontcolor="black",
    fontname="Arial",
    fontsize="11",
    penwidth="2",
    margin="0.18"
)

# DEFAULT EDGE
dot.attr(
    "edge",
    color="black",
    fontcolor="black",
    fontname="Arial",
    fontsize="9",
    penwidth="1.6",
    arrowsize="0.8"
)


# =========================================================
# USER + MAIN ENTRY
# =========================================================

dot.node("user", "USER", shape="ellipse")

dot.node(
    "main",
    "SHE SHIELD\nMAIN SCREEN\nMainActivity",
    penwidth="3"
)

dot.edge("user", "main", label="opens app")


# =========================================================
# MAIN SCREEN NAVIGATION
# =========================================================

with dot.subgraph(name="cluster_navigation") as c:
    c.attr(
        label="MAIN SCREEN NAVIGATION",
        color="black",
        fontcolor="black",
        fontname="Arial",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("drawer", "Navigation Drawer")
    c.node("quick", "Quick Access")
    c.node("sosbutton", "SOS BUTTON\nLong Press")

dot.edge("main", "drawer", label="menu click")
dot.edge("main", "quick", label="feature click")
dot.edge("main", "sosbutton", label="SOS")


# =========================================================
# NAVIGATION DRAWER
# =========================================================

with dot.subgraph(name="cluster_drawer") as c:
    c.attr(
        label="NAVIGATION DRAWER → SCREENS",
        color="black",
        fontcolor="black",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("home", "Home\nMainActivity")
    c.node("trusted", "Trusted Contacts\nTrustedContactsActivity")
    c.node("live", "Live Location\nLiveLocationActivity")
    c.node("safezone", "Safe Zones\nSafeZonesActivity")
    c.node("history", "Incident History\nIncidentHistoryActivity")
    c.node("profile", "Profile\nProfileActivity")
    c.node("settings", "Settings\nSettingsActivity")
    c.node("about", "About\nAboutActivity")

dot.edge("drawer", "home", label="Home")
dot.edge("drawer", "trusted", label="Trusted Contacts")
dot.edge("drawer", "live", label="Live Location")
dot.edge("drawer", "safezone", label="Safe Zones")
dot.edge("drawer", "history", label="Incident History")
dot.edge("drawer", "profile", label="Profile")
dot.edge("drawer", "settings", label="Settings")
dot.edge("drawer", "about", label="About")


# =========================================================
# QUICK ACCESS
# =========================================================

with dot.subgraph(name="cluster_quick") as c:
    c.attr(
        label="QUICK ACCESS → FEATURES",
        color="black",
        fontcolor="black",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("q_live", "Live Location")
    c.node("q_contacts", "Trusted Contacts")
    c.node("q_safe", "Safe Zone")
    c.node("fakecall", "Fake Call")
    c.node("tips", "Safety Tips")
    c.node("report", "Report Incident")
    c.node("emergency", "Emergency Services")
    c.node("voice", "Voice SOS")

dot.edge("quick", "q_live", label="Live Location")
dot.edge("quick", "q_contacts", label="Trusted Contacts")
dot.edge("quick", "q_safe", label="Safe Zone")
dot.edge("quick", "fakecall", label="Fake Call")
dot.edge("quick", "tips", label="Safety Tips")
dot.edge("quick", "report", label="Report Incident")
dot.edge("quick", "emergency", label="Emergency Services")
dot.edge("quick", "voice", label="Voice SOS")


# =========================================================
# SOS TRIGGERS
# =========================================================

with dot.subgraph(name="cluster_triggers") as c:
    c.attr(
        label="SOS TRIGGERS",
        color="black",
        fontcolor="black",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("longpress", "SOS Button\n3 Second Long Press")
    c.node("voice_trigger", "Voice Command\nHelp / Emergency")
    c.node("power", "Power Button\nMulti Tap")

dot.edge("sosbutton", "longpress")
dot.edge("voice", "voice_trigger")
dot.edge("main", "power", label="background trigger")


# =========================================================
# SOS CONTROLLER
# =========================================================

dot.node(
    "controller",
    "SOS CONTROLLER\nCentral Emergency Processing",
    penwidth="3"
)

dot.edge("longpress", "controller", label="trigger SOS")
dot.edge("voice_trigger", "controller", label="trigger SOS")
dot.edge("power", "controller", label="trigger SOS")


# =========================================================
# SOS PROCESSING
# =========================================================

with dot.subgraph(name="cluster_processing") as c:
    c.attr(
        label="SOS EMERGENCY PROCESSING",
        color="black",
        fontcolor="black",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("permissions", "Permission Manager")
    c.node("gps", "Get Current GPS Location")
    c.node("contact_alert", "Alert Trusted Contacts")
    c.node("record", "Audio / Video Recording")
    c.node("alarm", "Emergency Alarm")
    c.node("notify", "SOS Notification")
    c.node("storage", "Save Incident Data")

dot.edge("controller", "permissions")
dot.edge("permissions", "gps")
dot.edge("permissions", "contact_alert")
dot.edge("permissions", "record")
dot.edge("permissions", "alarm")
dot.edge("permissions", "notify")
dot.edge("permissions", "storage")


# =========================================================
# EXTERNAL OUTPUT
# =========================================================

with dot.subgraph(name="cluster_external") as c:
    c.attr(
        label="EXTERNAL SERVICES / OUTPUT",
        color="black",
        fontcolor="black",
        fontsize="14",
        penwidth="2",
        style="rounded"
    )

    c.node("maps", "GPS / Maps Service")
    c.node("sms", "SMS / Call Service")
    c.node("contacts_out", "Trusted Contacts")
    c.node("media", "Recorded Evidence")
    c.node("notification_out", "Foreground Notification")
    c.node("incident", "Incident History / Storage")

dot.edge("gps", "maps")
dot.edge("contact_alert", "sms")
dot.edge("sms", "contacts_out")
dot.edge("record", "media")
dot.edge("notify", "notification_out")
dot.edge("storage", "incident")


# =========================================================
# RENDER
# =========================================================

dot.render(
    "SheShield_Proper_Architecture",
    cleanup=True
)

print("SUCCESS!")
print("Created: SheShield_Proper_Architecture.png")

