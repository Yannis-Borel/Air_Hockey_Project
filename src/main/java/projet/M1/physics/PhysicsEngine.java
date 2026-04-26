package projet.M1.physics;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

import projet.M1.audio.SoundManager;

import java.util.Random;

/**
 * Moteur physique 2D (plan XZ) pour l'Air Hockey.
 *
 * Utilise les rayons dynamiques de Puck et Paddle ainsi que la largeur de but
 * courante de Table pour rester cohérent avec les power-ups et le tournoi.
 */
public class PhysicsEngine {

    private static final float FRICTION = 0.38f;  // balle glisse plus longtemps
    private static final float RESTITUTION = 0.92f;  // rebonds plus élastiques
    private static final float MAX_SPEED = 22f;
    private static final float MIN_SPEED = 0.20f;

    private static final float BOUNCE_NOISE = 0.06f;

    // Vitesse minimale de la rondelle après contact avec une raquette
    private static final float MIN_LAUNCH_SPEED = 5.0f;

    private static final float SMASH_THRESHOLD = 1.5f;  // déclenchement plus facile
    private static final float SMASH_SCALE = 6f;
    private static final float SMASH_MIN_BOOST = 0.08f;
    private static final float SMASH_MAX_BOOST = 0.35f;  // frappe puissante +35%

    private static final float FLIP_THRESHOLD = 2f;
    private static final float FLIP_REDUCTION = 0.72f;

    private static final float LIFT_THRESHOLD = 2f;
    private static final float LIFT_SCALE = 8f;
    private static final float LIFT_DEFLECTION = 0.30f;

    private final Puck puck;
    private final Paddle paddleP1;
    private final Paddle paddleP2;
    private final Table  table;
    private final Random random = new Random();

    private boolean goalP1 = false;
    private boolean goalP2 = false;

    private projet.M1.game.GameRules gameRules;
    private SoundManager soundManager;

    /** Injecte le gestionnaire de sons pour jouer les effets audio lors des contacts. */
    public void setSoundManager(SoundManager sm) { this.soundManager = sm; }

    /**
     * Initialise le moteur physique avec les entités de la scène.
     */
    public PhysicsEngine(Puck puck, Paddle paddleP1, Paddle paddleP2, Table table) {
        this.puck = puck;
        this.paddleP1 = paddleP1;
        this.paddleP2 = paddleP2;
        this.table = table;
    }

    /** Injecte les règles du jeu pour notifier les touches de raquette. */
    public void setGameRules(projet.M1.game.GameRules rules) {
        this.gameRules = rules;
    }

    /**
     * Mise à jour physique principale appelée à chaque frame.
     * Applique la friction, déplace le palet, détecte les collisions
     * avec les bandes et les raquettes, puis corrige les cas de blocage aux coins.
     */
    public void update(float tpf) {
        goalP1 = false;
        goalP2 = false;

        applyFriction(tpf);
        movePuck(tpf);
        handleCollisions();
        handlePaddleCollision(paddleP1, 1);
        handlePaddleCollision(paddleP2, 2);
        handleCollisions();   // 2e passe : corrige si la raquette a poussé la balle dans un mur
        handleCornerCollisions();
        escapeWallPins();
    }

