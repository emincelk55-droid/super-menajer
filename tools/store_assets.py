"""StoreAssetsTest çıktılarını mağaza adlarıyla kopyalar; ekran görüntülerinde alfa kanalını kaldırır."""
import glob
import os
import sys

from PIL import Image

src, dst = sys.argv[1], sys.argv[2]
os.makedirs(dst, exist_ok=True)
for f in glob.glob(os.path.join(src, "store_*.png")):
    name = os.path.basename(f)[len("store_"):]
    im = Image.open(f)
    if name.startswith("icon"):
        out = "ikon-512.png"
    elif name.startswith("feature"):
        out = "tanitim-1024x500.png"
        im = im.convert("RGB")
    else:
        out = "ekran-" + name
        im = im.convert("RGB")
    im.save(os.path.join(dst, out))
    print(out, im.size, im.mode)
