package projet.M1.game;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

import java.util.Random;

/**
 * Gère les power-ups du jeu Air Hockey.
 *
 * Six types disponibles :
 *   SPEED_PLUS   — puck ×1.5 (boost instantané)
 *   PUCK_BIGGER  — puck ×1.2 pendant 10s
 *   PUCK_SMALLER — puck ×0.8 pendant 10s
 *   SHOT_ON_GOAL — puck part droit vers le but adverse
 *   PADDLE_PLUS  — raquette du dernier toucheur ×1.1 pendant 20s
 *   PADDLE_MINUS — raquette de l'adversaire ×0.9 pendant 20s
 *
 * Un seul token à la fois sur la table, dans la zone neutre.
 * Le token pulse visuellement et disparaît quand la rondelle le touche.
 */
public class PowerUpManager {

    public enum Type {
        SPEED_PLUS   ("Vitesse +",   new ColorRGBA(1.0f, 0.55f, 0.0f, 1f)),
        PUCK_BIGGER  ("Palet +",     new ColorRGBA(0.2f, 0.85f, 0.2f, 1f)),
        PUCK_SMALLER ("Palet -",     new ColorRGBA(0.75f, 0.2f, 0.9f, 1f)),
        SHOT_ON_GOAL ("Tir au but",  new ColorRGBA(1.0f, 0.92f, 0.0f, 1f)),
        PADDLE_PLUS  ("Raquette +",  new ColorRGBA(0.2f, 0.55f, 1.0f, 1f)),
        PADDLE_MINUS ("Raquette -",  new ColorRGBA(1.0f, 0.2f,  0.2f, 1f));

        public final String    label;
        public final ColorRGBA color;

