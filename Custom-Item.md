# PixelRPG – Custom Item Blueprint

Stand: 2026-08-23

## Ziel

PixelRPG verwendet für die vollständige Progression **Level 1–99 eigene Items**. Es gibt keine Progressionssprünge, bei denen nur alle 5/10/25 Level neue Ausrüstung erscheint.

Jedes Level besitzt eine eigene Item-Stufe. Das Vanilla-Material ist ausschließlich die technische Basis. Die sichtbare Identität kommt aus der PixelRPG-Custom-ID, dem Namen, der Lore, den Stats, der Rarity und später optional aus einem Resourcepack.

**Ohne Resourcepack:** Item funktioniert als normales Vanilla-Material, z. B. `IRON_SWORD`.

**Mit Resourcepack:** dieselbe Custom-ID kann auf ein eigenes Modell/eine eigene Textur zeigen.

## Item-Identität

Jedes Item benötigt langfristig:

- `customId` – dauerhaft eindeutig, unsichtbar für den Spieler
- `baseMaterial` – Vanilla-Basis, z. B. `IRON_SWORD`
- `displayName`
- `lore`
- `rarity`
- `requiredLevel`
- `stats`
- `equipmentSlot`
- `soulbound`
- optional `resourcePackKey`

Beispiel:

```text
customId: pixelrpg:sharp_copper_blade_07
aBaseMaterial: IRON_SWORD
displayName: Scharfe Kupferklinge
requiredLevel: 7
```

Die Custom-ID ist die technische Identität. Name/Lore dürfen geändert werden, ohne die Item-ID zu ändern.

## Pro Level

Jedes Level erhält mindestens:

- 1 Helm
- 1 Brustplatte
- 1 Hose
- 1 Paar Stiefel
- 1 Schwert / Hauptwaffe
- 1 Axt / Nebenwaffe
- 1 Streitkolben / schwere Waffe
- 1 Bogen
- 1 Armbrust
- 1 Speer / Stangenwaffe
- 1 Schild
- 1 Spitzhacke
- 1 Werkzeug-Axt
- 1 Schaufel
- 1 Hacke
- 1 Angel

Das sind **16 Item-Slots pro Level** und damit mindestens **1.584 geplante Custom Items für Level 1–99**. Zusätzliche Waffen, Schmuck oder besondere Drops können später hinzukommen.

---

## Level 1–99 Masterliste

