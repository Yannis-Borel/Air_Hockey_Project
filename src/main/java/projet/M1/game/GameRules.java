package projet.M1.game;

import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;
import projet.M1.physics.PhysicsEngine;

import projet.M1.audio.SoundManager;

import java.util.Random;

/**
 * Gère les règles du jeu : scores, buts, remises en jeu, victoire.
 *
 * Règles implémentées :
 *   - But dans le camp P1 → P2 marque +1
 *   - But dans le camp P2 → P1 marque +1
 *   - Auto-but (dernier contact = joueur concerné) → ce joueur perd 1 pt (min 0)
 *     ET l'adversaire gagne +1
 *   - Premier à 12 points → victoire
 *   - Rondelle bloquée 5s → remise en jeu côté du serveur
 *   - Après un but : pause 2s puis remise en jeu côté du joueur qui a concédé
 *   - Après un but : les raquettes reviennent à leur position initiale
 */
public class GameRules {

    public enum State { PLAYING, GOAL_PAUSE, GAME_OVER }

    public static final int    WIN_SCORE         = 5;
    private static final float GOAL_PAUSE_DURATION  = 2f;
    private static final float STUCK_TIMEOUT        = 1.5f;
    private static final float NEUTRAL_STALL_TIMEOUT = 2.0f;

    private int   scoreP1    = 0;
    private int   scoreP2    = 0;
    private State state       = State.PLAYING;
    private float pauseTimer       = 0f;
    private float stuckTimer       = 0f;
    private float neutralStallTimer = 0f;
    private int   nextServer       = 1;
    private int   lastTouched = 0;

    private final Random random = new Random();

    private String goalMessage = "";
    private int    lastScorer  = 0;

    private final Puck          puck;
    private final PhysicsEngine physics;
    private final Paddle        paddleP1;
    private final Paddle        paddleP2;
    private       SoundManager  soundManager;

    public void setSoundManager(SoundManager sm) { this.soundManager = sm; }

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

        Vector3f vel = puck.getVelocity();
        float speed2 = vel.x * vel.x + vel.z * vel.z;

        // Rondelle bloquée → remise en jeu après STUCK_TIMEOUT secondes
        if (speed2 < 0.01f) {
            stuckTimer += tpf;
            if (stuckTimer >= STUCK_TIMEOUT) {
                resetPuck(nextServer);
                float dirZ = (nextServer == 1) ? 1f : -1f;
                float dirX = (random.nextFloat() - 0.5f) * 7f;
                puck.setVelocity(dirX, dirZ * 8f);
                stuckTimer       = 0f;
                neutralStallTimer = 0f;
            }
        } else {
            stuckTimer = 0f;
        }

        // Balle qui stagne en zone neutre (vitesse Z trop faible) →
        // mini boost vers le camp le plus proche
        boolean inNeutral = Math.abs(puck.getPosition().z) < Table.NEUTRAL_Z;
        if (inNeutral && speed2 > 0.01f && Math.abs(vel.z) < 1.5f) {
            neutralStallTimer += tpf;
            if (neutralStallTimer >= NEUTRAL_STALL_TIMEOUT) {
                float pz   = puck.getPosition().z;
                float dirZ = (pz >= 0f) ? 1f : -1f; // vers le camp le plus proche
                puck.setVelocity(vel.x, dirZ * 5f);
                neutralStallTimer = 0f;
            }
        } else {
            neutralStallTimer = 0f;
        }
    }

    /**
     * camp = camp dans lequel la rondelle est entrée (1 = camp P1, 2 = camp P2).
     *
     * Cas normal : l'adversaire marque +1
     * Auto-but   : le fautif perd 1 pt (min 0) ET l'adversaire gagne +1
     */
    private void handleGoal(int camp) {
        puck.setVelocity(0, 0);

        boolean ownGoal = (lastTouched == camp);

        if (ownGoal) {
            // Auto-but : le fautif perd 1 point ET l'adversaire gagne +1
            if (camp == 1) {
                scoreP1 = Math.max(0, scoreP1 - 1);
                scoreP2++;
                goalMessage = "Auto-but P1 ! P1 : " + scoreP1 + " - P2 : " + scoreP2;
                lastScorer  = 2;
            } else {
                scoreP2 = Math.max(0, scoreP2 - 1);
                scoreP1++;
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
        if (soundManager != null) soundManager.playGoal();

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

    /**
     * Remet le jeu à zéro : scores, état, positions.
     * Appelé par Main.restartGame() depuis l'écran Game Over.
     */
    public void reset() {
        scoreP1          = 0;
        scoreP2          = 0;
        state            = State.PLAYING;
        pauseTimer        = 0f;
        stuckTimer        = 0f;
        neutralStallTimer = 0f;
        nextServer        = 1;
        lastTouched = 0;
        lastScorer  = 0;
        goalMessage = "";
        resetAll(nextServer);
        float dir = (new java.util.Random().nextBoolean()) ? 1f : -1f;
        puck.setVelocity(5f, 9f * dir);
    }

    // Getters utilisés par HUD et Main
    public State  getState()         { return state; }
    public int    getScoreP1()       { return scoreP1; }
    public int    getScoreP2()       { return scoreP2; }
    public int    getWinner()        { return scoreP1 >= WIN_SCORE ? 1 : scoreP2 >= WIN_SCORE ? 2 : 0; }
    public String getGoalMessage()   { return goalMessage; }
    public int    getLastScorer()    { return lastScorer; }
    public float  getPauseTimer()    { return pauseTimer; }
    public float  getPauseDuration() { return GOAL_PAUSE_DURATION; }
    public boolean isGameOver()      { return state == State.GAME_OVER; }
    public int    getLastTouched()   { return lastTouched; }
}