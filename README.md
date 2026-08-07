# 家計簿（Kakeibo）

端末内に収入・支出を保存するシンプルなAndroid家計簿アプリです。

## 主な機能

- 収入・支出の登録、編集、削除
- カテゴリ、日付、メモ
- 月別の履歴と収支集計
- 月の支出予算と残額表示
- カテゴリの追加、名前変更、表示・非表示

## 開発環境

- Kotlin
- Jetpack Compose / Material 3
- Room
- Gradle Wrapper 8.13
- compileSdk 36 / minSdk 26
- applicationId `com.qkkk8402.kakeibo`

## ビルド・テスト

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
```

端末またはAVDを起動した状態では、Compose UIテストも実行できます。

```bash
./gradlew connectedDebugAndroidTest
```

データはアプリ専用のRoomデータベース（`kakeibo.db`）に保存され、ネットワーク通信は行いません。

## GitHub ReleaseへのAPK自動公開

`.github/workflows/release-apk.yml` により、`v1.0.0` のようなタグを `main` にpushすると、テスト・lint・署名付きrelease APKのビルド後、GitHub ReleaseへAPKが自動添付されます。

タグの`vMAJOR.MINOR.PATCH`（プレリリース suffixなし）からAndroidの`versionCode`を決定的に生成します（major 1999以下、minor/patch 999以下）。そのため、リリースごとにタグのバージョンを上げてください。

公開例:

```bash
git tag v0.1.0
git push origin v0.1.0
```

## 入力検証

- 金額は1〜9,999,999,999円。空欄、0円、上限超過は保存不可
- 金額欄へ貼り付けた数字以外の文字（負号、小数点、`¥`、カンマ、文字など）は入力エラー
- メモは前後空白を除去し、空白だけなら未入力として保存。200文字を超える入力は不可
- カテゴリ名は前後空白を除去し、1〜20文字。同じ種別での重複名は不可

## 軽量AVDでのUIテスト

この環境では、API 34 Google APIs x86_64の`kakeibo_api34`（Pixel 2）を使用できます。AVDの保存先が`~/.config/.android/avd`の場合は、次のように起動します。

```bash
ANDROID_AVD_HOME="$HOME/.config/.android/avd" \
  "$ANDROID_HOME/emulator/emulator" \
  -avd kakeibo_api34 -no-window -no-audio -no-boot-anim \
  -gpu swiftshader_indirect -memory 1024 -cores 2

./gradlew connectedDebugAndroidTest
```
