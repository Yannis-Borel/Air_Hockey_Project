package projet.M1;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

import java.util.Random;

/**
 * Contrôleur IA : raquette P1 (rouge, côté Z-).
 *
 * Les limites de déplacement sont recalculées dynamiquement depuis le rayon
 * courant de la raquette (power-ups, modifications du tournoi).
 *
 * Cinq niveaux :
 *   DEBUTANT : lent, reste centré en défense, erreur élevée
 *   INTERMEDIAIRE : prédiction linéaire courte, légère couverture
 *   SEMI_PRO : prédiction moyenne, erreur réduite
 *   PRO : prédiction précise, bonne couverture
 *   LEGENDE : simulation complète des rebonds, erreur nulle
 */
public class AIController {

    public enum Level {
        DEBUTANT("Débutant", 3.5f),
        INTERMEDIAIRE("Intermédiaire", 5.0f),
        SEMI_PRO("Semi-pro", 6.5f),
        PRO("Pro", 8.0f),
        LEGENDE("Légende", 8.5f);

        public final String label;
        public final float speed;

        Level(String label, float speed) {
            this.label = label;
            this.speed = speed;
        }
    }

    private static final float[] ERROR_RADIUS = { 3.0f, 1.5f, 0.65f, 0.20f, 0.0f };
    private static final float[] SMOOTH_SPEED = { 1.5f, 2.5f, 4.5f,  7.0f,  2.5f };
    private static final float[] PREDICT_DT = { 0.05f, 0.12f, 0.22f, 0.35f, 0.0f };

    private final Paddle paddle;
    private final Puck puck;
    private final Level level;
    private final float speed;
    private final int idx;
    private final Random random = new Random();

    // Cible lissée
    private float smoothX, smoothZ;
    private boolean firstUpdate = true;

    // Erreur de ciblage renouvelée progressivement
    private float errorX = 0f, errorZ = 0f;
    private float errorTimer = 0f;
    private static final float ERROR_RENEW = 0.50f;

    /**
     * Initialise le contrôleur IA avec le niveau donné.
     * La cible lissée est initialisée sur la position courante de la raquette.
     */
    public AIController(Paddle paddle, Puck puck, Level level) {
        this.paddle = paddle;
        this.puck = puck;
        this.level = level;
        this.speed = level.speed;
        this.idx = level.ordinal();

        this.smoothX = paddle.getPosition().x;
        this.smoothZ = paddle.getPosition().z;
    }

    /**
     * Mise à jour principale appelée à chaque frame.
     * Renouvelle l'erreur de ciblage, calcule la cible brute,
     * la lisse par interpolation puis déplace la raquette vers elle.
     */
    public void update(float tpf) {
        Vector3f pp = puck.getPosition();
        Vector3f vel = puck.getVelocity();

        // Renouveler l'erreur progressivement
        float r = ERROR_RADIUS[idx];
        if (r > 0f) {
            errorTimer += tpf;
            if (errorTimer >= ERROR_RENEW) {
                errorTimer = 0f;
                errorX = errorX * 0.4f + (random.nextFloat() - 0.5f) * 2f * r * 0.6f;
                errorZ = errorZ * 0.4f + (random.nextFloat() - 0.5f) * 2f * r * 0.4f;
            }
        }

        float[] raw = computeTarget(pp, vel);

        if (firstUpdate) {
            smoothX = raw[0];
            smoothZ = raw[1];
            firstUpdate = false;
        } else {
            float alpha = Math.min(1f, SMOOTH_SPEED[idx] * tpf);
            smoothX += (raw[0] - smoothX) * alpha;
            smoothZ += (raw[1] - smoothZ) * alpha;
        }

        moveToward(smoothX, smoothZ, tpf);
    }

    /**
     * Choisit entre attaque et défense selon la position et la direction du palet.
     * Attaque si le palet est dans le camp de l'IA ou s'approche d'elle.
     */
    private float[] computeTarget(Vector3f pp, Vector3f vel) {
        boolean inZone = pp.z < -Table.NEUTRAL_Z;
        boolean approaching = vel.z < -0.5f;

        if (inZone || approaching) return attackTarget(pp, vel);
        else return defendTarget(pp);
    }

