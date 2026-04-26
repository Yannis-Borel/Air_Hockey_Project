package projet.M1;

import java.util.List;

/**
 * Gère la progression du tournoi solo.
 * Cinq adversaires à battre dans l'ordre.
 *
 * Chaque adversaire peut modifier :
 *   goalWidth : largeur du but (modifie Table + PhysicsEngine)
 *   paddleScale : taille de la raquette IA (modifie Paddle visuellement et physiquement)
 */
public class TournamentManager {

    /**
     * Profil d'un adversaire du tournoi.
     * Contient son nom, sa description, son niveau IA
     * et les modificateurs appliqués à la table et à la raquette IA.
     */
    public static class Opponent {
        public final String name;
        public final String description;
        public final AIController.Level aiLevel;
        public final float goalWidth;    // largeur du but pour ce round
        public final float paddleScale;  // échelle de la raquette IA

        Opponent(String name, String description,
                 AIController.Level level, float goalWidth, float paddleScale) {
            this.name = name;
            this.description = description;
            this.aiLevel = level;
            this.goalWidth = goalWidth;
            this.paddleScale = paddleScale;
        }
    }

    /**
     * Liste immuable des cinq adversaires dans l'ordre du tournoi.
     * Chaque adversaire est plus difficile que le précédent :
     * le but rétrécit et la raquette IA grossit progressivement.
     */
    private static final List<Opponent> OPPONENTS = List.of(
            // Marco : but large (facile à marquer), raquette IA normale
            new Opponent("Marco",   "Débutant maladroit", AIController.Level.DEBUTANT, 4.5f, 1.0f),

            // Sofia : but normal, raquette IA légèrement plus grande
            new Opponent("Sofia",   "Joueuse intermédiaire", AIController.Level.INTERMEDIAIRE,  3.5f, 1.05f),

            // Ivan : but normal, raquette IA plus grande
            new Opponent("Ivan", "Semi-pro offensif", AIController.Level.SEMI_PRO,  3.5f, 1.12f),

            // Axel : but rétréci, grosse raquette IA
            new Opponent("Axel", "Joueur professionnel", AIController.Level.PRO, 2.8f, 1.20f),

            // Phantom : but très rétréci, très grosse raquette IA
            new Opponent("Phantom", "Légende imbattable", AIController.Level.LEGENDE, 2.3f, 1.30f)
    );

    private int currentRound = 0;

    /** Retourne le profil de l'adversaire du round courant. */
    public Opponent getCurrentOpponent() { return OPPONENTS.get(currentRound); }

    /** Passe au round suivant. */
    public void nextRound() { currentRound++; }

    /** Retourne l'index du round courant (0 = premier adversaire). */
    public int  getCurrentRound() { return currentRound; }

    /** Retourne le nombre total de rounds du tournoi. */
    public int  getTotalRounds() { return OPPONENTS.size(); }

    /** Retourne vrai si tous les adversaires ont été battus. */
    public boolean isFinished() { return currentRound >= OPPONENTS.size(); }

    /** Remet le tournoi au premier adversaire. */
    public void reset() { currentRound = 0; }
}