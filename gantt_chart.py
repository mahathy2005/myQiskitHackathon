import matplotlib.pyplot as plt

tasks = [
    "Project Planning",
    "UI / UX Design",
    "SOS System",
    "Voice SOS",
    "Power Button SOS",
    "Trusted Contacts",
    "Live Location",
    "Safety Tips",
    "Fake Call",
    "Maps & Safe Zones",
    "Testing & Debugging",
    "Final Integration",
    "Documentation"
]

start = [0, 1, 2, 3, 3, 2, 4, 4, 5, 5, 6, 7, 7]
duration = [2, 2, 3, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2]

fig, ax = plt.subplots(figsize=(12, 8))

for i, task in enumerate(tasks):
    ax.barh(
        task,
        duration[i],
        left=start[i],
        height=0.6
    )

ax.set_xlabel("Project Timeline (Weeks)")
ax.set_title("SheShield – Project Gantt Chart")

ax.set_xticks(range(10))
ax.set_xticklabels([
    "Week 1",
    "Week 2",
    "Week 3",
    "Week 4",
    "Week 5",
    "Week 6",
    "Week 7",
    "Week 8",
    "Week 9",
    "Week 10"
])

ax.grid(axis="x", linestyle="--", alpha=0.4)
ax.invert_yaxis()

plt.tight_layout()
plt.savefig("SheShield_Gantt_Chart.png", dpi=300)

print("SUCCESS! Gantt chart created: SheShield_Gantt_Chart.png")

plt.show()
