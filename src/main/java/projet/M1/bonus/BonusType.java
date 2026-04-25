package projet.M1.bonus;

import com.jme3.math.ColorRGBA;

/**
 * Types de bonus disponibles sur le terrain.
 * Chaque type a une couleur distincte pour être identifiable visuellement.
 */
public enum BonusType {

    SPEED_PLUS(
            new ColorRGBA(1f, 0.85f, 0f, 0.9f),   // jaune
            "Speed +",
            "Palet x1.5 vitesse (10s)"
    ),
    PADDLE_PLUS(
            new ColorRGBA(0.1f, 0.9f, 0.2f, 0.9f), // vert
            "Paddle +",
            "Raquette +10% taille (20s)"
    ),
    PADDLE_MINUS(
            new ColorRGBA(0.9f, 0.1f, 0.9f, 0.9f), // violet
            "Paddle -",
            "Raquette adverse -10% taille (20s)"
    );

    public final ColorRGBA color;
    public final String    shortName;
    public final String    description;

    BonusType(ColorRGBA color, String shortName, String description) {
        this.color       = color;
        this.shortName   = shortName;
        this.description = description;
    }
}