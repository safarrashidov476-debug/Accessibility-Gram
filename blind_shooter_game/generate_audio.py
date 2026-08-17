import asyncio
import edge_tts
import os

VOICE = "uz-UZ-SardorNeural"

PROMPTS = {
    "instructions": "O'yin qoidalari. Chapda dushman desam o'ngga suring, o'ngda desam chapga suring, ro'parada desam ekranga bosing. O'yinni boshlash uchun ekranga bosing.",
    "start": "Samolyot parvozi boshlandi",
    "enemy_left": "Chapda dushman",
    "enemy_right": "O'ngda dushman",
    "enemy_center": "Roparada dushman",
    "shoot": "O'q uzildi",
    "destroyed": "Dushman yo'q qilindi",
    "dodge": "Aylanib o'tildi",
    "crash": "Avariya",
    "turn_left": "Chapga burilish",
    "turn_right": "O'ngga burilish",
}

OUTPUT_DIR = "app/src/main/res/raw"

async def generate_prompt(name, text):
    communicate = edge_tts.Communicate(text, VOICE)
    output_file = os.path.join(OUTPUT_DIR, f"{name}.mp3")
    await communicate.save(output_file)
    print(f"Generated {output_file}")

async def main():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    tasks = []
    for name, text in PROMPTS.items():
        tasks.append(generate_prompt(name, text))

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
