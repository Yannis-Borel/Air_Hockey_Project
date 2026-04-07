package projet.M1.game;

import com.jme3.math.Vector3f;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;
import projet.M1.physics.PhysicsEngine;

/**
 * Gère les règles du jeu : scores, buts, remises en jeu, victoire.
 *
 * Règles implémentées :
 *   - But dans le camp P1 → P2 marque +1
 *   - But dans le camp P2 → P1 marque +1
 *   - Auto-but (dernier contact = joueur concerné) → ce joueur perd 1 pt (min 0)
 *   - Premier à 12 points → victoire
 *   - Rondelle bloquée 5s → remise en jeu côté du serveur
 *   - Après un but : pause 2s puis remise en jeu côté du joueur qui a concédé
 */
public class GameRules {

    public enum State { PLAYING, GOAL_PAUSE, GAME_OVER }

    public static final int   WIN_SCORE           = 12;
    private static final float GOAL_PAUSE_DURATION = 2f;
    private static final float STUCK_TIMEOUT       = 5f;

    private int   scoreP1   = 0;
    private int   scoreP2   = 0;
    private State state      = State.PLAYING;
    private float pauseTimer = 0f;
    private float stuckTimer = 0f;
    private int   nextServer = 1;   // qui sert après le prochain but
    private int   lastTouched = 0;  // dernier joueur à avoir touché la rondelle (0 = personne)

    private String goalMessage = "";
    private int    lastScorer  = 0;

    private final Puck          puck;
    private final PhysicsEngine physics;

    public GameRules(Puck puck, PhysicsEngine physics) {
        this.puck    = puck;
        this.physics = physics;
    }

    // Appelé par PhysicsEngine quand une raquette touche la rondelle
    public void notifyPaddleTouch(int player) {
        lastTouched = player;
    }

    public void update(float tpf) {
        switch (state) {
            case PLAYING    -> updatePlaying(tpf);
            case GOAL_PAUSE -> updateGoalPause(tpf);
            case GAME_OVER  -> {}
        }
    }

    private void updatePlaying(float tpf) {
        if (physics.isGoalP1()) {
            handleGoal(1); // rondelle dans camp P1
        } else if (physics.isGoalP2()) {
            handleGoal(2); // rondelle dans camp P2
        }

        // Rondelle bloquée → remise en jeu après STUCK_TIMEOUT secondes
        Vector3f vel = puck.getVelocity();
        if (vel.x * vel.x + vel.z * vel.z < 0.01f) {
            stuckTimer += tpf;
            if (stuckTimer >= STUCK_TIMEOUT) {
                resetPuck(nextServer);
                stuckTimer = 0f;
            }
        } else {
            stuckTimer = 0f;
        }
    }

    /**
     * camp = camp dans lequel la rondelle est entrée (1 = camp P1, 2 = camp P2).
     *
     * Cas normal    : l'adversaire marque +1
     * Auto-but      : le joueur concerné perd 1 pt (min 0), adversaire ne gagne rien
     */
    private void handleGoal(int camp) {
        puck.setVelocity(0, 0);

        boolean ownGoal = (lastTouched == camp); // P1 frappe dans son propre camp

        if (ownGoal) {
            if (camp == 1) {
                scoreP1 = Math.max(0, scoreP1 - 1);
                goalMessage = "Auto-but P1 ! P1 : " + scoreP1 + " - P2 : " + scoreP2;
                lastScorer  = 2;
            } else {
                scoreP2 = Math.max(0, scoreP2 - 1);
                goalMessage = "Auto-but P2 ! P1 : " + scoreP1 + " - P2 : " + scoreP2;
                lastScorer  = 1;
            }
        } else {
            if (camp == 1) {
                scoreP2++;
                goalMessage = "BUT P2 ! P1 : " + scoreP1 + " - P2 : " + scoreP2;
                lastScorer  = 2;
            } else {
                scoreP1++;
                goalMessage = "BUT P1 ! P1 : " + scoreP1 + " - P2 : " + scoreP2;
                lastScorer  = 1;
            }
        }

        System.out.println(goalMessage);

        // Le joueur qui a concédé sert ensuite — la balle va directement en position de service
        nextServer = camp;
        resetPuck(nextServer);

        if (scoreP1 >= WIN_SCORE || scoreP2 >= WIN_SCORE) {
            state = State.GAME_OVER;
            System.out.println("=== VICTOIRE Joueur " + (scoreP1 >= WIN_SCORE ? 1 : 2) + " ! ===");
        } else {
            state      = State.GOAL_PAUSE;
            pauseTimer = 0f;
        }
    }

    private void updateGoalPause(float tpf) {
        pauseTimer += tpf;
        if (pauseTimer >= GOAL_PAUSE_DURATION) {
            resetPuck(nextServer);
            state = State.PLAYING;
        }
    }

    /**
     * Replace la rondelle en position de service côté du joueur server.
     * server = 1 → côté P1 (Z négatif), server = 2 → côté P2 (Z positif)
     */
    public void resetPuck(int server) {
        float z = (server == 1) ? -(Table.HALF_L / 2f) : (Table.HALF_L / 2f);
        puck.setPosition(0f, z);
        puck.setVelocity(0f, 0f);
        lastTouched = 0;
    }

    // Getters utilisés par HUD et Main
    public State  getState()       { return state; }
    public int    getScoreP1()     { return scoreP1; }
    public int    getScoreP2()     { return scoreP2; }
    public String getGoalMessage() { return goalMessage; }
    public int    getLastScorer()  { return lastScorer; }
    public float  getPauseTimer()  { return pauseTimer; }
    public float  getPauseDuration() { return GOAL_PAUSE_DURATION; }
    public boolean isGameOver()    { return state == State.GAME_OVER; }
}
