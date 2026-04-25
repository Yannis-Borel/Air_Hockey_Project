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
import projet.M1.bonus.BonusManager;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;
import projet.M1.game.GameRules;
import projet.M1.game.OpponentProfile;
import projet.M1.hud.HUDManager;
import projet.M1.input.AIController;
import projet.M1.input.PlayerInputHandler;
import projet.M1.input.TournamentAIController;
import projet.M1.physics.PhysicsEngine;
import projet.M1.menu.MainMenuState;
import projet.M1.menu.MenuState;
import projet.M1.menu.GameOverState;
import projet.M1.menu.TournamentResultState;

import java.util.Random;

/**
 * Point d'entrée principal du jeu Air Hockey.
 */
public class Main extends SimpleApplication {

    public enum GameMode { VS, SOLO, TOURNAMENT }

    private GameMode currentMode;

    // Round courant du tournoi (1 à 5)
    private int tournamentRound = 1;

    private Table table;
    private Puck puck;
    private Paddle paddleP1;
    private Paddle paddleP2;
    private PhysicsEngine physics;
    private GameRules gameRules;
    private HUDManager hud;
    private BonusManager bonusManager;
    private PlayerInputHandler playerInputP1;
    private PlayerInputHandler playerInputP2;
    private AIController           aiController;           // IA mode solo
    private TournamentAIController tournamentAIController; // IA mode tournoi

    private final Vector3f savedPuckVelocity = new Vector3f();
    private boolean gameStarted = false;
    // Flag pour éviter d'attacher TournamentResultState plusieurs fois
    private boolean resultShown = false;

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Projet Air Hockey");
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

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        setDisplayStatView(false);
        setDisplayFps(false);

        viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.05f, 0.1f, 1f));

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.3f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.9f));
        rootNode.addLight(sun);

        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        stateManager.attach(new MainMenuState());
    }

    public void startGame(GameMode mode) {
        this.currentMode = mode;
        if (mode == GameMode.TOURNAMENT) {
            tournamentRound = 1;
            startTournamentRound(1);
            return;
        }
        initGame(mode, null);
    }

    /**
     * Démarre un round de tournoi contre l'adversaire du round donné.
     * Appelé par TournamentResultState quand on passe au round suivant.
     */
    public void startTournamentRound(int round) {
        this.currentMode    = GameMode.TOURNAMENT;
        this.tournamentRound = round;
        OpponentProfile profile = OpponentProfile.forRound(round);
        initGame(GameMode.TOURNAMENT, profile);
    }

    /**
     * Initialise et démarre une partie selon le mode et le profil adversaire.
     */
    private void initGame(GameMode mode, OpponentProfile profile) {
        if (gameStarted) cleanupGame();

        resultShown = false;

        // Vue longueur pour solo et tournoi, dessus pour 1v1
        if (mode == GameMode.VS) {
            setCamTop();
        } else {
            setCamSideLength();
        }

        table = new Table(assetManager);
        rootNode.attachChild(table.getNode());

        if (mode == GameMode.VS) {
            paddleP1 = new Paddle(assetManager, new ColorRGBA(0.9f, 0.15f, 0.15f, 1f), 0f, -7f);
            paddleP2 = new Paddle(assetManager, new ColorRGBA(0.1f, 0.3f, 0.9f, 1f),  0f,  7f);
        } else {
            paddleP1 = new Paddle(assetManager, new ColorRGBA(0.9f, 0.15f, 0.15f, 1f), 0f, -7f); // IA
            paddleP2 = new Paddle(assetManager, new ColorRGBA(0.1f, 0.3f, 0.9f, 1f),  0f,  7f); // joueur
        }
        rootNode.attachChild(paddleP1.getNode());
        rootNode.attachChild(paddleP2.getNode());

        puck = new Puck(assetManager);
        rootNode.attachChild(puck.getNode());

        physics   = new PhysicsEngine(puck, paddleP1, paddleP2);
        gameRules = new GameRules(puck, physics, paddleP1, paddleP2);
        physics.setGameRules(gameRules);

        // Placer le palet dans le camp du serveur aléatoire (hors zone neutre)
        Random rnd = new Random();
        int server = rnd.nextBoolean() ? 1 : 2;
        gameRules.resetPuck(server);
        float dirX = (rnd.nextFloat() - 0.5f) * 4f;
        float dirZ = (server == 1) ? 7f : -7f;
        puck.setVelocity(dirX, dirZ);

        int screenW = context.getSettings().getWidth();
        int screenH = context.getSettings().getHeight();

        hud = new HUDManager(assetManager, rootNode, guiNode, gameRules, mode, screenW, screenH);

        // Bonus uniquement en mode 1v1
        bonusManager = new BonusManager(assetManager, rootNode, puck, paddleP1, paddleP2);
        physics.setBonusManager(bonusManager);

        if (mode == GameMode.VS) {
            // 1v1 : P1 ZQSD, P2 flèches
            playerInputP1 = new PlayerInputHandler(inputManager, paddleP1, "p1_",
                    KeyInput.KEY_W, KeyInput.KEY_S, KeyInput.KEY_A, KeyInput.KEY_D);
            playerInputP2 = new PlayerInputHandler(inputManager, paddleP2, "p2_",
                    KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT);
            aiController           = null;
            tournamentAIController = null;
        } else if (mode == GameMode.SOLO) {
            // Solo : joueur = paddleP2 (flèches), IA basique = paddleP1
            playerInputP1 = null;
            playerInputP2 = new PlayerInputHandler(inputManager, paddleP2, "p2_",
                    KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT);
            aiController           = new AIController(paddleP1, puck);
            tournamentAIController = null;
        } else {
            // Tournoi : joueur = paddleP2 (flèches), IA tournoi = paddleP1
            playerInputP1 = null;
            playerInputP2 = new PlayerInputHandler(inputManager, paddleP2, "p2_",
                    KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT);
            aiController           = null;
            tournamentAIController = new TournamentAIController(paddleP1, puck, profile);
        }

        setupEscKey();

        gameStarted = true;

        // Décompte avant le début
        stateManager.attach(new CountdownState(this));

        System.out.println("=== Air Hockey démarré — mode : " + mode
                + (profile != null ? " — Round " + profile.round + " : " + profile.name : "") + " ===");
    }

    public void returnToMainMenu() {
        cleanupGame();
        stateManager.attach(new MainMenuState());
    }

    private void cleanupGame() {
        if (table        != null) rootNode.detachChild(table.getNode());
        if (paddleP1     != null) rootNode.detachChild(paddleP1.getNode());
        if (paddleP2     != null) rootNode.detachChild(paddleP2.getNode());
        if (puck         != null) rootNode.detachChild(puck.getNode());
        if (hud          != null) hud.cleanup();
        if (bonusManager != null) bonusManager.reset();
        if (playerInputP1 != null) playerInputP1.cleanup(inputManager);
        if (playerInputP2 != null) playerInputP2.cleanup(inputManager);

        if (inputManager.hasMapping("pause")) inputManager.deleteMapping("pause");

        aiController           = null;
        tournamentAIController = null;
        gameStarted            = false;
    }

    public void setCamTop() {
        cam.setLocation(new Vector3f(0f, 30f, 0f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z.negate());
    }

    public void setCamSideLength() {
        cam.setLocation(new Vector3f(0f, 7f, 19f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    public void savePuckVelocity() {
        savedPuckVelocity.set(puck.getVelocity());
        puck.setVelocity(0f, 0f);
    }

    public void restorePuckVelocity() {
        puck.setVelocity(savedPuckVelocity.x, savedPuckVelocity.z);
    }

    private void setupEscKey() {
        inputManager.addMapping("pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (!isPressed) return;
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

    @Override
    public void simpleUpdate(float tpf) {
        if (!gameStarted) return;

        gameRules.update(tpf);
        hud.update();

        if (gameRules.getState() == GameRules.State.GAME_OVER && !resultShown) {
            resultShown = true;
            // Déterminer le gagnant : P1 = IA (Z négatif), P2 = joueur (Z positif)
            // scoreP1 = score de la raquette rouge (IA), scoreP2 = score du joueur bleu
            boolean playerWon = gameRules.getScoreP2() >= GameRules.WIN_SCORE;

            if (currentMode == GameMode.TOURNAMENT) {
                stateManager.attach(new TournamentResultState(playerWon, tournamentRound));
            } else if (currentMode == GameMode.SOLO) {
                // En solo : winner = 2 si joueur (bleu) gagne, 1 si IA gagne
                int winner = playerWon ? 2 : 1;
                stateManager.attach(new GameOverState(winner, currentMode));
            } else {
                int winner = (gameRules.getScoreP1() >= GameRules.WIN_SCORE) ? 1 : 2;
                stateManager.attach(new GameOverState(winner, currentMode));
            }
        }

        if (gameRules.getState() == GameRules.State.PLAYING
                && stateManager.getState(CountdownState.class) == null) {
            if (playerInputP1 != null) playerInputP1.update(tpf);
            if (playerInputP2 != null) playerInputP2.update(tpf);
            if (aiController  != null) aiController.update(tpf);
            if (tournamentAIController != null) tournamentAIController.update(tpf);
            physics.update(tpf);
            // Bonus uniquement en mode 1v1
            if (currentMode == GameMode.VS) bonusManager.update(tpf);
        }
    }
}