| Lv. | Rüstungsset | Hauptwaffe | Axt | Streitkolben | Bogen | Armbrust | Speer | Schild | Werkzeuge |
|---:|---|---|---|---|---|---|---|---|---|
| 1 | Abgetragene Lederüstung | Rostiges Kurzschwert | Holzfälleraxt | Holzknüppel | Einfacher Kurzbogen | Alte Armbrust | Holzspeer | Holzschild | Alte Spitzhacke / Holzfälleraxt / Holzschaufel / Holzhacke / Alte Angel |
| 2 | Flicklederrüstung | Stumpfes Kurzschwert | Kleine Handfällaxt | Schwerer Knüppel | Jagdbogen | Handarmbrust | Trainingsspeer | Verstärkter Holzschild | Einfache Werkzeuge |
| 3 | Robuste Lederüstung | Gebrauchte Klinge | Gerbenaxt | Eisenbeschlagener Knüppel | Jagdbogen I | Jagdarmbrust | Jagdspeer | Lederschild | Robuste Werkzeuge |
| 4 | Gehärtete Lederüstung | Scharfes Kurzschwert | Gehärtete Axt | Eisenknauf | Kurzbogen des Jägers | Kurze Armbrust | Jägerspeer | Gehärteter Lederschild | Gehärtete Werkzeuge |
| 5 | Leichte Kupferrüstung | Kupferklinge | Kupferaxt | Kupferknüppel | Kupferbogen | Kupferarmbrust | Kupferspeer | Kupferschild | Kupferwerkzeuge |
| 6 | Verstärkte Kupferrüstung | Polierte Kupferklinge | Breite Kupferaxt | Kupferstreitkolben | Polierter Kupferbogen | Polierte Kupferarmbrust | Polierter Kupferspeer | Verstärkter Kupferschild | Polierte Kupferwerkzeuge |
| 7 | Kupferrüstung des Handwerkers | Scharfe Kupferklinge | Handwerkeraxt | Kupferhammer | Handwerkerbogen | Handwerkerarmbrust | Handwerkerspeer | Handwerkerschild | Handwerkerwerkzeuge |
| 8 | Kupferrüstung des Reisenden | Reisendenschwert | Reisendenaxt | Reisendenhammer | Reisendenbogen | Reisendenarmbrust | Reisendenspeer | Reiseschild | Reise-Werkzeuge |
| 9 | Kupferrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 10 | Veredelte Kupferrüstung | Veredelte Kupferklinge | Veredelte Kupferaxt | Veredelter Kupferhammer | Veredelter Kupferbogen | Veredelte Kupferarmbrust | Veredelter Kupferspeer | Veredelter Kupferschild | Veredelte Werkzeuge |
| 11 | Bronzelederrüstung | Bronzeschwert | Bronzeaxt | Bronzehammer | Bronzebogen | Bronzearmbrust | Bronzespeer | Bronzeschild | Bronze-Werkzeuge |
| 12 | Verstärkte Bronzelederrüstung | Breite Bronzeklinge | Breite Bronzeaxt | Schwerer Bronzehammer | Kräftiger Bronzebogen | Kräftige Bronzearmbrust | Breiter Bronzespeer | Verstärkter Bronzeschild | Verstärkte Werkzeuge |
| 13 | Bronzejägerrüstung | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 14 | Bronzehandwerkerrüstung | Handwerksklinge | Handwerkeraxt | Handwerkshammer | Handwerkerbogen | Handwerkerarmbrust | Handwerkerspeer | Handwerkerschild | Handwerkerwerkzeuge |
| 15 | Bronzerüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 16 | Gehärtete Bronzerüstung | Gehärtete Bronzeklinge | Gehärtete Bronzeaxt | Gehärteter Bronzehammer | Gehärteter Bronzebogen | Gehärtete Bronzearmbrust | Gehärteter Bronzespeer | Gehärteter Bronzeschild | Gehärtete Werkzeuge |
| 17 | Dunkle Bronzerüstung | Dunkle Bronzeklinge | Dunkle Bronzeaxt | Dunkler Bronzehammer | Dunkler Bronzebogen | Dunkle Bronzearmbrust | Dunkler Bronzespeer | Dunkler Bronzeschild | Dunkle Werkzeuge |
| 18 | Bronzerüstung des Grenzwächters | Grenzwächterklinge | Grenzwächteraxt | Grenzwächterhammer | Grenzwächterbogen | Grenzwächterarmbrust | Grenzwächterspeer | Grenzwächterschild | Grenzwächterwerkzeuge |
| 19 | Edle Bronzerüstung | Edle Bronzeklinge | Edle Bronzeaxt | Edler Bronzehammer | Edler Bronzebogen | Edle Bronzearmbrust | Edler Bronzespeer | Edler Bronzeschild | Edle Werkzeuge |
| 20 | Meisterliche Bronzerüstung | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 21 | Einfache Eisenrüstung | Eisenklinge | Eisenaxt | Eisenstreitkolben | Eisenbogen | Eisenarmbrust | Eisenspeer | Eisenschild | Eisenwerkzeuge |
| 22 | Verstärkte Eisenrüstung | Verstärkte Eisenklinge | Verstärkte Eisenaxt | Verstärkter Eisenstreitkolben | Verstärkter Eisenbogen | Verstärkte Eisenarmbrust | Verstärkter Eisenspeer | Verstärkter Eisenschild | Verstärkte Werkzeuge |
| 23 | Eisenjägerrüstung | Eisenjägerklinge | Eisenjägeraxt | Eisenjägerhammer | Eisenjägerbogen | Eisenjägerarmbrust | Eisenjägerspeer | Eisenjägerschild | Eisenjägerwerkzeuge |
| 24 | Eisenwächterrüstung | Eisenwächterklinge | Eisenwächteraxt | Eisenwächterhammer | Eisenwächterbogen | Eisenwächterarmbrust | Eisenwächterspeer | Eisenwächterschild | Eisenwächterwerkzeuge |
| 25 | Gehärtete Eisenrüstung | Gehärtete Eisenklinge | Gehärtete Eisenaxt | Gehärteter Eisenhammer | Gehärteter Eisenbogen | Gehärtete Eisenarmbrust | Gehärteter Eisenspeer | Gehärteter Eisenschild | Gehärtete Eisenwerkzeuge |
| 26 | Schwarze Eisenrüstung | Schwarze Eisenklinge | Schwarze Eisenaxt | Schwarzer Eisenhammer | Schwarzer Eisenbogen | Schwarze Eisenarmbrust | Schwarzer Eisenspeer | Schwarzer Eisenschild | Schwarze Eisenwerkzeuge |
| 27 | Eisenrüstung des Grenzwächters | Grenzwächterklinge | Grenzwächteraxt | Grenzwächterhammer | Grenzwächterbogen | Grenzwächterarmbrust | Grenzwächterspeer | Grenzwächterschild | Grenzwächterwerkzeuge |
| 28 | Eisenrüstung des Schmieds | Schmiedeklinge | Schmiedeaxt | Schmiedehammer | Schmiedebogen | Schmiedearmbrust | Schmiedespeer | Schmiedeschild | Schmiedewerkzeuge |
| 29 | Eisenrüstung des Soldaten | Soldatenklinge | Soldatenaxt | Soldatenhammer | Soldatenbogen | Soldatenarmbrust | Soldatenspeer | Soldatenschild | Soldatenwerkzeuge |
| 30 | Meisterliche Eisenrüstung | Meisterliche Eisenklinge | Meisterliche Eisenaxt | Meisterlicher Eisenhammer | Meisterlicher Eisenbogen | Meisterliche Eisenarmbrust | Meisterlicher Eisenspeer | Meisterlicher Eisenschild | Meisterliche Eisenwerkzeuge |
| 31 | Stahlrüstung des Novizen | Stahlklinge | Stahlaxt | Stahlhammer | Stahlbogen | Stahlarmbrust | Stahlspeer | Stahlschild | Stahlwerkzeuge |
| 32 | Stahlrüstung des Wächters | Wächterstahlklinge | Wächterstahlaxt | Wächterstahlhammer | Wächterstahlbogen | Wächterstahlarmbrust | Wächterstahlspeer | Wächterstahlschild | Wächterwerkzeuge |
| 33 | Stahlrüstung des Jägers | Jägerstahlklinge | Jägerstahlaxt | Jägerstahlhammer | Jägerstahlbogen | Jägerstahlarmbrust | Jägerstahlspeer | Jägerstahlschild | Jägerwerkzeuge |
| 34 | Stahlrüstung des Schmieds | Schmiedestahlklinge | Schmiedestahlaxt | Schmiedestahlhammer | Schmiedestahlbogen | Schmiedestahlarmbrust | Schmiedestahlspeer | Schmiedestahlschild | Schmiedewerkzeuge |
| 35 | Gehärtete Stahlrüstung | Gehärtete Stahlklinge | Gehärtete Stahlaxt | Gehärteter Stahlhammer | Gehärteter Stahlbogen | Gehärtete Stahlarmbrust | Gehärteter Stahlspeer | Gehärteter Stahlschild | Gehärtete Stahlwerkzeuge |
| 36 | Stahlrüstung der Festung | Festungsklinge | Festungsaxt | Festungshammer | Festungsbogen | Festungsarmbrust | Festungsspeer | Festungsschild | Festungswerkzeuge |
| 37 | Stahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 38 | Stahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 39 | Stahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 40 | Meisterstahlrüstung | Meisterstahlklinge | Meisterstahlaxt | Meisterstahlhammer | Meisterstahlbogen | Meisterstahlarmbrust | Meisterstahlspeer | Meisterstahlschild | Meisterstahlwerkzeuge |
| 41 | Silberstahlrüstung | Silberstahlklinge | Silberstahlaxt | Silberstahlhammer | Silberstahlbogen | Silberstahlarmbrust | Silberstahlspeer | Silberstahlschild | Silberstahlwerkzeuge |
| 42 | Silberstahlrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 43 | Silberstahlrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 44 | Silberstahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 45 | Veredelte Silberstahlrüstung | Veredelte Silberstahlklinge | Veredelte Silberstahlaxt | Veredelter Silberstahlhammer | Veredelter Silberstahlbogen | Veredelte Silberstahlarmbrust | Veredelter Silberstahlspeer | Veredelter Silberstahlschild | Veredelte Werkzeuge |
| 46 | Dunkle Silberstahlrüstung | Dunkle Silberstahlklinge | Dunkle Silberstahlaxt | Dunkler Silberstahlhammer | Dunkler Silberstahlbogen | Dunkle Silberstahlarmbrust | Dunkler Silberstahlspeer | Dunkler Silberstahlschild | Dunkle Werkzeuge |
| 47 | Silberstahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 48 | Silberstahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 49 | Silberstahlrüstung des Meisters | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 50 | Meisterliche Silberstahlrüstung | Meisterliche Silberstahlklinge | Meisterliche Silberstahlaxt | Meisterlicher Silberstahlhammer | Meisterlicher Silberstahlbogen | Meisterliche Silberstahlarmbrust | Meisterlicher Silberstahlspeer | Meisterlicher Silberstahlschild | Meisterliche Werkzeuge |
| 51 | Goldstahlrüstung | Goldstahlklinge | Goldstahlaxt | Goldstahlhammer | Goldstahlbogen | Goldstahlarmbrust | Goldstahlspeer | Goldstahlschild | Goldstahlwerkzeuge |
| 52 | Goldstahlrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 53 | Goldstahlrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 54 | Goldstahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 55 | Veredelte Goldstahlrüstung | Veredelte Goldstahlklinge | Veredelte Goldstahlaxt | Veredelter Goldstahlhammer | Veredelter Goldstahlbogen | Veredelte Goldstahlarmbrust | Veredelter Goldstahlspeer | Veredelter Goldstahlschild | Veredelte Werkzeuge |
| 56 | Dunkle Goldstahlrüstung | Dunkle Goldstahlklinge | Dunkle Goldstahlaxt | Dunkler Goldstahlhammer | Dunkler Goldstahlbogen | Dunkle Goldstahlarmbrust | Dunkler Goldstahlspeer | Dunkler Goldstahlschild | Dunkle Werkzeuge |
| 57 | Goldstahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 58 | Goldstahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 59 | Goldstahlrüstung des Meisters | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 60 | Meisterliche Goldstahlrüstung | Meisterliche Goldstahlklinge | Meisterliche Goldstahlaxt | Meisterlicher Goldstahlhammer | Meisterlicher Goldstahlbogen | Meisterliche Goldstahlarmbrust | Meisterlicher Goldstahlspeer | Meisterlicher Goldstahlschild | Meisterliche Werkzeuge |
| 61 | Mithrilrüstung | Mithrilklinge | Mithrilaxt | Mithrilhammer | Mithrilbogen | Mithrilarmbrust | Mithrilspeer | Mithrilschild | Mithrilwerkzeuge |
| 62 | Mithrilrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 63 | Mithrilrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 64 | Mithrilrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 65 | Veredelte Mithrilrüstung | Veredelte Mithrilklinge | Veredelte Mithrilaxt | Veredelter Mithrilhammer | Veredelter Mithrilbogen | Veredelte Mithrilarmbrust | Veredelter Mithrilspeer | Veredelter Mithrilschild | Veredelte Werkzeuge |
| 66 | Dunkle Mithrilrüstung | Dunkle Mithrilklinge | Dunkle Mithrilaxt | Dunkler Mithrilhammer | Dunkler Mithrilbogen | Dunkle Mithrilarmbrust | Dunkler Mithrilspeer | Dunkler Mithrilschild | Dunkle Werkzeuge |
| 67 | Mithrilrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 68 | Mithrilrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 69 | Mithrilrüstung des Meisters | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 70 | Meisterliche Mithrilrüstung | Meisterliche Mithrilklinge | Meisterliche Mithrilaxt | Meisterlicher Mithrilhammer | Meisterlicher Mithrilbogen | Meisterliche Mithrilarmbrust | Meisterlicher Mithrilspeer | Meisterlicher Mithrilschild | Meisterliche Werkzeuge |
| 71 | Runenstahlrüstung | Runenstahlklinge | Runenstahlaxt | Runenstahlhammer | Runenstahlbogen | Runenstahlarmbrust | Runenstahlspeer | Runenstahlschild | Runenstahlwerkzeuge |
| 72 | Runenstahlrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 73 | Runenstahlrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 74 | Runenstahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 75 | Veredelte Runenstahlrüstung | Veredelte Runenstahlklinge | Veredelte Runenstahlaxt | Veredelter Runenstahlhammer | Veredelter Runenstahlbogen | Veredelte Runenstahlarmbrust | Veredelter Runenstahlspeer | Veredelter Runenstahlschild | Veredelte Werkzeuge |
| 76 | Dunkle Runenstahlrüstung | Dunkle Runenstahlklinge | Dunkle Runenstahlaxt | Dunkler Runenstahlhammer | Dunkler Runenstahlbogen | Dunkle Runenstahlarmbrust | Dunkler Runenstahlspeer | Dunkler Runenstahlschild | Dunkle Werkzeuge |
| 77 | Runenstahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 78 | Runenstahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 79 | Runenstahlrüstung des Meisters | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 80 | Meisterliche Runenstahlrüstung | Meisterliche Runenstahlklinge | Meisterliche Runenstahlaxt | Meisterlicher Runenstahlhammer | Meisterlicher Runenstahlbogen | Meisterliche Runenstahlarmbrust | Meisterlicher Runenstahlspeer | Meisterlicher Runenstahlschild | Meisterliche Werkzeuge |
| 81 | Drachenstahlrüstung | Drachenstahlklinge | Drachenstahlaxt | Drachenstahlhammer | Drachenstahlbogen | Drachenstahlarmbrust | Drachenstahlspeer | Drachenstahlschild | Drachenstahlwerkzeuge |
| 82 | Drachenstahlrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 83 | Drachenstahlrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 84 | Drachenstahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 85 | Veredelte Drachenstahlrüstung | Veredelte Drachenstahlklinge | Veredelte Drachenstahlaxt | Veredelter Drachenstahlhammer | Veredelter Drachenstahlbogen | Veredelte Drachenstahlarmbrust | Veredelter Drachenstahlspeer | Veredelter Drachenstahlschild | Veredelte Werkzeuge |
| 86 | Dunkle Drachenstahlrüstung | Dunkle Drachenstahlklinge | Dunkle Drachenstahlaxt | Dunkler Drachenstahlhammer | Dunkler Drachenstahlbogen | Dunkle Drachenstahlarmbrust | Dunkler Drachenstahlspeer | Dunkler Drachenstahlschild | Dunkle Werkzeuge |
| 87 | Drachenstahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 88 | Drachenstahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 89 | Drachenstahlrüstung des Meisters | Meisterklinge | Meisteraxt | Meisterhammer | Meisterbogen | Meisterarmbrust | Meisterspeer | Meisterschild | Meisterwerkzeuge |
| 90 | Meisterliche Drachenstahlrüstung | Meisterliche Drachenstahlklinge | Meisterliche Drachenstahlaxt | Meisterlicher Drachenstahlhammer | Meisterlicher Drachenstahlbogen | Meisterliche Drachenstahlarmbrust | Meisterlicher Drachenstahlspeer | Meisterlicher Drachenstahlschild | Meisterliche Werkzeuge |
| 91 | Ätherstahlrüstung | Ätherstahlklinge | Ätherstahlaxt | Ätherstahlhammer | Ätherstahlbogen | Ätherstahlarmbrust | Ätherstahlspeer | Ätherstahlschild | Ätherstahlwerkzeuge |
| 92 | Ätherstahlrüstung des Wächters | Wächterklinge | Wächteraxt | Wächterhammer | Wächterbogen | Wächterarmbrust | Wächterspeer | Wächterschild | Wächterwerkzeuge |
| 93 | Ätherstahlrüstung des Jägers | Jägerklinge | Jägeraxt | Jägerhammer | Jägerbogen | Jägerarmbrust | Jägerspeer | Jägerschild | Jägerwerkzeuge |
| 94 | Ätherstahlrüstung des Ritters | Ritterklinge | Ritteraxt | Ritterhammer | Ritterbogen | Ritterarmbrust | Ritterspeer | Ritterschild | Ritterwerkzeuge |
| 95 | Veredelte Ätherstahlrüstung | Veredelte Ätherstahlklinge | Veredelte Ätherstahlaxt | Veredelter Ätherstahlhammer | Veredelter Ätherstahlbogen | Veredelte Ätherstahlarmbrust | Veredelter Ätherstahlspeer | Veredelter Ätherstahlschild | Veredelte Werkzeuge |
| 96 | Dunkle Ätherstahlrüstung | Dunkle Ätherstahlklinge | Dunkle Ätherstahlaxt | Dunkler Ätherstahlhammer | Dunkler Ätherstahlbogen | Dunkle Ätherstahlarmbrust | Dunkler Ätherstahlspeer | Dunkler Ätherstahlschild | Dunkle Werkzeuge |
| 97 | Ätherstahlrüstung des Hauptmanns | Hauptmannsklinge | Hauptmannsaxt | Hauptmannshammer | Hauptmannsbogen | Hauptmannsarmbrust | Hauptmannsspeer | Hauptmannsschild | Hauptmannswerkzeuge |
| 98 | Ätherstahlrüstung des Champions | Championklinge | Championaxt | Championhammer | Championbogen | Championarmbrust | Championspeer | Championschild | Championwerkzeuge |
| 99 | Krone des letzten Champions | Letzte Klinge | Letzte Axt | Letzter Kriegshammer | Letzter Bogen | Letzte Armbrust | Letzter Speer | Schild des letzten Wächters | Letzte Spitzhacke / Letzte Axt / Letzte Schaufel / Letzte Hacke / Letzte Angel |

