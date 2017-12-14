# WorldDuplicator

WorldDuplicator ist eine eigenständige Java-Anwendung, mit der es möglich ist, die Blöcke in einer Minecraft Welt zu duplizieren.
Zum Kompilieren wird die Library https://github.com/RewisServer/NBTStorage benötigt.

## Für was braucht man sowas?

Auf Rewinside.tv haben wir z.B. das Minispiel CastleRush, in dem 12 Leute spielen und jeder in einer eigenen Arena etwas bauen muss.
Die Arena wird von den Buildern aber nur einmal gebaut. Jetzt könnte man die Arena beim Minigame Start kopieren, das würde aber sehr viel Leistung fressen.
Deshalb bearbeiten wir die Welt mit diesem Programm, bevor sie auf den Hauptserver hochgeladen wird. Das Programm sucht den Arenenbereich und kopiert ihn x-mal.

## Download

WorldDuplicator kann von unserem Jenkins heruntergeladen oder selbst kompiliert werden.
Downloadlink: https://ci.rewinside.tv/job/WorldDuplicator/lastSuccessfulBuild/artifact/target/WorldDuplicator.jar

## Kompilierung

```
git clone https://github.com/RewisServer/NBTStorage.git
cd NBTStorage
mvn clean install
cd ..
git clone https://github.com/RewisServer/WorldDuplicator.git
cd WorldDuplicator
mvn clean install
```

## Ablauf

* Ordner world-in: Die Welt, die dupliziert werden soll. Um den Spawn müssen Blöcke sein und die Welt muss außerhalb mit Luft gefüllt sein.
* Das Programm ließt die Welt ein und sucht sich den Bereich bis zur Luft raus
* Vom gefundenen Bereich wird ein Abbild erzeugt und im Arbeitsspeicher gespeichert
* Das Abbild wird x-mal immer nebeneinander eingefügt (+x Achse)
* Die Welt wird abgespeichert.

## Verwendung

Download: https://ci.rewinside.tv/job/WorldDuplicator/lastSuccessfulBuild/artifact/target/WorldDuplicator.jar  
```
java -jar WorldDuplicator.jar -in world-in/ -out world-new -amount 11
```
* in: Welt, die kopiert werden woll
* out: Der Speicherort für die neue Welt
* amount: Wie oft der Bereich kopiert werden soll