    /**
     * Gère la collision entre le palet et une raquette.
     *
     * Détection : distance entre centres < somme des rayons dynamiques.
     * Résolution :
     *   1. Repositionne le palet hors de la raquette
     *   2. Calcule l'impulsion de base selon la vitesse relative
     *   3. Applique le smash (+boost) si la raquette frappe fort en face
     *   4. Applique le flip (-amortissement) si la raquette recule
     *   5. Applique le lift (déviation latérale) si la raquette frappe en angle
     *   6. Garantit une vitesse minimale après contact
     */
    private void handlePaddleCollision(Paddle paddle, int playerNum) {
        Vector3f pp = puck.getPosition();
        Vector3f pdp = paddle.getPosition();

        float dx = pp.x - pdp.x;
        float dz = pp.z - pdp.z;
        float dist = FastMath.sqrt(dx * dx + dz * dz);
        float minD = puck.getRadius() + paddle.getRadius();  // rayons dynamiques

        if (dist >= minD) return;

        float nx, nz;
        if (dist < 0.001f) { nx = 0; nz = 1; }
        else { nx = dx / dist; nz = dz / dist; }

        puck.setPosition(pdp.x + nx * minD, pdp.z + nz * minD);

        Vector3f pv = puck.getVelocity();
        Vector3f padv = paddle.getVelocity();
        float relDot = (pv.x - padv.x) * nx + (pv.z - padv.z) * nz;

        if (relDot < 0) {
            float impulse = -(1f + RESTITUTION) * relDot;
            float vx = pv.x + impulse * nx;
            float vz = pv.z + impulse * nz;

            float paddleDotN = padv.x * nx + padv.z * nz;
            float tangX = padv.x - paddleDotN * nx;
            float tangZ = padv.z - paddleDotN * nz;
            float tangSpeed = FastMath.sqrt(tangX * tangX + tangZ * tangZ);

            if (paddleDotN > SMASH_THRESHOLD) {
                float ratio = FastMath.clamp(
                        (paddleDotN - SMASH_THRESHOLD) / SMASH_SCALE, 0f, 1f);
                float boost = 1f + SMASH_MIN_BOOST + ratio * (SMASH_MAX_BOOST - SMASH_MIN_BOOST);
                vx *= boost;
                vz *= boost;
            } else if (paddleDotN < -FLIP_THRESHOLD) {
                vx *= FLIP_REDUCTION;
                vz *= FLIP_REDUCTION;
            }

            if (tangSpeed > LIFT_THRESHOLD) {
                float factor = FastMath.clamp(tangSpeed / LIFT_SCALE, 0f, 1f) * LIFT_DEFLECTION;
                vx += tangX * factor;
                vz += tangZ * factor;
            }

            float speed = FastMath.sqrt(vx * vx + vz * vz);
            // Garantir une vitesse minimale après contact — chaque frappe a du punch
            if (speed < MIN_LAUNCH_SPEED) {
                float s = MIN_LAUNCH_SPEED / Math.max(speed, 0.001f);
                vx *= s;
                vz *= s;
                speed = MIN_LAUNCH_SPEED;
            }
            if (speed > MAX_SPEED) {
                float scale = MAX_SPEED / speed;
                vx *= scale;
                vz *= scale;
            }

            puck.setVelocity(vx, vz);
            if (gameRules != null) gameRules.notifyPaddleTouch(playerNum);
            if (soundManager != null) soundManager.playPaddleHit();
        }
    }

    /**
     * Intégration d'Euler : déplace le palet selon sa vitesse courante.
     */
    private void movePuck(float tpf) {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        puck.setPosition(pos.x + vel.x * tpf, pos.z + vel.z * tpf);
    }

