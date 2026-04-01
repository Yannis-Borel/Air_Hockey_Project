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
import projet.M1.input.PlayerInputHandler;
import projet.M1.physics.PhysicsEngine;

/**
 * Point d'entrée principal du jeu Air Hockey.
 *
 * Architecture globale :
 *   - Main hérite de SimpleApplication (JME3)
 *   - La boucle de jeu principale est gérée par simpleUpdate(float tpf)
 *   - Les entités (table, raquettes, rondelle) seront initialisées dans simpleInitApp()
 *   - Le GameState orchestrera les règles et l'état courant de la partie
 *
 * Projet M1 Informatique - Université de Toulon
 * Module : Vision par Ordinateur - Pr. Julien SEINTURIER
 */
public class Main extends SimpleApplication {

    // TODO: injecter le GameState ici quand il sera créé
    private Table                table;
    private Puck                 puck;
    private Paddle               paddleP1;
    private Paddle               paddleP2;
    private PhysicsEngine        physics;
    private PlayerInputHandler   playerInputP1;
    private PlayerInputHandler   playerInputP2;

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Air Hockey - M1 Informatique - Université de Toulon");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setFrameRate(60);
        settings.setSamples(4);         // anti-aliasing x4
        settings.setVSync(true);
        settings.setFullscreen(false);

        app.setSettings(settings);
        app.setShowSettings(false);     // pas de dialog de config au démarrage
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);

        // Position initiale : vue isométrique sur la table (10x20 unités)
        // Table centrée en (0,0,0), s'étend de Z=-10 à Z=+10 et X=-5 à X=+5
        cam.setLocation(new Vector3f(0f, 28f, 14f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Fond bleu très sombre, style arène
        viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.05f, 0.1f, 1f));

        // Lumières
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.3f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.9f));
        rootNode.addLight(sun);

        // Table
        table = new Table(assetManager);
        rootNode.attachChild(table.getNode());

        // Raquettes — P1 côté Z négatif (bleu), P2 côté Z positif (rouge)
        paddleP1 = new Paddle(assetManager, new ColorRGBA(0.9f, 0.15f, 0.15f, 1f), 0f, -7f); // rouge
        paddleP2 = new Paddle(assetManager, new ColorRGBA(0.1f, 0.3f, 0.9f, 1f),  0f,  7f); // bleu
        rootNode.attachChild(paddleP1.getNode());
        rootNode.attachChild(paddleP2.getNode());

        // Rondelle — au centre de la table
        puck = new Puck(assetManager);
        rootNode.attachChild(puck.getNode());
        puck.setVelocity(4f, 7f); // vitesse initiale de test

        physics      = new PhysicsEngine(puck, paddleP1, paddleP2);
        // Rouge (P1) : ZQSD — Bleu (P2) : flèches
        playerInputP1 = new PlayerInputHandler(inputManager, paddleP1, "p1_",
            KeyInput.KEY_W, KeyInput.KEY_S, KeyInput.KEY_A, KeyInput.KEY_D);
        playerInputP2 = new PlayerInputHandler(inputManager, paddleP2, "p2_",
            KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT);

        setupCameraKeys();
        setupEscKey();

        System.out.println("=== Air Hockey - JME3 initialisé ===");
        System.out.println("Caméras : [1] dessus  [2] côté largeur  [3] côté longueur");
        System.out.println("[Echap] Menu pause");
    }

    // --- Presets de caméra ---
    // Table : X in [-5, +5], Z in [-10, +10], surface à Y=0

    void setCamTop() {
        // Vue du dessus, axe Y, centrée sur la table
        cam.setLocation(new Vector3f(0f, 30f, 0f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z.negate());
    }

    void setCamSideWidth() {
        cam.setLocation(new Vector3f(-18f, 10f, 0f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    void setCamSideLength() {
        cam.setLocation(new Vector3f(0f, 7f, 19f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private void setupEscKey() {
        // Remplace le comportement par défaut de JME (ESC = quitter)
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        inputManager.addMapping("pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (!isPressed) return;
            // Bloquer ESC pendant le décompte
            if (stateManager.getState(CountdownState.class) != null) return;
            MenuState existing = stateManager.getState(MenuState.class);
            if (existing != null) {
                existing.resume(); // ESC ferme le menu aussi
            } else {
                stateManager.attach(new MenuState());
            }
        }, "pause");
    }

    private void setupCameraKeys() {
        inputManager.addMapping("cam1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("cam2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("cam3", new KeyTrigger(KeyInput.KEY_3));

        ActionListener camListener = (name, isPressed, tpf) -> {
            if (!isPressed) return;
            switch (name) {
                case "cam1" -> setCamTop();
                case "cam2" -> setCamSideWidth();
                case "cam3" -> setCamSideLength();
            }
        };

        inputManager.addListener(camListener, "cam1", "cam2", "cam3");
    }

    @Override
    public void simpleUpdate(float tpf) {
        playerInputP1.update(tpf);
        playerInputP2.update(tpf);
        physics.update(tpf);
    }
}