        Type(String label, ColorRGBA color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final float TOKEN_RADIUS   = 0.55f;
    private static final float SPAWN_INTERVAL = 7f;
    private static final float SPEED_FACTOR   = 1.5f;
    private static final float PUCK_BIG       = 1.2f;
    private static final float PUCK_SMALL     = 0.8f;
    private static final float PAD_BIG        = 1.45f;  // +45% — bien visible
    private static final float PAD_SMALL      = 0.60f;  // -40% — clairement réduite
    private static final float EFFECT_DUR     = 10f;
    private static final float PADDLE_DUR     = 20f;

    private final Puck         puck;
    private final Paddle       paddleP1, paddleP2;
    private final Node         rootNode;
    private final AssetManager assetManager;
    private final Random       random = new Random();

    // Token affiché sur la table
    private Node     tokenNode = null;
    private Type     tokenType = null;
    private float    tokenX, tokenZ;
    private float    spawnTimer = 4f;
    private float    tokenAnim  = 0f;

    // Effets actifs
    private boolean speedOn;         private float speedTimer;
    private boolean puckSizeOn;      private float puckSizeTimer;    private float puckSizeFactor;
    private boolean p1PadOn;         private float p1PadTimer;       private float p1PadFactor;
    private boolean p2PadOn;         private float p2PadTimer;       private float p2PadFactor;

    // Dernier joueur ayant touché la rondelle (1 ou 2, 0 = inconnu)
    private int lastTouched = 0;

    public PowerUpManager(Puck puck, Paddle paddleP1, Paddle paddleP2,
                          Node rootNode, AssetManager assetManager) {
        this.puck         = puck;
        this.paddleP1     = paddleP1;
        this.paddleP2     = paddleP2;
        this.rootNode     = rootNode;
        this.assetManager = assetManager;
    }

    /** Appelé par GameRules quand une raquette touche la rondelle. */
    public void notifyPaddleTouch(int player) { lastTouched = player; }

    public void update(float tpf) {
        updateToken(tpf);
        updateEffects(tpf);
    }

    // ---------------------------------------------------------------

    private void updateToken(float tpf) {
        if (tokenNode != null) {
            // Animation : pulse + rotation Y
            tokenAnim += tpf * 2.8f;
            float s = 1f + 0.12f * FastMath.sin(tokenAnim);
            tokenNode.setLocalScale(s, 1f, s);
            Quaternion q = new Quaternion();
            q.fromAngleAxis(tokenAnim * 0.55f, Vector3f.UNIT_Y);
            tokenNode.setLocalRotation(q);

            // Détection collecte
            Vector3f pp = puck.getPosition();
            float dx = pp.x - tokenX;
            float dz = pp.z - tokenZ;
            float r  = puck.getRadius() + TOKEN_RADIUS;
            if (dx * dx + dz * dz <= r * r) collectToken();
        } else {
            spawnTimer -= tpf;
            if (spawnTimer <= 0f) spawnToken();
        }
    }

    private void spawnToken() {
        float mx = Table.HALF_W  - 1.3f;
        float mz = Table.NEUTRAL_Z - 0.8f;
        tokenX = (random.nextFloat() * 2f - 1f) * mx;
        tokenZ = (random.nextFloat() * 2f - 1f) * mz;

        Type[] types = Type.values();
        tokenType = types[random.nextInt(types.length)];

        tokenNode = buildTokenNode(tokenType);
        tokenNode.setLocalTranslation(tokenX, 0.07f, tokenZ);
        rootNode.attachChild(tokenNode);
        tokenAnim = 0f;
    }

    private void collectToken() {
        rootNode.detachChild(tokenNode);
        tokenNode = null;
        Type t    = tokenType;
        tokenType = null;
        spawnTimer = SPAWN_INTERVAL;
        applyEffect(t);
    }

    private void applyEffect(Type t) {
        switch (t) {
            case SPEED_PLUS -> {
                Vector3f vel = puck.getVelocity();
                float spd = FastMath.sqrt(vel.x * vel.x + vel.z * vel.z);
                if (spd > 0.2f) {
                    puck.setVelocity(vel.x * SPEED_FACTOR, vel.z * SPEED_FACTOR);
                } else {
                    float angle = random.nextFloat() * FastMath.TWO_PI;
                    puck.setVelocity(FastMath.cos(angle) * 9f, FastMath.sin(angle) * 9f);
                }
                speedOn    = true;
                speedTimer = EFFECT_DUR;
            }
            case PUCK_BIGGER -> {
                puck.setRadius(Puck.RADIUS * PUCK_BIG);
                puckSizeOn     = true;
                puckSizeTimer  = EFFECT_DUR;
                puckSizeFactor = PUCK_BIG;
            }
            case PUCK_SMALLER -> {
                puck.setRadius(Puck.RADIUS * PUCK_SMALL);
                puckSizeOn     = true;
                puckSizeTimer  = EFFECT_DUR;
                puckSizeFactor = PUCK_SMALL;
            }
            case SHOT_ON_GOAL -> {
                // Envoi vers le but de l'adversaire du dernier toucheur
                float targetZ = (lastTouched == 1) ? Table.HALF_L : -Table.HALF_L;
                Vector3f pos  = puck.getPosition();
                puck.setPosition(0f, pos.z);
                puck.setVelocity(0f, targetZ > 0 ? 16f : -16f);
            }
            case PADDLE_PLUS -> {
                if (lastTouched == 1) {
                    paddleP1.setRadius(paddleP1.getNominalRadius() * PAD_BIG);
                    p1PadOn = true; p1PadTimer = PADDLE_DUR; p1PadFactor = PAD_BIG;
                } else {
                    paddleP2.setRadius(paddleP2.getNominalRadius() * PAD_BIG);
                    p2PadOn = true; p2PadTimer = PADDLE_DUR; p2PadFactor = PAD_BIG;
                }
            }
            case PADDLE_MINUS -> {
                // Rétrécit la raquette de l'adversaire
                if (lastTouched == 1) {
                    paddleP2.setRadius(paddleP2.getNominalRadius() * PAD_SMALL);
                    p2PadOn = true; p2PadTimer = PADDLE_DUR; p2PadFactor = PAD_SMALL;
                } else {
                    paddleP1.setRadius(paddleP1.getNominalRadius() * PAD_SMALL);
                    p1PadOn = true; p1PadTimer = PADDLE_DUR; p1PadFactor = PAD_SMALL;
                }
            }
        }
    }

    private void updateEffects(float tpf) {
        if (speedOn) {
            speedTimer -= tpf;
            if (speedTimer <= 0) speedOn = false;
        }
        if (puckSizeOn) {
            puckSizeTimer -= tpf;
            if (puckSizeTimer <= 0) { puckSizeOn = false; puck.setRadius(Puck.RADIUS); }
        }
        if (p1PadOn) {
            p1PadTimer -= tpf;
            if (p1PadTimer <= 0) { p1PadOn = false; paddleP1.setRadius(paddleP1.getNominalRadius()); }
        }
        if (p2PadOn) {
            p2PadTimer -= tpf;
            if (p2PadTimer <= 0) { p2PadOn = false; paddleP2.setRadius(paddleP2.getNominalRadius()); }
        }
    }

    /** Supprime le token visible et annule tous les effets actifs. */
    public void reset() {
        if (tokenNode != null) { rootNode.detachChild(tokenNode); tokenNode = null; }
        tokenType  = null;
        spawnTimer = 4f;
        tokenAnim  = 0f;
        speedOn    = false;
        if (puckSizeOn) { puck.setRadius(Puck.RADIUS);             puckSizeOn = false; }
        if (p1PadOn)    { paddleP1.setRadius(paddleP1.getNominalRadius()); p1PadOn = false; }
        if (p2PadOn)    { paddleP2.setRadius(paddleP2.getNominalRadius()); p2PadOn = false; }
        lastTouched = 0;
    }

    // ---------------------------------------------------------------
    // Visuels du token (disc coloré + disque blanc central)

    private Node buildTokenNode(Type type) {
        Node n = new Node("pu_" + type.name());

        // Disque principal coloré
        Geometry disc = makeDisc(TOKEN_RADIUS, 0.10f, type.color);
        n.attachChild(disc);

        // Petit disque blanc au centre (indicateur visuel)
        Geometry center = makeDisc(TOKEN_RADIUS * 0.35f, 0.11f, ColorRGBA.White);
        n.attachChild(center);

        return n;
    }

    private Geometry makeDisc(float radius, float height, ColorRGBA color) {
        Cylinder cyl = new Cylinder(2, 20, radius, height, true);
        Geometry geo = new Geometry("disc_" + System.nanoTime(), cyl);
        geo.rotate(-FastMath.HALF_PI, 0, 0);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }

    // ---------------------------------------------------------------
    // Getters pour HUDManager

    public boolean isSpeedOn()        { return speedOn; }
    public float   getSpeedTimer()    { return speedTimer; }

    public boolean isPuckSizeOn()     { return puckSizeOn; }
    public float   getPuckSizeTimer() { return puckSizeTimer; }
    public float   getPuckSizeFactor(){ return puckSizeFactor; }

    public boolean isP1PadOn()        { return p1PadOn; }
    public float   getP1PadTimer()    { return p1PadTimer; }
    public float   getP1PadFactor()   { return p1PadFactor; }

    public boolean isP2PadOn()        { return p2PadOn; }
    public float   getP2PadTimer()    { return p2PadTimer; }
    public float   getP2PadFactor()   { return p2PadFactor; }

    public Type    getTokenType()     { return tokenType; }
}
