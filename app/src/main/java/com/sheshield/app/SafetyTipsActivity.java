package com.sheshield.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SafetyTipsActivity extends AppCompatActivity {

    private FrameLayout heartOverlay;
    private final Random random = new Random();
    private long lastHeartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_tips);

        heartOverlay = findViewById(R.id.heartOverlay);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        RecyclerView recyclerView = findViewById(R.id.rvSafetyTips);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            List<YouTubeVideo> videoList = getSampleVideos();
            YouTubeVideoAdapter adapter = new YouTubeVideoAdapter(videoList, getLifecycle());
            recyclerView.setAdapter(adapter);

            // Listen for scroll events to trigger floating hearts
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // Spawn tiny hearts when scrolling down
                    if (dy > 10) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastHeartTime > 250) { // Throttle heart rate
                            spawnTinyHeart();
                            lastHeartTime = currentTime;
                        }
                    }
                }
            });
        }
    }

    // Spawns a tiny floating heart that rises up and fades out
    public void spawnTinyHeart() {
        if (heartOverlay == null) return;

        ImageView heart = new ImageView(this);
        heart.setImageResource(R.drawable.ic_heart);

        // Very small particle size (14dp - 22dp)
        int size = (int) (14 + random.nextInt(9) * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

        // Random horizontal spawn position across screen width
        int width = heartOverlay.getWidth();
        if (width <= 0) width = 800;
        params.leftMargin = random.nextInt(Math.max(1, width - size));
        params.topMargin = heartOverlay.getHeight() - 150; // Spawn near bottom

        heart.setLayoutParams(params);
        heartOverlay.addView(heart);

        // Float motion: Rise upward + slight side drift + fade out
        float startY = params.topMargin;
        float endY = startY - (300 + random.nextInt(250));
        float driftX = (random.nextBoolean() ? 1 : -1) * (30 + random.nextInt(50));

        ObjectAnimator moveY = ObjectAnimator.ofFloat(heart, "translationY", 0f, endY - startY);
        ObjectAnimator moveX = ObjectAnimator.ofFloat(heart, "translationX", 0f, driftX);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(heart, "alpha", 1f, 0f);

        moveY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new AccelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveY, moveX, alpha);
        animatorSet.setDuration(1200 + random.nextInt(400));

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                heartOverlay.removeView(heart);
            }
        });

        animatorSet.start();
    }

    private List<YouTubeVideo> getSampleVideos() {
        List<YouTubeVideo> list = new ArrayList<>();

        // 1. STRIKING TECHNIQUES
        list.add(new YouTubeVideo(
                "n9qiScyfe4Y",
                "STRIKING TECHNIQUE",
                "Palm Heel & Elbow Strikes",
                "Target vulnerable facial areas (nose, chin, jaw). Strike upward with the heel of your palm or drive your elbow horizontally to stun an attacker instantly."
        ));

        // 2. WRIST RELEASE
        list.add(new YouTubeVideo(
                "nz-3my_LbUY",
                "GRAB ESCAPE",
                "Single & Double Wrist Release",
                "Locate the attacker's thumb gap, step into the stance for leverage, and snap your forearm back toward your own shoulder to break free."
        ));

        // 3. REAR BEAR HUG
        list.add(new YouTubeVideo(
                "R9Tlnvo1ml8",
                "REAR ATTACK DEFENSE",
                "Bear Hug & Choke Release",
                "Lower your center of gravity immediately to prevent being lifted. Stomp firmly on the attacker's instep, drive elbows into ribs, and slip away."
        ));

        // 4. HAIR PULL ESCAPE
        list.add(new YouTubeVideo(
                "zWidY2sS5ao",
                "HAIR GRAB COUNTER",
                "Hair Pull Disengagement",
                "Trap the attacker's hand against your scalp using both hands to neutralize pain, then pivot your hips to twist their wrist until they release."
        ));

        // 5. LOWER BODY COUNTERS
        list.add(new YouTubeVideo(
                "M4_8PoRQP8w",
                "KNEE & GROIN STRIKES",
                "Close-Range Knee Counter",
                "Pull down on the attacker's shoulders or neck to bring their posture forward while driving a powerful knee straight into the groin or torso."
        ));

        // 6. GROUND DEFENSE
        list.add(new YouTubeVideo(
                "15csy6-0mZI",
                "GROUND DEFENSE",
                "Escaping Being Pinned",
                "Protect your face and throat. Bridge your hips upward forcefully, sweep the attacker's knee out, and push off the ground to regain a standing position."
        ));

        // 7. SITUATIONAL AWARENESS
        list.add(new YouTubeVideo(
                "lM-hidRprak",
                "PREVENTION & AWARENESS",
                "De-escalation & Safe Distance",
                "Maintain a 6-foot safety perimeter, project confidence through open defensive hand placement, and give firm verbal commands to deter threats."
        ));

        // 8. VULNERABLE TARGETS
        list.add(new YouTubeVideo(
                "THoI7EaS-ho",
                "VULNERABLE TARGETS",
                "Nose, Eyes & Throat Target Strikes",
                "Focus on soft tissue areas that require minimal physical power. Use finger spears to eyes or open palm strikes to the chin to create exit windows."
        ));

        // 9. 360-DEGREE SCAN
        list.add(new YouTubeVideo(
                "nGaOcKG7U2I",
                "SITUATIONAL SCAN",
                "360-Degree Environmental Awareness",
                "Practice active environmental scanning when walking alone at night. Identify exits, potential hazard spots, and safe zones ahead of time."
        ));

        // 10. EVERYDAY OBJECT DEFENSE
        list.add(new YouTubeVideo(
                "_VXy2gMtohQ",
                "IMPROVISED DEFENSE",
                "Defending with Everyday Objects",
                "Learn how common carry items like keys, water bottles, umbrella tips, and heavy bag straps can act as defensive force multipliers."
        ));

        // 11. MULTIPLE THREAT SPACING
        list.add(new YouTubeVideo(
                "jOudPduYjFg",
                "MULTIPLE THREATS",
                "Positioning & Evacuation Tactics",
                "Avoid being surrounded by keeping attackers lined up in front of each other. Use lateral footwork to escape without getting cornered."
        ));

        // 12. HOOK & PUNCH BLOCKING
        list.add(new YouTubeVideo(
                "682_xDBQmYQ",
                "STREET DEFENSE",
                "Wild Punch & Hook Counters",
                "Raise your guard to cover the temple and ear area. Step inside the arc of a swinging punch to nullify power and follow with close elbows."
        ));

        // 13. COLLAR & SHIRT RELEASE
        list.add(new YouTubeVideo(
                "o0UTVZTkoG4",
                "CLOTHING GRAB",
                "Front Shirt & Collar Hold Escape",
                "Trap their gripping wrist with one hand, pin their elbow joint with your forearm, and rotate downward to break their finger hold."
        ));

        // 14. ENCLOSED SPACE DEFENSE
        list.add(new YouTubeVideo(
                "TaFam5TROaQ",
                "ENCLOSED SPACES",
                "Vehicle & Elevator Defense Moves",
                "Adapt short-range strikes (short elbows, palm strikes, heel stomps) when trapped in confined spaces with limited room to maneuver."
        ));

        // 15. PRESSURE POINT DISENGAGEMENT
        list.add(new YouTubeVideo(
                "9bZkp7q19f0",
                "JOINT LEVERAGE",
                "Pressure Points & Small Joint Escapes",
                "Apply simple joint mechanics against smaller joints like fingers and thumb bases to break strong locks regardless of weight differences."
        ));

        return list;
    }
}