---

## Rüstung – Einzelteile

Jedes oben genannte Rüstungsset wird als eigene Custom-ID aufgeteilt:

```text
<set>_helmet
<set>_chestplate
<set>_leggings
<set>_boots
```

Damit entstehen 4 eigenständige Rüstungsteile je Set.

## Waffen

Mindestens folgende Waffenfamilien werden pro Level geführt:

- Sword
- Axe
- Mace / Hammer
- Bow
- Crossbow
- Spear

Die Waffen dürfen unterschiedliche Vanilla-Basismaterialien verwenden. Die Basis bestimmt nur die technische Vanilla-Funktion; die PixelRPG-ID bestimmt die Identität.

## Schilde

Jedes Level besitzt mindestens einen eigenen Schild.

```text
pixelrpg:<item_id>_shield
baseMaterial: SHIELD
```

## Werkzeuge

Jedes Level besitzt mindestens:

- Pickaxe
- Axe
- Shovel
- Hoe
- Fishing Rod

Die Werkzeug-Axt ist unabhängig von der Kampf-Axt zu behandeln und erhält eine eigene Custom-ID.

## Rarity

Rarity ist **nicht ausschließlich an das Level gekoppelt**. Innerhalb eines Levels können mehrere Varianten existieren:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Ein Level kann daher z. B. ein Common-Crafting-Item und zusätzlich einen Rare-Bossdrop besitzen.