    /**
     * Applique la friction à la vitesse du palet et plafonne à MAX_SPEED.
     * Annule la vitesse si elle passe sous MIN_SPEED sur les deux axes.
     */
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
     * Ajoute un léger bruit d'angle au vecteur vitesse après rebond.
     * Rend les rebonds moins parfaitement spéculaires pour plus de naturel.
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
     * Détecte et résout les collisions du palet avec les quatre bandes et les murs de fond.
     * Utilise les rayons dynamiques du palet et la largeur de but courante de la table.
     * Détecte également les buts (la rondelle passe dans l'ouverture).
     */
    private void handleCollisions() {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        float r = puck.getRadius();  // rayon dynamique
        float gw = table.getCurrentGoalWidth();  // largeur de but dynamique

        if (pos.x - r < -Table.HALF_W) {
            puck.setPosition(-Table.HALF_W + r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            if (soundManager != null) soundManager.playWallHit();
            pos = puck.getPosition(); vel = puck.getVelocity();
        }

        if (pos.x + r > Table.HALF_W) {
            puck.setPosition(Table.HALF_W - r, pos.z);
            Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(-1, 0, 0)));
            puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
            if (soundManager != null) soundManager.playWallHit();
            pos = puck.getPosition(); vel = puck.getVelocity();
        }

        if (pos.z - r < -Table.HALF_L) {
            if (Math.abs(pos.x) < gw / 2f) {
                goalP1 = true;
            } else {
                puck.setPosition(pos.x, -Table.HALF_L + r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, 1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
                if (soundManager != null) soundManager.playWallHit();
            }
            pos = puck.getPosition();
        }

        if (pos.z + r > Table.HALF_L) {
            if (Math.abs(pos.x) < gw / 2f) {
                goalP2 = true;
            } else {
                puck.setPosition(pos.x, Table.HALF_L - r);
                Vector3f v2 = addBounceNoise(VectorMath.reflect(vel, new Vector3f(0, 0, -1)));
                puck.setVelocity(v2.x * RESTITUTION, v2.z * RESTITUTION);
                if (soundManager != null) soundManager.playWallHit();
            }
        }
    }

    /**
     * Empêche la rondelle d'être écrasée dans les coins ou contre les bandes.
     *
     * S'active uniquement quand la rondelle est AU CONTACT du mur (eps = 0.02)
     * pour ne pas déclencher de rebond avant la bande visuellement.
     * Impose une vitesse sortante minimale pour éviter le coin-pinch.
     */
    private void escapeWallPins() {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        float r = puck.getRadius();
        float gw = table.getCurrentGoalWidth();
        float eps = 0.02f;  // marge infime : au contact du mur seulement
        float minV = 3.5f;

        if (pos.x - r < -Table.HALF_W + eps && vel.x <  minV) vel.x =  minV;
        if (pos.x + r > Table.HALF_W - eps && vel.x > -minV) vel.x = -minV;

        boolean outsideGoal = Math.abs(pos.x) >= gw / 2f;
        if (outsideGoal) {
            if (pos.z - r < -Table.HALF_L + eps && vel.z <  minV) vel.z =  minV;
            if (pos.z + r >  Table.HALF_L - eps && vel.z > -minV) vel.z = -minV;
        }
    }

    /**
     * Gère les collisions avec les coins arrondis de la table.
     * Détecte la pénétration dans le rayon CORNER_R de chaque coin
     * et applique une réflexion par rapport à la normale au point de contact.
     */
    private void handleCornerCollisions() {
        Vector3f pos = puck.getPosition();
        Vector3f vel = puck.getVelocity();
        float r = puck.getRadius();
        float[] cx = {-Table.HALF_W, Table.HALF_W, -Table.HALF_W, Table.HALF_W};
        float[] cz = {-Table.HALF_L, -Table.HALF_L, Table.HALF_L, Table.HALF_L};

        for (int i = 0; i < 4; i++) {
            float dx = pos.x - cx[i];
            float dz = pos.z - cz[i];
            float dist = FastMath.sqrt(dx * dx + dz * dz);
            float minD = Table.CORNER_R + r;
            if (dist >= minD) continue;

            float nx = (dist < 0.001f) ? -Math.signum(cx[i]) : dx / dist;
            float nz = (dist < 0.001f) ? -Math.signum(cz[i]) : dz / dist;
            puck.setPosition(cx[i] + nx * minD, cz[i] + nz * minD);
            pos = puck.getPosition();

            float dot = vel.x * nx + vel.z * nz;
            if (dot < 0) {
                vel.x = (vel.x - 2f * dot * nx) * RESTITUTION;
                vel.z = (vel.z - 2f * dot * nz) * RESTITUTION;
                puck.setVelocity(vel.x, vel.z);
                vel = puck.getVelocity();
            }
        }
    }

    /** Retourne vrai si un but a été marqué dans le camp de P1 ce frame. */
    public boolean isGoalP1() { return goalP1; }

    /** Retourne vrai si un but a été marqué dans le camp de P2 ce frame. */
    public boolean isGoalP2() { return goalP2; }
}