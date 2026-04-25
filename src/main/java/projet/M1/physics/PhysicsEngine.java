package projet.M1.physics;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import projet.M1.bonus.BonusManager;
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
 *   - Le smash  : boost de 5%-15% si la raquette frappe fort et en face
 *   - Le lift   : spin latéral si la raquette frappe en angle
 *   - Le flip   : amortissement si la raquette frappe mollement
 *
 * La physique est purement 2D dans le plan XZ.
 * La friction est désactivée pendant l'effet Speed+ (consulté via BonusManager).
 */
public class PhysicsEngine {

    // Friction modérée
    private static final float FRICTION        = 0.35f;

    // Restitution : légère perte d'énergie à chaque rebond
    private static final float RESTITUTION     = 0.88f;

    // Vitesse max
    private static final float MAX_SPEED       = 20f;

    // Vitesse min en dessous de laquelle on considère la rondelle arrêtée
    private static final float MIN_SPEED       = 0.1f;

    // Variation max d'angle sur un rebond (en radians)
    private static final float BOUNCE_NOISE    = 0.06f;

    // --- Smash ---
    // Vitesse min de la raquette pour déclencher un smash
    private static final float SMASH_THRESHOLD = 5f;
    // Alignement min entre vitesse raquette et normale (produit scalaire normalisé)
    private static final float SMASH_ALIGN     = 0.7f;

    // --- Lift ---
    // Composante tangentielle min pour générer du spin
    private static final float LIFT_THRESHOLD  = 2f;
    // Intensité max du spin appliqué
    private static final float LIFT_MAX_SPIN   = 3.5f;

    // --- Flip ---
    // Vitesse max de la raquette sous laquelle on considère un flip
    private static final float FLIP_THRESHOLD  = 1.5f;
    // Coefficient de réduction de vitesse lors d'un flip
    private static final float FLIP_DAMPING    = 0.55f;

    private final Puck    puck;
    private final Paddle  paddleP1;
    private final Paddle  paddleP2;
    private final Random  random = new Random();

    private boolean goalP1 = false;
    private boolean goalP2 = false;

    private projet.M1.game.GameRules gameRules;
    private BonusManager bonusManager;

    public PhysicsEngine(Puck puck, Paddle paddleP1, Paddle paddleP2) {
        this.puck     = puck;
        this.paddleP1 = paddleP1;
        this.paddleP2 = paddleP2;
    }

    public void setGameRules(projet.M1.game.GameRules rules) {
        this.gameRules = rules;
    }

    public void setBonusManager(BonusManager bonusManager) {
        this.bonusManager = bonusManager;
    }

    public void update(float tpf) {
        goalP1 = false;
        goalP2 = false;

        // La friction est désactivée pendant l'effet Speed+
        boolean speedActive = (bonusManager != null && bonusManager.isSpeedActive());
        if (!speedActive) {
            applyFriction(tpf);
        }

        applySpinEffect(tpf);
        puck.applySpinDecay(tpf);
        movePuck(tpf);
        handleCollisions();
        handlePaddleCollision(paddleP1, 1);
        handlePaddleCollision(paddleP2, 2);
    }

