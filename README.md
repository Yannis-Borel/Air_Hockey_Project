# Air Hockey Project

## CONTEXTE DU PROJET

Projet universitaire de fin de module "Vision par Ordinateur" — Master 1 Informatique/Mathématiques, Université de Toulon.
Professeur : Julien SEINTURIER.
Rendu : 15 avril, sous forme de démo + Q&R techniques + code source.
Le projet est développé en groupe mixte INFO/MATH.

---

## STACK TECHNIQUE

- Java 21
- JMonkeyEngine 3 (JME 3) — moteur 3D
- Build : Maven
- Physique : moteur Bullet intégré à JME ou physique codée à la main si plus simple
- Pas de framework externe autre que JME

---

## RÈGLES DU JEU À IMPLÉMENTER 
### La table
- Surface de jeu rectangulaire (dimensions fixes ou variables)
- 2 goal lines aux extrémités (une par joueur)
- 2 paddle zones devant les goal lines (zone où la raquette peut se déplacer)
- 1 zone neutre centrale (la raquette ne peut pas y entrer)
- 2 bandes latérales (rebonds)

### Déroulement d'un échange
- La rondelle ne peut être touchée qu'avec une raquette
- La rondelle peut rebondir sur les bandes autant de fois que nécessaire
- Quand la rondelle franchit la goal line d'un joueur → l'adversaire marque 1 point
- Si un joueur met accidentellement la rondelle dans son propre camp → il perd 1 point
- Un joueur ne peut pas avoir moins de 0 points
- Le premier à 12 points gagne la partie
- Si la rondelle est bloquée → remise en jeu par le joueur qui n'a pas touché en dernier
- Début d'échange : la rondelle est placée sur la ligne de service du joueur qui sert
- Après un point : nouvelle mise en jeu par le joueur qui a concédé le point

---

## MODES DE JEU

### Mode solo (obligatoire)
- Joueur contre IA
- Tournoi : le joueur doit battre au moins 5 adversaires IA différents avant la victoire finale
- Chaque adversaire IA a un comportement distinct

### Mode versus (obligatoire)
- 2 joueurs sur le même clavier (touches différentes)
- Mode réseau optionnel (si le temps le permet)

---

## ADVERSAIRES IA (au moins 5, comportements différents)

Inspiration : ShufflePuck Cafe (Princess Bejin, Lexan...)

Exemples de comportements :
1. Débutant — suit la rondelle lentement, beaucoup d'erreurs
2. Défenseur — reste proche de son but, frappe précisément mais rarement
3. Agressif — fonce sur la rondelle, vitesse élevée, parfois imprévisible
4. Stratège — anticipe la trajectoire de la rondelle, vise les angles
5. Ivre (style Lexan) — très fort au début, se dégrade au fil des points qu'il marque
6. Télépathe (style Princess Bejin) — petite raquette mais peut dévier la rondelle à distance une fois par échange

Chaque adversaire peut modifier la table : taille du but, taille de la raquette, forme de la raquette.

---

## PHYSIQUE (OBLIGATOIRE, à coder rigoureusement)

### Concepts mathématiques issus du cours (à utiliser dans le code)

L'espace 3D est représenté par un espace euclidien E³ avec base orthonormée (X, Y, Z).

Représentation homogène des points 3D :
- Point euclidien (x, y, z) → homogène (x, y, z, 1)ᵀ
- Retour euclidien : (a, b, c, w) → (a/w, b/w, c/w)

Translation homogène (matrice 4×4) :
T(α,β,γ) = [1 0 0 α]    T⁻¹ = [1 0 0 -α]
            [0 1 0 β]           [0 1 0 -β]
            [0 0 1 γ]           [0 0 1 -γ]
            [0 0 0 1]           [0 0 0  1]

Rotations homogènes (matrices 4×4), inverse = transposée :
Rx(ω) = [1    0       0    0]
        [0  cos(ω) -sin(ω) 0]
        [0  sin(ω)  cos(ω) 0]
        [0    0       0    1]

