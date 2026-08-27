# Pojav Controls import-editor v5

Paket ini mempertahankan runtime cursor stable v4 dan memperbaiki import profile Zalith 2 yang sebelumnya menumpuk di tengah. Editor properties memakai backdrop transparan, panel rounded, dan slider untuk opacity, corner radius, width, height, serta stroke width.

Import Zalith 2 memetakan normalButtons, joystickButtons, posisi percentage, ukuran percentage/dp, key events, dan visibility dasar. textBoxes, perpindahan layer, style visual launcher, serta event non-key tidak dipetakan satu per satu.

Ukuran percentage dikonversi ke dp memakai baseline 1920x1080 dan density 3, lalu dibatasi 16 sampai 400 dp. Posisi memakai placeholder runtime screen_width/screen_height sehingga tidak lagi jatuh ke titik tengah.

Paket ini tidak menyertakan synthetic touch, hotbar forwarding, atau dispatch touch ke Activity. Gunakan paket ini sebagai baseline stabil.
