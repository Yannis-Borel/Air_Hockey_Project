package projet.M1;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;
import projet.M1.game.GameRules;
import projet.M1.game.PowerUpManager;
import projet.M1.hud.HUDManager;
import projet.M1.input.PlayerInputHandler;
import projet.M1.physics.PhysicsEngine;

/**
 * Point d'entrée principal du jeu Air Hockey.
 *
 * Modes de jeu :
 *   MULTIPLAYER — deux joueurs humains (ZQSD vs flèches), vue du dessus
 *   SOLO_AI     — un joueur contre une IA, niveau choisi au menu
 *   TOURNAMENT  — cinq adversaires IA avec modifications de terrain par round
 *
 * Power-ups, textures Lighting, et comportements spéciaux du tournoi sont gérés ici.
 */
public class Main extends SimpleApplication {

    public enum GameMode { MULTIPLAYER, SOLO_AI, TOURNAMENT }

    // ---- Entités de scène ----
    private Table               table;
    private Puck                puck;
    private Paddle              paddleP1;
    private Paddle              paddleP2;
    private PhysicsEngine       physics;
    private GameRules           gameRules;
    private HUDManager          hud;
    private PowerUpManager      powerUpManager;
    private PlayerInputHandler  playerInputP1;
    private PlayerInputHandler  playerInputP2;

    // ---- État ----
    private GameMode           currentMode  = null;
    private AIController       aiController = null;
    private TournamentManager  tournament   = null;

    private final Vector3f savedPuckVelocity = new Vector3f();
    private boolean        gameOverShown     = false;

    // ---------------------------------------------------------------

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Air Hockey - M1 Informatique - Université de Toulon");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setFrameRate(60);
        settings.setSamples(4);
        settings.setVSync(true);
        settings.setFullscreen(false);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    // ---------------------------------------------------------------

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        setCamSideLength(); // caméra unique par défaut

        viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.05f, 0.1f, 1f));

        // Lumières (nécessaires pour Lighting.j3md sur puck/paddles/table)
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.45f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.3f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.95f));
        rootNode.addLight(sun);

        // Table
        table = new Table(assetManager);
        rootNode.attachChild(table.getNode());

        // Raquettes
        paddleP1 = new Paddle(assetManager, new ColorRGBA(0.9f, 0.15f, 0.15f, 1f), 0f, -7f);
        paddleP2 = new Paddle(assetManager, new ColorRGBA(0.1f, 0.3f,  0.9f, 1f), 0f,  7f);
        rootNode.attachChild(paddleP1.getNode());
        rootNode.attachChild(paddleP2.getNode());

        // Rondelle
        puck = new Puck(assetManager);
        rootNode.attachChild(puck.getNode());

        // Physique + règles
        physics   = new PhysicsEngine(puck, paddleP1, paddleP2, table);
        gameRules = new GameRules(puck, physics, paddleP1, paddleP2);
        physics.setGameRules(gameRules);

        // Power-ups
        powerUpManager = new PowerUpManager(puck, paddleP1, paddleP2, rootNode, assetManager);

        // HUD
        int screenW = context.getSettings().getWidth();
        int screenH = context.getSettings().getHeight();
        hud = new HUDManager(assetManager, rootNode, guiNode, screenW, screenH, gameRules);
        hud.setPowerUpManager(powerUpManager);

        // Inputs
        playerInputP1 = new PlayerInputHandler(inputManager, paddleP1, "p1_",
                KeyInput.KEY_W, KeyInput.KEY_S, KeyInput.KEY_A, KeyInput.KEY_D);
        playerInputP2 = new PlayerInputHandler(inputManager, paddleP2, "p2_",
                KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT);

        setupEscKey();

        setDisplayStatView(false);
        setDisplayFps(false);

        stateManager.attach(new MainMenuState());

        System.out.println("=== Air Hockey — JME3 initialisé ===");
    }

    // ===============================================================
    // Méthodes publiques appelées par les menus
    // ===============================================================

    public void startMultiplayer() {
        currentMode   = GameMode.MULTIPLAYER;
        aiController  = null;
        gameOverShown = false;
        applyDefaultTableSettings();
        detachMenus();
        resetGameAndPowerUps();
        hud.setTournamentLabel(null);
        setCamTop();           // vue du dessus pour voir les deux raquettes
        savePuckVelocity();
        stateManager.attach(new CinematicState(this, currentMode));
    }

    public void startSoloAI(AIController.Level level) {
        currentMode   = GameMode.SOLO_AI;
        aiController  = new AIController(paddleP1, puck, level);
        gameOverShown = false;
        applyDefaultTableSettings();
        detachMenus();
        resetGameAndPowerUps();
        hud.setTournamentLabel(null);
        savePuckVelocity();
        stateManager.attach(new CinematicState(this, currentMode));
    }

    public void startTournament() {
        currentMode = GameMode.TOURNAMENT;
        if (tournament == null) tournament = new TournamentManager();
        tournament.reset();
        gameOverShown = false;
        detachMenus();
        startTournamentRound();
    }

    public void startTournamentRound() {
        TournamentManager.Opponent opp = tournament.getCurrentOpponent();

        // Appliquer les modificateurs de l'adversaire
        table.setGoalWidth(opp.goalWidth);
        paddleP1.setNominalRadius(Paddle.RADIUS * opp.paddleScale); // IA = P1
        paddleP2.setNominalRadius(Paddle.RADIUS);                    // Humain inchangé

        aiController  = new AIController(paddleP1, puck, opp.aiLevel);
        gameOverShown = false;
        detachGameOver();
        resetGameAndPowerUps();
        hud.forceRefreshScores();

        int round = tournament.getCurrentRound() + 1;
        int total = tournament.getTotalRounds();
        hud.setTournamentLabel("Round " + round + "/" + total
                + "  —  " + opp.name + "  (" + opp.description + ")");

        savePuckVelocity();
        stateManager.attach(new CinematicState(this, currentMode));
    }

    public void returnToMainMenu() {
        detachGameOver();
        detachMenus();
        CountdownState cs = stateManager.getState(CountdownState.class);
        if (cs != null) stateManager.detach(cs);

        currentMode   = null;
        aiController  = null;
        gameOverShown = false;

        applyDefaultTableSettings();
        resetGameAndPowerUps();
        puck.setVelocity(0f, 0f);
        puck.setPosition(0f, 0f);
        hud.setTournamentLabel(null);

        setCamSideLength();

        stateManager.attach(new MainMenuState());
    }

    // ===============================================================
    // Fin de partie
    // ===============================================================

    private void handleGameOver() {
        gameOverShown = true;
        int s1 = gameRules.getScoreP1();
        int s2 = gameRules.getScoreP2();

        switch (currentMode) {
            case MULTIPLAYER -> {
                int winner = gameRules.getWinner();
                ColorRGBA tc = (winner == 1)
                        ? new ColorRGBA(0.9f, 0.15f, 0.15f, 1f)
                        : new ColorRGBA(0.1f, 0.5f,  1.0f,  1f);
                stateManager.attach(new GameOverState(this,
                        "JOUEUR " + winner + " GAGNE !",   tc,
                        "Score final  —  P1 : " + s1 + "   P2 : " + s2,
                        "Rejouer",  this::restartGame,
                        "Menu",     this::returnToMainMenu));
            }
            case SOLO_AI -> {
                boolean win = (gameRules.getWinner() == 2);
                stateManager.attach(new GameOverState(this,
                        win ? "VICTOIRE !" : "DÉFAITE...",
                        win ? new ColorRGBA(0.1f, 0.5f, 1.0f, 1f)
                                : new ColorRGBA(0.9f, 0.15f, 0.15f, 1f),
                        "Score  —  IA : " + s1 + "   Toi : " + s2,
                        win ? "Rejouer"    : "Réessayer",  this::restartGame,
                        "Menu",                                  this::returnToMainMenu));
            }
            case TOURNAMENT -> handleTournamentGameOver(s1, s2);
        }
    }

    private void handleTournamentGameOver(int s1, int s2) {
        boolean playerWon = (gameRules.getWinner() == 2);
        TournamentManager.Opponent opp = tournament.getCurrentOpponent();

        if (playerWon) {
            tournament.nextRound();

            if (tournament.isFinished()) {
                stateManager.attach(new GameOverState(this,
                        "TOURNOI TERMINÉ !",
                        new ColorRGBA(1f, 0.8f, 0f, 1f),
                        "Tu es le champion d'Air Hockey !",
                        "Recommencer", this::startTournament,
                        "Menu",        this::returnToMainMenu));
            } else {
                TournamentManager.Opponent next = tournament.getCurrentOpponent();
                stateManager.attach(new GameOverState(this,
                        opp.name + " battu !",
                        new ColorRGBA(0.2f, 0.9f, 0.2f, 1f),
                        "Prochain : " + next.name + "  —  " + next.description,
                        "Continuer", this::startTournamentRound,
                        "Menu",      this::returnToMainMenu));
            }
        } else {
            stateManager.attach(new GameOverState(this,
                    "DÉFAITE contre " + opp.name,
                    new ColorRGBA(0.9f, 0.15f, 0.15f, 1f),
                    opp.description,
                    "Réessayer", this::startTournamentRound,
                    "Menu",          this::returnToMainMenu));
        }
    }

    public void restartGame() {
        detachGameOver();
        gameOverShown = false;

        switch (currentMode) {
            case MULTIPLAYER -> {
                resetGameAndPowerUps();
                savePuckVelocity();
                stateManager.attach(new CinematicState(this, currentMode));
            }
            case SOLO_AI -> {
                resetGameAndPowerUps();
                savePuckVelocity();
                stateManager.attach(new CinematicState(this, currentMode));
            }
            case TOURNAMENT -> startTournamentRound();
        }
    }

    // ===============================================================
    // Helpers
    // ===============================================================

    private void resetGameAndPowerUps() {
        gameRules.reset();
        powerUpManager.reset();
    }

    /** Remet la table et les raquettes aux valeurs par défaut (hors tournoi). */
    private void applyDefaultTableSettings() {
        table.setGoalWidth(Table.GOAL_WIDTH);
        paddleP1.setNominalRadius(Paddle.RADIUS);
        paddleP2.setNominalRadius(Paddle.RADIUS);
    }

    private void detachMenus() {
        MainMenuState mm = stateManager.getState(MainMenuState.class);
        if (mm != null) stateManager.detach(mm);
        MenuState ms = stateManager.getState(MenuState.class);
        if (ms != null) stateManager.detach(ms);
    }

    private void detachGameOver() {
        GameOverState gos = stateManager.getState(GameOverState.class);
        if (gos != null) stateManager.detach(gos);
    }

    // ===============================================================
    // Caméra
    // ===============================================================

    void setCamTop() {
        cam.setLocation(new Vector3f(0f, 30f, 0f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z.negate());
    }

    void setCamSideLength() {
        cam.setLocation(new Vector3f(0f, 7f, 19f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    // ===============================================================
    // Vélocité du palet (pause / décompte)
    // ===============================================================

    public void savePuckVelocity() {
        savedPuckVelocity.set(puck.getVelocity());
        puck.setVelocity(0f, 0f);
    }

    public void restorePuckVelocity() {
        puck.setVelocity(savedPuckVelocity.x, savedPuckVelocity.z);
    }

    // ===============================================================
    // Boucle principale
    // ===============================================================

    @Override
    public void simpleUpdate(float tpf) {
        gameRules.update(tpf);
        hud.update();

        if (currentMode == null) return;

        if (gameRules.isGameOver() && !gameOverShown) {
            handleGameOver();
            return;
        }

        if (gameRules.getState() == GameRules.State.PLAYING
                && stateManager.getState(CountdownState.class) == null
                && stateManager.getState(CinematicState.class) == null
                && stateManager.getState(MenuState.class) == null
                && !gameOverShown) {

            playerInputP2.update(tpf);

            if (currentMode == GameMode.MULTIPLAYER) {
                playerInputP1.update(tpf);
            } else if (aiController != null) {
                aiController.update(tpf);
            }

            physics.update(tpf);

            // Notifier le PowerUpManager du dernier joueur ayant touché la rondelle
            powerUpManager.notifyPaddleTouch(gameRules.getLastTouched());
            powerUpManager.update(tpf);
        }
    }

    // ===============================================================
    // Touches
    // ===============================================================

    private void setupEscKey() {
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        inputManager.addMapping("pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (!isPressed) return;
            if (currentMode == null) return;
            if (stateManager.getState(CountdownState.class) != null) return;
            MenuState existing = stateManager.getState(MenuState.class);
            if (existing != null) {
                existing.resume();
            } else {
                savePuckVelocity();
                stateManager.attach(new MenuState());
            }
        }, "pause");
    }
}