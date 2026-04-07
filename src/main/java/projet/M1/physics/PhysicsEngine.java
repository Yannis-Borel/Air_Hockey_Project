package projet.M1.physics;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

import java.util.Random;

/**
 * Moteur physique maison pour l'Air Hockey.
 *
 * Ce moteur gère :
 *   - L'intégration d'Euler (position += vitesse * tpf)
 *   - Les rebonds sur les 4 bandes (formule de réflexion vectorielle)
 *   - La friction légère simulant le coussin d'air
 *   - La détection de but (la rondelle passe dans l'ouverture du fond)
 *
 * La physique est purement 2D dans le plan XZ.
 * L'axe Y est fixe : la rondelle reste toujours posée sur la table.
 */
public class PhysicsEngine {

    // Friction modérée — la rondelle ralentit visiblement sans s'arrêter trop vite
    private static final float FRICTION     = 0.35f;

    // Restitution : légère perte d'énergie à chaque rebond
    private static final float RESTITUTION  = 0.88f;

    // Vitesse max (évite que ça parte dans tous les sens après un smash)
    private static final float MAX_SPEED    = 20f;

    // Vitesse min en dessous de laquelle on considère la rondelle arrêtée
    private static final float MIN_SPEED    = 0.1f;

    // Variation max d'angle sur un rebond (en radians) — rend les rebonds moins parfaits
    private static final float BOUNCE_NOISE = 0.06f;  // ≈ ±3.5°

    private final Puck    puck;
    private final Paddle  paddleP1;
    private final Paddle  paddleP2;
    private final Random  random = new Random();

    // Indique si un but vient d'être marqué (lu par GameRules à l'étape 6)
    private boolean goalP1 = false;
    private boolean goalP2 = false;

    // Référence optionnelle à GameRules pour notifier les touches de raquette
    private projet.M1.game.GameRules gameRules;

    public PhysicsEngine(Puck puck, Paddle paddleP1, Paddle paddleP2) {
        this.puck     = puck;
        this.paddleP1 = paddleP1;
        this.paddleP2 = paddleP2;
    }

    public void setGameRules(projet.M1.game.GameRules rules) {
        this.gameRules = rules;
    }

    /**
     * Mise à jour physique principale — appelée à chaque frame depuis simpleUpdate().
     * @param tpf time per frame en secondes
     */
    public void update(float tpf) {
        goalP1 = false;
        goalP2 = false;

        applyFriction(tpf);
        movePuck(tpf);
        handleCollisions();
        handlePaddleCollision(paddleP1, 1);
        handlePaddleCollision(paddleP2, 2);
    }

    /**
     * Collision circulaire rondelle / raquette.
     *
     * Détection : distance entre centres < Puck.RADIUS + Paddle.RADIUS
     * Résolution :
     *   1. On repositionne la rondelle hors de la raquette
     *   2. On calcule la vitesse relative et on applique une impulsion
     *      en tenant compte de la vitesse de la raquette (effet smash)
     */
    private void handlePaddleCollision(Paddle paddle, int playerNum) {
        Vector3f pp  = puck.getPosition();
        Vector3f pdp = paddle.getPosition();

        float dx   = pp.x - pdp.x;
        float dz   = pp.z - pdp.z;
        float dist = FastMath.sqrt(dx * dx + dz * dz);
        float minD = Puck.RADIUS + Paddle.RADIUS;

        if (dist >= minD) return;

        // Normale de séparation (du centre paddle vers centre puck)
        float nx, nz;
        if (dist < 0.001f) {
            nx = 0; nz = 1; // cas dégénéré
        } else {
            nx = dx / dist;
            nz = dz / dist;
        }

        // Repositionner la rondelle hors du paddle
        puck.setPosition(pdp.x + nx * minD, pdp.z + nz * minD);

        // Vitesse relative puck - paddle selon la normale
        Vector3f pv  = puck.getVelocity();
        Vector3f padv = paddle.getVelocity();
        float relDot = (pv.x - padv.x) * nx + (pv.z - padv.z) * nz;

        // Appliquer l'impulsion seulement si les objets se rapprochent
        if (relDot < 0) {
            float impulse = -(1f + RESTITUTION) * relDot;
            puck.setVelocity(
                pv.x + impulse * nx,
                pv.z + impulse * nz
            );
            // Notifier GameRules du dernier joueur à avoir touché la rondelle
            if (gameRules != null) gameRules.notifyPaddleTouch(playerNum);
        }
    }

