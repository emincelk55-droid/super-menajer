#!/bin/sh
# Play Store görsellerini StoreAssetsTest çıktısından docs/store'a kopyalar (ekran görüntüleri alfasız RGB).
set -e
cd "$(dirname "$0")/.."
gradle :app:testDebugUnitTest --tests '*StoreAssetsTest*' -q
python3 -I tools/store_assets.py app/build/store docs/store