## Crafting / Loot / Shops

Die Custom-ID ist überall dieselbe:

```text
Crafting Recipe
    ↓
Custom Item ID
    ↓
Item Definition
    ↓
Name + Lore + Stats + Rarity + Level + Soulbound
```

Boss-Loot, Quest-Rewards und Shops referenzieren ebenfalls die Custom-ID und erzeugen damit exakt dasselbe Item.

## Resourcepack-Ziel

Das Resourcepack ist optional. Das Plugin darf niemals davon abhängig sein.

Geplanter Pfad:

```text
Custom ID
    ↓
resourcePackKey
    ↓
assets/pixelrpg/...
```

Der Grafiker soll später anhand dieser IDs/Keys die Assets eindeutig zuordnen können.

## Grafik-Freigabe

Diese Datei ist zunächst der **Content-/Grafik-Blueprint**, nicht die endgültige Balance.

Der Grafiker soll insbesondere prüfen:

1. Welche Material-/Designfamilien sinnvoll sind.
2. Ob die 1–99 Progression visuell genug Unterschiede besitzt.
3. Welche Sets mehrere Varianten benötigen.
4. Welche Waffen eine eigene Silhouette benötigen.
5. Welche Schilde eigene Designs erhalten.
6. Welche Werkzeuge eigene Designs erhalten.
7. Welche Items für Boss-/Quest-/Unique-Drops zusätzlich individuelle Designs brauchen.

**Keine Item-ID darf später für ein anderes Item wiederverwendet werden.**

## Definition of Done

Das Custom-Item-System gilt erst als abgeschlossen, wenn:

- alle Level 1–99 abgedeckt sind
- alle oben definierten Slots abgedeckt sind
- jede Item-Definition eine stabile Custom-ID besitzt
- Vanilla-Basismaterial korrekt gesetzt ist
- Name/Lore/Stats/Rarity/Level/Soulbound definiert sind
- Crafting auf Custom-IDs referenziert
- Loot auf Custom-IDs referenziert
- Shops auf Custom-IDs referenzieren
- Quests/Bosses dieselben IDs verwenden können
- Items ohne Resourcepack korrekt funktionieren
- Resourcepack-Zuordnung später ohne Änderung am Gameplay ergänzt werden kann
- alle IDs eindeutig und dokumentiert sind