Ry(φ) = [cos(φ)  0  sin(φ)  0]
        [  0     1    0     0]
        [-sin(φ) 0  cos(φ)  0]
        [  0     0    0     1]

Rz(κ) = [cos(κ) -sin(κ)  0  0]
        [sin(κ)  cos(κ)  0  0]
        [  0       0     1  0]
        [  0       0     0  1]

Transformation rigide (rotation + translation) :
B = [R | T]    B⁻¹ = [Rᵀ | -RᵀT]
    [0 | 1]           [ 0 |   1  ]

Les transformations se composent par multiplication de matrices.
Une transformation rigide préserve les distances, les longueurs et les angles.

### Effets physiques à implémenter

Rebond sur les bandes : réflexion vectorielle.
Si la normale de la bande est n et la vitesse de la rondelle est v :
v_rebond = v - 2(v·n)n

Smash : si la raquette frappe la rondelle avec une vitesse suffisante et un bon angle,
la rondelle reçoit un boost de vitesse entre +5% et +15% (proportionnel à la force de la frappe).

Lift : si la raquette frappe la rondelle en biais (angle d'impact ≠ 0°),
la rondelle reçoit un effet de spin (rotation sur elle-même, légère déviation de trajectoire).

Flip : frappe douce → réduction de la vitesse de la rondelle.

---

## POWER-UPS (6 bonus, apparition aléatoire sur la table)

Speed+      : vitesse rondelle ×1.5 pendant 10s
Size+ puck  : taille rondelle ×1.2 pendant 10s
Size- puck  : taille rondelle ×0.8 pendant 10s
Shot on Goal : la rondelle file directement vers le centre du but adverse (instantané)
Paddle+     : raquette du joueur ×1.1 pendant 20s
Paddle-     : raquette de l'adversaire ×0.9 pendant 20s

---

## MODÉLISATION 3D ET RENDU

- Tous les objets visibles doivent être modélisés et texturés : table, bandes, raquettes, rondelle, power-ups
- Utiliser les primitives JME (Box, Cylinder, Sphere) ou charger des modèles .j3o/.obj simples
- Appliquer des matériaux et textures sur chaque objet
- La caméra peut être fixe (vue de dessus ou vue 3/4 isométrique)
- HUD affiché à l'écran : scores des deux joueurs, timer d'effets actifs, nom de l'adversaire en mode tournoi (voir documentation HUD JMonkeyEngine)



---

## CE QUI SERA ÉVALUÉ PAR LE PROFESSEUR

- Fonctionnalités implémentées vs demandées
- Qualité du code et des comportements
- Compréhension technique démontrée lors du Q&R
- Tous les objets modélisés et texturés
- HUD présent
- Les deux modes de jeu fonctionnels
- Les 6 power-ups
- La physique réaliste (smash, lift, flip, rebonds)
- Le tournoi avec 5+ adversaires différents

---

## ORDRE DE DÉVELOPPEMENT ?

1. Setup projet JME avec Maven, Java 21, SimpleApplication vide qui tourne
2. Créer la table avec zones et bandes (géométrie + textures)
3. Rondelle et raquette basiques (formes + textures)
4. Physique de base : mouvement rondelle, rebonds sur les bandes
5. Contrôle clavier du joueur (déplacement dans la paddle zone)
6. Détection des buts + système de score
7. HUD basique (scores)
8. Smash / Lift / Flip
9. IA : BeginnerAI d'abord, puis les 4 autres
10. Mode tournoi
11. Power-ups
12. Mode versus 2 joueurs
13. Polish : textures, effets visuels
14. Tests et corrections

## IDEE

1. Mettre des cubes à la Mario kart avec des bonus aléatoires (adversaire congelé, bonus fois 2, obstacle, cage retrecit, agrandit , balle lente , balle rapide, grosse balle, ptite balle, )
2. Tribune (celebration quand il y a un but)
3. replay avec la camera qui suit (rocket league)
4. explosion de but 