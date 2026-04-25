package projet.M1.game;

/**
 * Profils des 5 adversaires du mode tournoi.
 * Chaque adversaire a une vitesse et un comportement distinct.
 *
 *   1 - Le Débutant   : lent, défensif seulement
 *   2 - L'Équilibré   : vitesse normale, attaque et défense
 *   3 - Le Rapide     : rapide, attaque et défense
 *   4 - L'Agressif    : très rapide, fonce toujours vers le palet
 *   5 - Le Boss       : très rapide, anticipe la trajectoire du palet
 */
public enum OpponentProfile {

    DEBUTANT   (1, "Le Débutant",  4f,  BehaviorType.DEFENSIVE),
    EQUILIBRE  (2, "L'Équilibré",  7f,  BehaviorType.BALANCED),
    RAPIDE     (3, "Le Rapide",    10f, BehaviorType.BALANCED),
    AGRESSIF   (4, "L'Agressif",   12f, BehaviorType.AGGRESSIVE),
    BOSS       (5, "Le Boss",      14f, BehaviorType.PREDICTIVE);

    public enum BehaviorType {
        DEFENSIVE,   // reste devant son but, suit le palet en X seulement
        BALANCED,    // attaque quand le palet va vers le joueur, défend sinon
        AGGRESSIVE,  // fonce toujours vers le palet
        PREDICTIVE   // anticipe la trajectoire du palet
    }

    public final int          round;
    public final String       name;
    public final float        speed;
    public final BehaviorType behavior;

    OpponentProfile(int round, String name, float speed, BehaviorType behavior) {
        this.round    = round;
        this.name     = name;
        this.speed    = speed;
        this.behavior = behavior;
    }

    /** Retourne le profil correspondant au round donné (1 à 5). */
    public static OpponentProfile forRound(int round) {
        for (OpponentProfile p : values()) {
            if (p.round == round) return p;
        }
        throw new IllegalArgumentException("Round invalide : " + round);
    }

    public static int totalRounds() {
        return values().length;
    }
}