    /**
     * Calcule la cible d'attaque selon le niveau :
     * - LEGENDE : simule les rebonds pour prédire la position exacte d'arrivée
     * - Autres  : prédiction linéaire avec un horizon PREDICT_DT + erreur aléatoire
     */
    private float[] attackTarget(Vector3f pp, Vector3f vel) {
        float tx, tz;
        float padR = paddle.getRadius();
        float minZ = -Table.HALF_L + padR;

        if (level == Level.LEGENDE) {
            tx = simulateBounces(pp.x, pp.z, vel.x, vel.z, padR);
            tz = minZ + 0.7f;
        } else {
            float dt = PREDICT_DT[idx];
            tx = pp.x + vel.x * dt;
            tz = pp.z + vel.z * dt;
        }

        tx += errorX;
        tz += errorZ;
        return clampBounds(tx, tz);
    }

    /**
     * Calcule la cible de défense : la raquette revient près du but
     * et suit partiellement le X du palet selon le niveau (trackFactor).
     */
    private float[] defendTarget(Vector3f pp) {
        float trackFactor = switch (level) {
            case DEBUTANT -> 0.0f;
            case INTERMEDIAIRE -> 0.20f;
            case SEMI_PRO -> 0.40f;
            case PRO -> 0.60f;
            case LEGENDE -> 0.82f;
        };

        float padR = paddle.getRadius();
        float maxZ = -Table.NEUTRAL_Z - padR;

        float defX = pp.x * trackFactor + errorX * 0.4f;
        float defZ = maxZ - 1.2f;
        return clampBounds(defX, defZ);
    }

    /**
     * Simule les rebonds du palet sur les bandes latérales jusqu'à ce qu'il
     * atteigne le fond du camp de l'IA. Retourne la position X d'arrivée.
     * Utilisé exclusivement par le niveau LEGENDE.
     */
    private float simulateBounces(float px, float pz, float vx, float vz, float padR) {
        float r = puck.getRadius();
        float minX = -Table.HALF_W + padR;
        float maxX = Table.HALF_W - padR;

        for (int i = 0; i < 300; i++) {
            float dt = 0.022f;
            px += vx * dt;
            pz += vz * dt;

            if (px - r < -Table.HALF_W)
            {
                px = -Table.HALF_W + r;
                vx =  Math.abs(vx);
            }
            else if (px + r >  Table.HALF_W)
            {
                px =  Table.HALF_W - r;
                vx = -Math.abs(vx);
            }

            if (pz - r <= -Table.HALF_L) return FastMath.clamp(px, minX, maxX);
            if (pz + r >=  Table.HALF_L)
            {
                pz = Table.HALF_L - r;
                vz = -Math.abs(vz);
            }
        }
        return FastMath.clamp(px, -Table.HALF_W + padR, Table.HALF_W - padR);
    }

    /**
     * Déplace la raquette vers la cible (targetX, targetZ) à vitesse constante.
     * Les bornes sont recalculées dynamiquement depuis le rayon courant.
     */
    private void moveToward(float targetX, float targetZ, float tpf) {
        // Recalculer les bornes depuis le rayon courant du paddle (power-ups, tournoi)
        float padR = paddle.getRadius();
        float minX = -Table.HALF_W + padR;
        float maxX = Table.HALF_W - padR;
        float minZ = -Table.HALF_L + padR;
        float maxZ = -Table.NEUTRAL_Z - padR;

        Vector3f pos = paddle.getPosition();
        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = FastMath.sqrt(dx * dx + dz * dz);

        if (dist < 0.03f) return;

        float step = Math.min(speed * tpf, dist);
        float newX = FastMath.clamp(pos.x + dx / dist * step, minX, maxX);
        float newZ = FastMath.clamp(pos.z + dz / dist * step, minZ, maxZ);

        paddle.moveTo(newX, newZ, tpf);
    }

    /**
     * Contraint les coordonnées (x, z) dans les limites de la zone de jeu de l'IA.
     */
    private float[] clampBounds(float x, float z) {
        float padR = paddle.getRadius();
        return new float[]{
                FastMath.clamp(x, -Table.HALF_W  + padR, Table.HALF_W  - padR),
                FastMath.clamp(z, -Table.HALF_L  + padR, -Table.NEUTRAL_Z - padR)
        };
    }
}