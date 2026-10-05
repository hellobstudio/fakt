# Fakt-App

Digitális kultúra / informatika érettségi feladatok nyilvántartása szaktanároknak (Android).

## APK készítése GitHubon (lépésről lépésre)

1. Hozz létre egy új, üres GitHub repót (pl. `fakt-app`).
2. Töltsd fel ennek a mappának a **teljes tartalmát** a repo gyökerébe (a `.github` rejtett mappával együtt!).
   Weben: *Add file → Upload files*, vagy parancssorból:
   ```
   git init && git add . && git commit -m "Fakt-App" 
   git branch -M main
   git remote add origin https://github.com/FELHASZNALO/fakt-app.git
   git push -u origin main
   ```
3. A feltöltés után automatikusan elindul a **Build APK** workflow (Actions fül). Kb. 4-6 perc.
   Kézzel is indítható: *Actions → Build APK → Run workflow*.
4. Ha lefutott, nyisd meg a futást, és az oldal alján az **Artifacts** között töltsd le a `Fakt-App-debug-apk` csomagot.
   Kicsomagolva benne van az `app-debug.apk`.
5. Másold a telefonra, nyisd meg, és engedélyezd az ismeretlen forrásból való telepítést.

Az APK debug-kulccsal van aláírva, saját használatra ez bőven elég. (Play Áruházba nem alkalmas.)

## Az app

* **Feladatok** fül: Középszint / Emelt szint váltó, témakör-szűrő, típus-szűrő (digitális kultúra / régi informatika),
  állapot-szűrő és keresés. A feladatok témakörönként csoportosítva, táblázatsorokként látszanak.
* Egy sorra kattintva megnyílik a kijelölés: **Órán megcsináltuk** (zöld), **Dolgozat** (piros), **Házi feladat** (kék),
  vagy **kijelölés törlése**. Bármikor módosítható. Jegyzet is írható (osztály, határidő).
  Innen megnyitható a feladatlap PDF-je és a forrásfájlok is.
* **Kiadottak** fül: a már kijelölt feladatok állapot szerint csoportosítva, szint- és témakör-szűréssel,
  megosztható szöveges listával.
* **Áttekintés** fül: összesítés szintenként és témakörönként, valamint biztonsági mentés / visszaállítás.

A kijelölések a telefonon tárolódnak (Androidos Google-mentés is menti őket). Biztos, ami biztos: az Áttekintés fülön van kézi mentés.

## Feladatlista frissítése

A feladatok az `app/src/main/assets/tasks.json` fájlban vannak. Új évek hozzáadásához frissítsd a docxet,
majd futtasd: `python tools/docx_to_tasks.py DIGITÁLIS_KULTÚRA.docx app/src/main/assets/tasks.json`.
A régi kijelölések megmaradnak, mert az azonosítók évből, időszakból, témakörből és szintből állnak.

## Technikai adatok

Kotlin + Jetpack Compose (Material 3), minSdk 26 (Android 8.0), nincs külön adatbázis és nincs Gradle wrapper:
a workflow a `gradle/actions/setup-gradle` lépéssel a Gradle 8.7-et használja. Android Studiouban is megnyitható.
