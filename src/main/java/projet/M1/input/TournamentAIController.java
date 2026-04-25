package projet.M1.input;

import com.jme3.math.Vector3f;
import projet.M1.entities.Paddle;
import projet.M1.entities.Puck;
import projet.M1.entities.Table;
import projet.M1.game.OpponentProfile;

/**
 * Contrôleur IA pour le mode tournoi.
 * Chaque adversaire a un profil (vitesse + comportement) distinct.
 *
 * Comportements :
 *   DEFENSIVE  : reste devant son but, suit uniquement le X du palet
 *   BALANCED   : attaque quand le palet va vers le joueur, défend sinon
 *   AGGRESSIVE : fonce toujours vers le palet
 *   PREDICTIVE : anticipe la position future du palet pour le couper
 */
public class TournamentAIController {

    // Position de repli devant le but (Z négatif = camp IA)
    private static final float DEFENSE_Z = -7f;

    // Tolérance de position
    private static final float TOLERANCE = 0.1f;

    // Vitesse min du palet pour considérer qu'il est immobile
    private static final float PUCK_STILL_THRESHOLD = 0.5f;

    // Horizon de prédiction (secondes) pour le comportement PREDICTIVE
    private static final float PREDICT_HORIZON = 0.6f;

    private final Paddle          paddle;
    private final Puck            puck;
    private final OpponentProfile profile;

    private final float minX, maxX, minZ, maxZ;

    public TournamentAIController(Paddle paddle, Puck puck, OpponentProfile profile) {
        this.paddle  = paddle;
        this.puck    = puck;
        this.profile = profile;

        float r = Paddle.RADIUS;
        minX = -Table.HALF_W + r;
        maxX =  Table.HALF_W - r;
        // L'IA est côté Z négatif
        minZ = -Table.HALF_L + r;
        maxZ = -Table.NEUTRAL_Z - r;
    }

    public void update(float tpf) {
        Vector3f puckPos = puck.getPosition();
        Vector3f puckVel = puck.getVelocity();
        Vector3f padPos  = paddle.getPosition();

        float targetX, targetZ;

        // Si le palet est immobile dans le camp IA → frapper dans tous les modes
        float puckSpeed = puckVel.x * puckVel.x + puckVel.z * puckVel.z;
        boolean puckStill = puckSpeed < PUCK_STILL_THRESHOLD * PUCK_STILL_THRESHOLD;
        if (puckStill && puckPos.z < 0) {
            targetX = puckPos.x;
            targetZ = puckPos.z;
        } else {
            switch (profile.behavior) {
                case DEFENSIVE -> {
                    // Reste devant son but, suit seulement le X du palet
                    targetX = puckPos.x;
                    targetZ = DEFENSE_Z;
                }
                case BALANCED -> {
                    // Attaque si le palet va vers le joueur, défend sinon
                    if (puckVel.z > 0) {
                        targetX = puckPos.x;
                        targetZ = puckPos.z;
                    } else {
                        targetX = puckPos.x;
                        targetZ = DEFENSE_Z;
                    }
                }
                case AGGRESSIVE -> {
                    // Fonce toujours vers le palet
                    targetX = puckPos.x;
                    targetZ = puckPos.z;
                }
                case PREDICTIVE -> {
                    // Anticipe la position future du palet
                    float futureX = puckPos.x + puckVel.x * PREDICT_HORIZON;
                    float futureZ = puckPos.z + puckVel.z * PREDICT_HORIZON;
                    // Si le palet va vers le joueur, on intercepte à mi-chemin
                    if (puckVel.z > 0) {
                        targetX = futureX;
                        targetZ = Math.max(minZ, Math.min(maxZ, futureZ));
                    } else {
                        // Défense : se placer sur X prédit devant le but
                        targetX = futureX;
                        targetZ = DEFENSE_Z;
                    }
                }
                default -> {
                    targetX = puckPos.x;
                    targetZ = DEFENSE_Z;
                }
            }
        }

        // Déplacer vers la cible à vitesse constante
        float newX = moveToward(padPos.x, targetX, profile.speed * tpf);
        float newZ = moveToward(padPos.z, targetZ, profile.speed * tpf);

        // Contraindre dans les limites de la zone IA
        newX = Math.max(minX, Math.min(maxX, newX));
        newZ = Math.max(minZ, Math.min(maxZ, newZ));

        paddle.moveTo(newX, newZ, tpf);
    }

    private float moveToward(float current, float target, float maxStep) {
        float diff = target - current;
        if (Math.abs(diff) <= TOLERANCE) return current;
        return current + Math.signum(diff) * Math.min(Math.abs(diff), maxStep);
    }
}