# RotateKasa

Paper 1.21.x icin anahtarla acilan, animasyonlu kasa eklentisi.

## Komutlar (yetki: `rotatekasa.admin`, varsayilan OP)
- `/kasakoy` : Baktigin yere shulker kasa koyar, envanterindeki itemleri icine doldurur. Kasadaki itemler odul havuzudur.
- `/kasaanahtarver <adet> [oyuncu]` : Kasa anahtari verir.
- `/setspawn` : Spawn noktasini bulundugun yer yapar (admin).
- `/spawn` : Herkes spawn alanina isinlanir. Her giriste ve yatagi olmayip olunce de orada dogarsin.
- `/rtp` : Dunyada rastgele guvenli bir yere isinlanir.
- `/artp` : Menu acar; Dunya, Nether veya End secip rastgele isinlanirsin. (Mesafe ve bekleme config.yml'den ayarlanir.)
- `/rtpnpcolustur` : Baktigin yere RTP NPC'si koyar, sag tiklayinca /artp menusu acilir (admin).
- `/rtpnpcsil` : Baktigin RTP NPC'sini siler (admin).
- `/evler` : 5 slotluk ev menusu. Bos slota tikla = ev kur, sol tik = isinlan, sag tik = evi tasi, shift + sag tik = sil.

## Kullanim
- Kasaya SOL tik -> icindeki oduller onizlenir (salt okunur).
- Kasaya anahtarla SAG tik -> animasyon -> rastgele odul. Anahtarsiz sag tik onizlemeyi acar.
- Kasanin ustunde 'Atlas Mc Kasalar' yazisi (TextDisplay) durur.
- Kasadaki itemler hic bitmez (odulun kopyasi verilir). Ayni itemi birden fazla slota koyarsan sansi artar.
- Admin: Shift + sag tik -> kasanin icini acip odulleri duzenle.
- Admin: Shift + kir -> kasayi kaldir.
- Kasa; patlama, piston ve hopper'a karsi korumalidir.

## Jar alma
GitHub'a yukle -> Actions sekmesi -> Build -> Artifacts -> RotateKasa.
