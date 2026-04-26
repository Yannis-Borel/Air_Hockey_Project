package projet.M1.physics;

import com.jme3.math.Vector3f;

/**
 * Utilitaires mathématiques pour la physique 2D de l'Air Hockey.
 * Les calculs se font dans le plan XZ (Y ignoré, la table est plate).
 */
public class VectorMath {

    /**
     * Calcule le vecteur vitesse après rebond sur une surface plane.
     *
     * Formule de réflexion vectorielle :
     *   v' = v - 2(v · n) * n
     *
     * où :
     *   v = vitesse incidente (avant impact)
     *   n = normale unitaire de la surface frappée
     *   v' = vitesse réfléchie (après rebond)
     *
     * Intuition : on inverse uniquement la composante de v perpendiculaire à la surface.
     *
     * @param velocity : vitesse incidente
     * @param normal : normale unitaire de la surface (doit être normalisée)
     * @return vitesse après rebond
     */
    public static Vector3f reflect(Vector3f velocity, Vector3f normal) {
        float dot = velocity.dot(normal); // v * n
        return velocity.subtract(normal.mult(2f * dot));  // v - 2(v*n)n
    }

    /** Constructeur privé : classe utilitaire non instanciable. */
    private VectorMath() {}
}