    // Intégration d'Euler : position(t+dt) = position(t) + vitesse(t) * dt
    private void movePuck(float tpf) {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        puck.setPosition(
            pos.x + vel.x * tpf,
            pos.z + vel.z * tpf
        );
    }

    // Amortissement + cap de vitesse
    private void applyFriction(float tpf) {
        Vector3f vel = puck.getVelocity();

        float factor = 1f - FRICTION * tpf;
        vel.x *= factor;
        vel.z *= factor;

        // Stopper proprement si trop lent
        if (Math.abs(vel.x) < MIN_SPEED && Math.abs(vel.z) < MIN_SPEED) {
            vel.x = 0;
            vel.z = 0;
            return;
        }

        // Plafonner la vitesse
        float speed = FastMath.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed > MAX_SPEED) {
            float scale = MAX_SPEED / speed;
            vel.x *= scale;
            vel.z *= scale;
        }
    }

    /**
     * Ajoute un léger bruit d'angle au vecteur vitesse après rebond.
     * Rotation 2D dans le plan XZ d'un angle aléatoire en ±BOUNCE_NOISE radians.
     * Rend les rebonds moins parfaitement spéculaires, plus naturels.
     */
    private Vector3f addBounceNoise(Vector3f vel) {
        float angle = (random.nextFloat() - 0.5f) * 2f * BOUNCE_NOISE;
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        return new Vector3f(
            vel.x * cos - vel.z * sin,
            0,
            vel.x * sin + vel.z * cos
        );
    }

    /**
     * Détection et résolution des collisions avec les bandes et les murs de fond.
     *
     * On compare le centre de la rondelle ± rayon avec les limites de la table.
     * En cas de collision :
     *   1. On repositionne la rondelle hors de la bande
     *   2. On applique la formule de réflexion : v' = v - 2(v·n)n
     */
    private void handleCollisions() {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        float r = Puck.RADIUS;

        // --- Bande gauche : normale = (1, 0, 0) ---
        if (pos.x - r < -Table.HALF_W) {
            puck.setPosition(-Table.HALF_W + r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            pos = puck.getPosition();
            vel = puck.getVelocity();
        }

        // --- Bande droite : normale = (-1, 0, 0) ---
        if (pos.x + r > Table.HALF_W) {
            puck.setPosition(Table.HALF_W - r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(-1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            pos = puck.getPosition();
            vel = puck.getVelocity();
        }

        // --- Mur de fond joueur 1 (Z = -HALF_L) : normale = (0, 0, 1) ---
        if (pos.z - r < -Table.HALF_L) {
            if (Math.abs(pos.x) < Table.GOAL_WIDTH / 2f) {
                goalP1 = true;  // but dans le camp P1 → point pour P2
            } else {
                puck.setPosition(pos.x, -Table.HALF_L + r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, 1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            }
            pos = puck.getPosition();
        }

        // --- Mur de fond joueur 2 (Z = +HALF_L) : normale = (0, 0, -1) ---
        if (pos.z + r > Table.HALF_L) {
            if (Math.abs(pos.x) < Table.GOAL_WIDTH / 2f) {
                goalP2 = true;  // but dans le camp P2 → point pour P1
            } else {
                puck.setPosition(pos.x, Table.HALF_L - r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, -1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            }
        }
    }

    public boolean isGoalP1() { return goalP1; }
    public boolean isGoalP2() { return goalP2; }
}
