package projet.M1.bonus;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

import java.util.Random;

/**
 * Gère le cycle de vie des bonus sur le terrain :
 *   - Apparition aléatoire après 15 secondes
 *   - Détection de collision avec le palet OU les raquettes
 *   - Application des effets (speed+, paddle+, paddle-)
 *   - Expiration des effets et réapparition après 15 secondes
 *
 * Zones d'apparition : zones des raquettes (hors zone neutre et hors bords)
 *   P1 : Z in [-HALF_L + margin, -NEUTRAL_Z - margin]
 *   P2 : Z in [+NEUTRAL_Z + margin, +HALF_L - margin]
 */
public class BonusManager {

    private static final float SPAWN_DELAY      = 15f;  // délai avant premier spawn et après activation
    private static final float SPEED_DURATION   = 10f;  // durée Speed+
    private static final float PADDLE_DURATION  = 20f;  // durée Paddle+ et Paddle-
    private static final float SPEED_MULTIPLIER = 1.5f;
    private static final float PADDLE_GROW      = 1.10f; // +10%
    private static final float PADDLE_SHRINK    = 0.90f; // -10%
    private static final float MARGIN           = 1.0f;  // marge par rapport aux bords

    private final AssetManager assetManager;
    private final Node         rootNode;
    private final Puck         puck;
    private final Paddle       paddleP1;
    private final Paddle       paddleP2;
    private final Random       random = new Random();

    // Bonus actuellement sur le terrain (null = aucun)
    private Bonus   currentBonus  = null;
    private float   spawnTimer    = 0f;   // temps écoulé depuis le dernier spawn/activation
    private boolean waitingSpawn  = true; // true = on attend le prochain spawn

    // Effets actifs
    private boolean speedActive      = false;
    private float   speedTimer       = 0f;

    private boolean paddlePlusActive = false;
    private float   paddlePlusTimer  = 0f;
    private int     paddlePlusPlayer = 0;  // joueur bénéficiaire

    private boolean paddleMinusActive = false;
    private float   paddleMinusTimer  = 0f;
    private int     paddleMinusPlayer = 0; // joueur pénalisé

    public BonusManager(AssetManager assetManager, Node rootNode,
                        Puck puck, Paddle paddleP1, Paddle paddleP2) {
        this.assetManager = assetManager;
        this.rootNode     = rootNode;
        this.puck         = puck;
        this.paddleP1     = paddleP1;
        this.paddleP2     = paddleP2;
    }

    public void update(float tpf) {
        updateSpawnTimer(tpf);
        checkCollisions();
        updateActiveEffects(tpf);
    }

    // --- Spawn ---

    private void updateSpawnTimer(float tpf) {
        if (!waitingSpawn || currentBonus != null) return;

        spawnTimer += tpf;
        if (spawnTimer >= SPAWN_DELAY) {
            spawnBonus();
            spawnTimer    = 0f;
            waitingSpawn  = false;
        }
    }

    private void spawnBonus() {
        BonusType type = BonusType.values()[random.nextInt(BonusType.values().length)];

        // Choisir aléatoirement le camp P1 ou P2
        float zMin, zMax;
        if (random.nextBoolean()) {
            zMin = -Table.HALF_L + MARGIN;
            zMax = -Table.NEUTRAL_Z - MARGIN;
        } else {
            zMin =  Table.NEUTRAL_Z + MARGIN;
            zMax =  Table.HALF_L - MARGIN;
        }

        float x = (-Table.HALF_W + MARGIN) + random.nextFloat() * (Table.WIDTH - 2 * MARGIN);
        float z = zMin + random.nextFloat() * (zMax - zMin);

        currentBonus = new Bonus(assetManager, type, x, z);
        rootNode.attachChild(currentBonus.getNode());
        System.out.println("[Bonus] Apparu : " + type.shortName + " en (" + x + ", " + z + ")");
    }

    // --- Détection de collision : palet ET raquettes ---

    private void checkCollisions() {
        if (currentBonus == null) return;

        Vector3f bonusPos = currentBonus.getPosition();

        // Collision avec le palet (Speed+)
        if (currentBonus.getType() == BonusType.SPEED_PLUS) {
            Vector3f puckPos = puck.getPosition();
            float dx = puckPos.x - bonusPos.x;
            float dz = puckPos.z - bonusPos.z;
            if (Math.abs(dx) < Bonus.SIZE + Puck.RADIUS
                    && Math.abs(dz) < Bonus.SIZE + Puck.RADIUS) {
                activateBonus(currentBonus, 0); // toucher = indéterminé, on utilise la position du palet
                triggerRemove();
                return;
            }
        }

        // Collision avec la raquette P1 (Paddle+ / Paddle-)
        if (checkPaddleCollision(paddleP1, bonusPos)) {
            activateBonus(currentBonus, 1);
            triggerRemove();
            return;
        }

        // Collision avec la raquette P2 (Paddle+ / Paddle-)
        if (checkPaddleCollision(paddleP2, bonusPos)) {
            activateBonus(currentBonus, 2);
            triggerRemove();
        }
    }

