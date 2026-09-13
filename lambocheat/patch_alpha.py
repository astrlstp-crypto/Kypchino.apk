from pathlib import Path
import re

root = Path(__file__).resolve().parent
java = root / 'app/src/main/java/com/lambocheat/app/CheatOverlayService.java'
s = java.read_text(encoding='utf-8')

# Alpha identity / transport channel.
s = s.replace('private static final String CHANNEL = "lambocheats_b1";', 'private static final String CHANNEL = "lambocheats_alpha";')
s = s.replace('private static final int PORT = 48771;', 'private static final int PORT = 48772;')
s = s.replace('getSharedPreferences("lambocheats_b1", MODE_PRIVATE)', 'getSharedPreferences("lambocheats_alpha", MODE_PRIVATE)')
s = s.replace('PONGB1', 'PONGALPHA')
s = s.replace('LamboB1Health', 'LamboAlphaHealth').replace('LamboB1Command', 'LamboAlphaCommand')
s = s.replace('LamboCheats B1', 'LamboCheats [Alpha]')
s = s.replace('SchoolBoy runaway B1', 'SchoolBoy Runaway [Alpha]')
s = s.replace('Связь B1 с игрой есть', 'Связь Alpha с игрой есть')
s = s.replace('B1 выполняет прошлую команду', 'Alpha выполняет прошлую команду')
s = s.replace('B1 не знает эту команду', 'Alpha не знает эту команду')
s = s.replace('Ответ SchoolBoy B1:', 'Ответ SchoolBoy Alpha:')

items = '''    private final String[] ITEMS = {
            "Анти-жир", "Батарейка", "Гвозди", "Гвоздодёр", "Ёршик",
            "Заколка", "Зарядка", "Ключ", "Ключи от машины", "Лампочка",
            "Метла", "Молоток", "Мясо", "Отвёртка", "Снотворное",
            "Спрей от насекомых", "Стиральный порошок", "Ступенька", "Тряпка",
            "Цветок", "Швабра", "Шланг", "Фонарик"
    };
    private final String[] ITEM_CODES = {
            "ANTIFAT", "BATTERY", "NAILS", "NAILPULLER", "BRUSH",
            "BARRETTE", "CHARGER", "KEY", "CARKEY", "BULB",
            "BROOM", "HAMMER", "MEAT", "SCREWDRIVER", "SLEEPINGPILL",
            "INSECTSPRAY", "POWDER", "STEP", "RAG", "FLOWER", "MOP",
            "HOSE", "FLASHLIGHT"
    };'''
s, n = re.subn(r'    private final String\[\] ITEMS = \{.*?\n    \};', items, s, count=1, flags=re.S)
if n != 1:
    raise SystemExit('ITEMS block not found')

s = s.replace('sendCommand("ITEM:"+idx,"Выдано: "+ITEMS[idx],null)', 'sendCommand("ITEMCODE:"+ITEM_CODES[idx],"Выдано корректно: "+ITEMS[idx],null)')
s = s.replace('Проходить сквозь стены (с Fly)', 'Только стены/потолки; пол и земля остаются твёрдыми')
s = s.replace('section("ПРЕДМЕТЫ", "Нажми на предмет — он выдаётся через настоящий SpawnItems игры.");', 'section("ПРЕДМЕТЫ", "Alpha ищет предмет по настоящему имени внутри SpawnItems.items, а не по случайному номеру.");')
s = s.replace('section("MODES", "Вторая вкладка: режимы игрока и игровые действия.");', 'section("MODES", "Alpha: Fly и Noclip работают независимо. Noclip не отключает пол/землю.");')
s = s.replace('section("NPC • X / Y / Z", "Меняй размер отдельно по каждой оси. Диапазон 0.25–5.0.");', 'section("NPC • X / Y / Z", "Размер применяется к настоящим AI-компонентам и повторяется, если анимация его сбросила.");')
s = s.replace('section("TRANSPORT", "B1 вызывает реальные SpawnPrefab-компоненты, которые уже лежат в игре.");', 'section("TRANSPORT", "Alpha спавнит только совпавший настоящий SpawnPrefab. Случайный объект больше не подменяется.");')
s = s.replace('Если конкретный транспорт в этой сцене не загружен, B1 вернёт FAIL вместо фальшивого «успеха».', 'Если нужного prefab нет в сцене, Alpha покажет ERR:NO_TRANSPORT вместо фальшивого успеха.')

# Detailed bridge diagnostics so a broken function says what is missing.
needle = 'else if("BAD_CMD".equals(code)) result="Alpha не знает эту команду";'
replacement = '''else if("BAD_CMD".equals(code)) result="Alpha не знает эту команду";
                else if("ERR:NO_PLAYER".equals(code)) result="Alpha не нашёл контроллер игрока в этой сцене";
                else if("ERR:NO_ITEM".equals(code)) result="Такой предмет не найден в настоящем SpawnItems.items";
                else if("ERR:NO_NPC".equals(code)) result="NPC сейчас не загружен в сцене";
                else if("ERR:NO_WALLS".equals(code)) result="Контроллер найден, но коллайдеры стен/потолка ещё не найдены";
                else if("ERR:NO_TRANSPORT".equals(code)) result="Настоящий prefab этого транспорта в сцене не найден";
                else if("ERR:NO_OBJECT".equals(code)) result="Игровой объект этой функции не найден в текущей сцене";'''
if needle not in s:
    raise SystemExit('diagnostic insertion point not found')
s = s.replace(needle, replacement)
java.write_text(s, encoding='utf-8')

build = root / 'app/build.gradle'
b = build.read_text(encoding='utf-8')
b = re.sub(r"applicationId\s+'[^']+'", "applicationId 'com.lambocheats.alpha'", b, count=1)
b = re.sub(r'versionCode\s+\d+', 'versionCode 200', b, count=1)
b = re.sub(r"versionName\s+'[^']+'", "versionName 'Alpha'", b, count=1)
build.write_text(b, encoding='utf-8')

main = root / 'app/src/main/java/com/lambocheat/app/MainActivity.java'
m = main.read_text(encoding='utf-8')
m = m.replace('com.LamboCheats.SchoolBoyRunaway', 'com.LamboCheatA.SchoolBoyRunaway')
m = m.replace('LamboCheats B1', 'LamboCheats [Alpha]')
m = m.replace('SchoolBoy runaway B1', 'SchoolBoy Runaway [Alpha]')
m = m.replace('CHEAT B1', 'CHEAT [Alpha]')
main.write_text(m, encoding='utf-8')

manifest = root / 'app/src/main/AndroidManifest.xml'
x = manifest.read_text(encoding='utf-8')
x = x.replace('com.LamboCheats.SchoolBoyRunaway', 'com.LamboCheatA.SchoolBoyRunaway')
x = x.replace('android:label="LamboCheats B1"', 'android:label="LamboCheats [Alpha]"')
manifest.write_text(x, encoding='utf-8')

print('Applied LamboCheats [Alpha] patch: port 48772, exact item codes, Alpha diagnostics')