    /**
     * Collision circulaire rondelle / raquette.
     *
     * Trois effets possibles selon la vitesse de la raquette :
     *
     * SMASH  — raquette rapide et alignée avec la normale :
     *   boost multiplicatif de 5% à 15% sur la vitesse résultante
     *
     * LIFT   — raquette rapide mais en angle (composante tangentielle forte) :
     *   spin latéral ajouté au palet, crée une déviation progressive
     *
     * FLIP   — raquette lente ou immobile :
     *   impulsion réduite + amortissement → le palet ralentit
     */
    private void handlePaddleCollision(Paddle paddle, int playerNum) {
        Vector3f pp  = puck.getPosition();
        Vector3f pdp = paddle.getPosition();

        float dx   = pp.x - pdp.x;
        float dz   = pp.z - pdp.z;
        float dist = FastMath.sqrt(dx * dx + dz * dz);
        float minD = Puck.RADIUS + paddle.getCurrentRadius();

        if (dist >= minD) return;

        // Normale de séparation (du centre paddle vers centre puck)
        float nx, nz;
        if (dist < 0.001f) {
            nx = 0; nz = 1;
        } else {
            nx = dx / dist;
            nz = dz / dist;
        }

        // Repositionner la rondelle hors du paddle
        puck.setPosition(pdp.x + nx * minD, pdp.z + nz * minD);

        Vector3f pv   = puck.getVelocity();
        Vector3f padv = paddle.getVelocity();

        // Vitesse relative selon la normale
        float relDot = (pv.x - padv.x) * nx + (pv.z - padv.z) * nz;
        if (relDot >= 0) return; // objets qui s'éloignent

        // Vitesse scalaire de la raquette
        float padSpeed = FastMath.sqrt(padv.x * padv.x + padv.z * padv.z);

        // Composante normale de la vitesse raquette
        float padNormal = padv.x * nx + padv.z * nz;

        // Composante tangentielle de la vitesse raquette (perpendiculaire à la normale)
        float padTangX = padv.x - padNormal * nx;
        float padTangZ = padv.z - padNormal * nz;
        float padTangSpeed = FastMath.sqrt(padTangX * padTangX + padTangZ * padTangZ);

        // Alignement raquette/normale (1 = parfaitement aligné, 0 = perpendiculaire)
        float alignment = (padSpeed > 0.01f) ? Math.abs(padNormal) / padSpeed : 0f;

        // --- Calcul de l'impulsion de base ---
        float impulse = -(1f + RESTITUTION) * relDot;
        float newVx   = pv.x + impulse * nx;
        float newVz   = pv.z + impulse * nz;

        if (padSpeed < FLIP_THRESHOLD) {
            // === FLIP : raquette lente → amortissement ===
            newVx *= FLIP_DAMPING;
            newVz *= FLIP_DAMPING;
            puck.setSpin(0f); // annule le spin en cours
            System.out.println("[Physics] FLIP — vitesse raquette : " + padSpeed);

        } else if (padTangSpeed > LIFT_THRESHOLD && alignment < SMASH_ALIGN) {
            // === LIFT : composante tangentielle forte → spin latéral ===
            // Le spin est proportionnel à la vitesse tangentielle
            float spinFactor = Math.min(padTangSpeed / LIFT_THRESHOLD, 1f);
            // Direction du spin : signe de la composante tangentielle en X
            float spinSign = (padTangX != 0f) ? Math.signum(padTangX) : Math.signum(padTangZ);
            float newSpin  = spinSign * spinFactor * LIFT_MAX_SPIN;
            puck.setSpin(newSpin);
            System.out.println("[Physics] LIFT — spin : " + newSpin);

        } else if (padSpeed >= SMASH_THRESHOLD && alignment >= SMASH_ALIGN) {
            // === SMASH : coup fort et aligné → boost 5%-15% ===
            float boost = 1f + 0.05f + random.nextFloat() * 0.10f;
            newVx *= boost;
            newVz *= boost;
            System.out.println("[Physics] SMASH — boost : " + boost);
        }

        puck.setVelocity(newVx, newVz);

        if (gameRules != null) gameRules.notifyPaddleTouch(playerNum);
    }

    /**
     * Applique l'effet de spin (lift) à chaque frame.
     * Le spin dévie la trajectoire du palet latéralement.
     * La déviation est perpendiculaire à la direction de déplacement courante.
     */
    private void applySpinEffect(float tpf) {
        float spin = puck.getSpin();
        if (spin == 0f) return;

        Vector3f vel = puck.getVelocity();
        float speed  = FastMath.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed < MIN_SPEED) return;

        // Vecteur perpendiculaire à la direction de déplacement dans le plan XZ
        // Si direction = (vx, vz), perpendiculaire = (-vz, vx) normalisé
        float perpX = -vel.z / speed;
        float perpZ =  vel.x / speed;

        // Déviation proportionnelle au spin et à la vitesse
        float deflection = spin * tpf;
        puck.setVelocity(
                vel.x + perpX * deflection * speed * 0.08f,
                vel.z + perpZ * deflection * speed * 0.08f
        );
    }

    // Intégration d'Euler
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

        if (Math.abs(vel.x) < MIN_SPEED && Math.abs(vel.z) < MIN_SPEED) {
            vel.x = 0;
            vel.z = 0;
            return;
        }

        float speed = FastMath.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed > MAX_SPEED) {
            float scale = MAX_SPEED / speed;
            vel.x *= scale;
            vel.z *= scale;
        }
    }

    /**
     * Ajoute un léger bruit d'angle au vecteur vitesse après rebond sur une bande.
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
     * Un rebond sur une bande annule le spin en cours.
     */
    private void handleCollisions() {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        float r = Puck.RADIUS;

        // --- Bande gauche ---
        if (pos.x - r < -Table.HALF_W) {
            puck.setPosition(-Table.HALF_W + r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            puck.setSpin(0f);
            pos = puck.getPosition();
            vel = puck.getVelocity();
        }

        // --- Bande droite ---
        if (pos.x + r > Table.HALF_W) {
            puck.setPosition(Table.HALF_W - r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(-1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            puck.setSpin(0f);
            pos = puck.getPosition();
            vel = puck.getVelocity();
        }

        // --- Mur de fond P1 (Z = -HALF_L) ---
        if (pos.z - r < -Table.HALF_L) {
            if (Math.abs(pos.x) < Table.GOAL_WIDTH / 2f) {
                goalP1 = true;
            } else {
                puck.setPosition(pos.x, -Table.HALF_L + r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, 1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
                puck.setSpin(0f);
            }
            pos = puck.getPosition();
        }

        // --- Mur de fond P2 (Z = +HALF_L) ---
        if (pos.z + r > Table.HALF_L) {
            if (Math.abs(pos.x) < Table.GOAL_WIDTH / 2f) {
                goalP2 = true;
            } else {
                puck.setPosition(pos.x, Table.HALF_L - r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, -1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
                puck.setSpin(0f);
            }
        }
    }

    public boolean isGoalP1() { return goalP1; }
    public boolean isGoalP2() { return goalP2; }
}