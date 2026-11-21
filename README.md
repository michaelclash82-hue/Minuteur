# Minuteur

Application Android de minuteurs multiples pour la cuisine (batch cooking).

- Autant de minuteurs que nécessaire, en même temps
- Un nom par minuteur (la recette) et une couleur pour le repérer
- Choix de la durée par roulettes heures : minutes : secondes
- Favoris (nom + durée) : un appui relance la recette, appui long pour modifier ou supprimer
- Les 3 derniers minuteurs lancés proposés avec leur durée
- Pause, +1 min, renommer (appui sur le nom), suppression avec « Annuler »
- Sonnerie forte et vibration en boucle, même téléphone verrouillé, jusqu'à « Arrêter »
- Compte à rebours visible dans les notifications

## Installer sur le téléphone

1. Sur le téléphone, ouvrir la page **Releases** du dépôt sur GitHub
   (ou directement `https://github.com/michaelclash82-hue/Minuteur/releases/latest/download/Minuteur.apk`).
2. Télécharger `Minuteur.apk` et l'ouvrir.
3. Android demande d'autoriser l'installation d'applications depuis Chrome
   (ou le gestionnaire de fichiers) : accepter, puis « Installer ».
4. Au premier lancement, accepter les notifications.
5. Si un encadré jaune « Pour que ça sonne même téléphone verrouillé » apparaît,
   appuyer sur chaque « Régler » et accepter.

### Réglage spécifique OnePlus

OxygenOS ferme les applications en arrière-plan de façon agressive. En plus du
réglage proposé par l'app :

- Paramètres → Applications → Minuteur → Utilisation de la batterie →
  **Autoriser l'activité en arrière-plan** (et désactiver l'optimisation).

### Mettre à jour

Chaque modification poussée sur GitHub reconstruit l'APK automatiquement
(onglet **Actions**) et publie une nouvelle version. Il suffit de retélécharger
`Minuteur.apk` et de l'installer par-dessus : les minuteurs ne sont pas perdus.

## Technique

- Kotlin + Jetpack Compose (Material 3), Android 8.0 minimum
- Alarmes via `AlarmManager.setAlarmClock` (même mécanisme que l'app Horloge)
- Sonnerie jouée par un service de premier plan (son d'alarme par défaut du téléphone)
- Données conservées localement (aucun compte, aucune connexion internet)
- La clé de signature (`app/minuteur.keystore`) est volontairement dans le dépôt
  pour que les mises à jour s'installent par-dessus l'ancienne version.
  Elle ne convient pas à une publication sur le Play Store.
