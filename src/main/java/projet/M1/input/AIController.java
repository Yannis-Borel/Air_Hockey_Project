package projet.M1.input;

import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;

/**
 * Contrôleur IA pour le mode solo.
 *
 * Comportement :
 *   - Frappe : si le palet est immobile dans le camp de l'IA, elle fonce dessus pour le frapper
 *   - Attaque : si le palet se dirige vers le joueur (vz > 0), l'IA suit le palet en X et Z
 *   - Défense : si le palet se dirige vers l'IA (vz < 0), elle revient devant son but
 *
 * La vitesse est volontairement inférieure à celle du joueur pour ne pas être imbattable.
 */
public class AIController {

    // Vitesse de déplacement de l'IA (plus lente que le joueur à 10f)
    private static final float SPEED = 7f;

    // Position de repli en Z quand l'IA défend (devant son but)
    private static final float DEFENSE_Z = -7f;

    // Tolérance de position : en dessous de cette distance on considère l'IA arrivée
    private static final float TOLERANCE = 0.1f;

    // Vitesse min du palet en dessous de laquelle on considère qu'il est immobile
    private static final float PUCK_STILL_THRESHOLD = 0.5f;

    private final Paddle paddle;
    private final Puck   puck;

    // Limites de déplacement (même logique que PlayerInputHandler)
    private final float minX, maxX, minZ, maxZ;

    public AIController(Paddle paddle, Puck puck) {
        this.paddle = paddle;
        this.puck   = puck;

        float r = Paddle.RADIUS;
        minX = -Table.HALF_W + r;
        maxX =  Table.HALF_W - r;
        // L'IA est côté Z négatif (P1)
        minZ = -Table.HALF_L + r;
        maxZ = -Table.NEUTRAL_Z - r;
    }

    public void update(float tpf) {
        Vector3f puckPos = puck.getPosition();
        Vector3f puckVel = puck.getVelocity();
        Vector3f padPos  = paddle.getPosition();

        float targetX, targetZ;

        float puckSpeed = puckVel.x * puckVel.x + puckVel.z * puckVel.z;
        boolean puckStill = puckSpeed < PUCK_STILL_THRESHOLD * PUCK_STILL_THRESHOLD;

        // Si le palet est immobile dans le camp de l'IA → elle fonce dessus pour frapper
        if (puckStill && puckPos.z < 0) {
            targetX = puckPos.x;
            targetZ = puckPos.z;
        } else if (puckVel.z > 0) {
            // Attaque : le palet va vers le joueur → l'IA suit le palet en X et Z
            targetX = puckPos.x;
            targetZ = puckPos.z;
        } else {
            // Défense : le palet revient vers l'IA → elle se place devant son but
            targetX = puckPos.x;
            targetZ = DEFENSE_Z;
        }

        // Déplacer vers la cible à vitesse constante
        float newX = moveToward(padPos.x, targetX, SPEED * tpf);
        float newZ = moveToward(padPos.z, targetZ, SPEED * tpf);

        // Contraindre dans les limites de la zone IA
        newX = Math.max(minX, Math.min(maxX, newX));
        newZ = Math.max(minZ, Math.min(maxZ, newZ));

        paddle.moveTo(newX, newZ, tpf);
    }

    /**
     * Déplace current vers target d'un maximum de maxStep.
     */
    private float moveToward(float current, float target, float maxStep) {
        float diff = target - current;
        if (Math.abs(diff) <= TOLERANCE) return current;
        return current + Math.signum(diff) * Math.min(Math.abs(diff), maxStep);
    }
}