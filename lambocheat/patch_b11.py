from pathlib import Path
import re

root = Path(__file__).resolve().parent
java = root / "app/src/main/java/com/lambocheat/app/CheatOverlayService.java"
s = java.read_text(encoding="utf-8")

items = '''    private final String[] ITEMS = {
            "Анти-жир", "Батарейка", "Гвозди", "Гвоздодёр", "Ёршик",
            "Заколка", "Зарядка", "Ключ", "Ключи от машины", "Лампочка",
            "Метла", "Молоток", "Мясо", "Отвёртка", "Снотворное",
            "Спрей от насекомых", "Стиральный порошок", "Ступенька", "Тряпка",
            "Цветок", "Швабра", "Шланг", "Фонарик"
    };'''
s, n = re.subn(r"    private final String\[\] ITEMS = \{.*?\n    \};", items, s, count=1, flags=re.S)
if n != 1:
    raise SystemExit("ITEMS block not found")

s = s.replace("Проходить сквозь стены (с Fly)", "Проходить сквозь стены независимо от Fly")
s = s.replace(
    'section("TRANSPORT", "B1 вызывает реальные SpawnPrefab-компоненты, которые уже лежат в игре.");',
    'section("TRANSPORT", "B1.1 сначала ищет настоящий SpawnPrefab, затем реальный объект транспорта в загруженной сцене.");'
)
s = s.replace(
    'content.addView(transportButton("🛴 Самокат","SPAWN:SCOOTER"),lp(dp(58)));',
    'content.addView(transportButton("🛴 Самокат","SPAWN:SCOOTER"),lp(dp(58)));\n'
    '        content.addView(transportButton("🎲 Любой игровой SpawnPrefab","SPAWN:ANY"),lp(dp(58)));'
)
s = s.replace(
    'TextView note=label("Если конкретный транспорт в этой сцене не загружен, B1 вернёт FAIL вместо фальшивого «успеха».",11,0xFF9AA5B8);',
    'TextView note=label("Для конкретного транспорта B1.1 не подменяет его случайным объектом: если модели в сцене нет, будет FAIL.",11,0xFF9AA5B8);'
)
java.write_text(s, encoding="utf-8")

build = root / "app/build.gradle"
b = build.read_text(encoding="utf-8")
b = re.sub(r"versionCode\s+\d+", "versionCode 102", b, count=1)
b = re.sub(r"versionName\s+'[^']+'", "versionName 'B1.1'", b, count=1)
build.write_text(b, encoding="utf-8")

main = root / "app/src/main/java/com/lambocheat/app/MainActivity.java"
m = main.read_text(encoding="utf-8")
m = m.replace("LamboCheats B1", "LamboCheats B1.1")
m = m.replace("SchoolBoy runaway B1 • real in-game overlay", "SchoolBoy runaway B1.1 • corrected items + modes")
main.write_text(m, encoding="utf-8")

manifest = root / "app/src/main/AndroidManifest.xml"
x = manifest.read_text(encoding="utf-8").replace('android:label="LamboCheats B1"', 'android:label="LamboCheats B1.1"')
manifest.write_text(x, encoding="utf-8")

print("Applied LamboCheats B1.1 UI/item patch")