    /**
     * Vérifie si une raquette chevauche le bonus.
     * La raquette est un cercle de rayon getCurrentRadius(), le bonus un carré de côté SIZE*2.
     */
    private boolean checkPaddleCollision(Paddle paddle, Vector3f bonusPos) {
        Vector3f padPos = paddle.getPosition();
        float dx = padPos.x - bonusPos.x;
        float dz = padPos.z - bonusPos.z;
        float r  = paddle.getCurrentRadius();
        return Math.abs(dx) < Bonus.SIZE + r && Math.abs(dz) < Bonus.SIZE + r;
    }

    private void triggerRemove() {
        removeBonus();
        waitingSpawn = true;
        spawnTimer   = 0f;
    }

    private void activateBonus(Bonus bonus, int toucher) {
        // Si toucher == 0, on détermine le joueur via la position du palet
        if (toucher == 0) toucher = (puck.getPosition().z < 0) ? 1 : 2;

        switch (bonus.getType()) {
            case SPEED_PLUS -> {
                // Appliquer boost vitesse immédiatement
                Vector3f vel = puck.getVelocity();
                puck.setVelocity(vel.x * SPEED_MULTIPLIER, vel.z * SPEED_MULTIPLIER);
                speedActive = true;
                speedTimer  = 0f;
                System.out.println("[Bonus] SPEED+ activé par P" + toucher);
            }
            case PADDLE_PLUS -> {
                // Annuler effet précédent si actif
                if (paddlePlusActive) expirePaddlePlus();
                paddlePlusPlayer = toucher;
                getPaddle(toucher).scaleRadius(PADDLE_GROW);
                paddlePlusActive = true;
                paddlePlusTimer  = 0f;
                System.out.println("[Bonus] PADDLE+ activé pour P" + toucher);
            }
            case PADDLE_MINUS -> {
                // La raquette adverse rétrécit
                int opponent = (toucher == 1) ? 2 : 1;
                if (paddleMinusActive) expirePaddleMinus();
                paddleMinusPlayer = opponent;
                getPaddle(opponent).scaleRadius(PADDLE_SHRINK);
                paddleMinusActive = true;
                paddleMinusTimer  = 0f;
                System.out.println("[Bonus] PADDLE- activé sur P" + opponent);
            }
        }
    }

    private void removeBonus() {
        if (currentBonus != null) {
            rootNode.detachChild(currentBonus.getNode());
            currentBonus = null;
        }
    }

    // --- Effets actifs ---

    private void updateActiveEffects(float tpf) {
        if (speedActive) {
            speedTimer += tpf;
            if (speedTimer >= SPEED_DURATION) expireSpeed();
        }
        if (paddlePlusActive) {
            paddlePlusTimer += tpf;
            if (paddlePlusTimer >= PADDLE_DURATION) expirePaddlePlus();
        }
        if (paddleMinusActive) {
            paddleMinusTimer += tpf;
            if (paddleMinusTimer >= PADDLE_DURATION) expirePaddleMinus();
        }
    }

    private void expireSpeed() {
        // Pas de rollback sur la vitesse — elle a évolué depuis l'activation
        speedActive = false;
        System.out.println("[Bonus] SPEED+ expiré");
    }

    private void expirePaddlePlus() {
        getPaddle(paddlePlusPlayer).scaleRadius(1f / PADDLE_GROW);
        paddlePlusActive = false;
        System.out.println("[Bonus] PADDLE+ expiré pour P" + paddlePlusPlayer);
    }

    private void expirePaddleMinus() {
        getPaddle(paddleMinusPlayer).scaleRadius(1f / PADDLE_SHRINK);
        paddleMinusActive = false;
        System.out.println("[Bonus] PADDLE- expiré pour P" + paddleMinusPlayer);
    }

    private Paddle getPaddle(int player) {
        return (player == 1) ? paddleP1 : paddleP2;
    }

    /** Remet à zéro tous les effets et supprime le bonus du terrain (appelé après un but). */
    public void reset() {
        removeBonus();
        if (speedActive)       expireSpeed();
        if (paddlePlusActive)  expirePaddlePlus();
        if (paddleMinusActive) expirePaddleMinus();
        spawnTimer   = 0f;
        waitingSpawn = true;
    }

    // Getters
    public boolean isSpeedActive()       { return speedActive; }
    public float   getSpeedTimer()       { return speedTimer; }
    public boolean isPaddlePlusActive()  { return paddlePlusActive; }
    public float   getPaddlePlusTimer()  { return paddlePlusTimer; }
    public int     getPaddlePlusPlayer() { return paddlePlusPlayer; }
    public boolean isPaddleMinusActive() { return paddleMinusActive; }
    public float   getPaddleMinusTimer() { return paddleMinusTimer; }
    public int     getPaddleMinusPlayer(){ return paddleMinusPlayer; }
}