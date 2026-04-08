package projet.M1.game;

import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
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
 *   - Après un but : les raquettes reviennent à leur position initiale
 */
public class GameRules {

    public enum State { PLAYING, GOAL_PAUSE, GAME_OVER }

    public static final int    WIN_SCORE           = 12;
    private static final float GOAL_PAUSE_DURATION = 2f;
    private static final float STUCK_TIMEOUT       = 5f;

    private int   scoreP1    = 0;
    private int   scoreP2    = 0;
    private State state       = State.PLAYING;
    private float pauseTimer  = 0f;
    private float stuckTimer  = 0f;
    private int   nextServer  = 1;
    private int   lastTouched = 0;

    private String goalMessage = "";
    private int    lastScorer  = 0;

    private final Puck          puck;
    private final PhysicsEngine physics;
    private final Paddle        paddleP1;
    private final Paddle        paddleP2;

    public GameRules(Puck puck, PhysicsEngine physics, Paddle paddleP1, Paddle paddleP2) {
        this.puck     = puck;
        this.physics  = physics;
        this.paddleP1 = paddleP1;
        this.paddleP2 = paddleP2;
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
            handleGoal(1);
        } else if (physics.isGoalP2()) {
            handleGoal(2);
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
     * Cas normal : l'adversaire marque +1
     * Auto-but   : le joueur concerné perd 1 pt (min 0)
     */
    private void handleGoal(int camp) {
        puck.setVelocity(0, 0);

        boolean ownGoal = (lastTouched == camp);

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

        // Le joueur qui a concédé sert ensuite
        nextServer = camp;

        // Remettre les raquettes et le palet en position initiale
        resetAll(nextServer);

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
            state = State.PLAYING;
        }
    }

    /**
     * Replace la rondelle et les deux raquettes en position initiale.
     * Le palet est placé côté du serveur (Z = ±HALF_L/2),
     * les raquettes reviennent à leur position de départ (Z = ±7).
     * Pas de risque de collision : palet à Z=±5, raquettes à Z=±7.
     */
    private void resetAll(int server) {
        resetPuck(server);
        paddleP1.resetPosition();
        paddleP2.resetPosition();
    }

    public void resetPuck(int server) {
        float z = (server == 1) ? -(Table.HALF_L / 2f) : (Table.HALF_L / 2f);
        puck.setPosition(0f, z);
        puck.setVelocity(0f, 0f);
        lastTouched = 0;
    }

    // Getters utilisés par HUD et Main
    public State  getState()         { return state; }
    public int    getScoreP1()       { return scoreP1; }
    public int    getScoreP2()       { return scoreP2; }
    public String getGoalMessage()   { return goalMessage; }
    public int    getLastScorer()    { return lastScorer; }
    public float  getPauseTimer()    { return pauseTimer; }
    public float  getPauseDuration() { return GOAL_PAUSE_DURATION; }
    public boolean isGameOver()      { return state == State.GAME_OVER; }
}