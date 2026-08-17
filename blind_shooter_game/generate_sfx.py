import wave
import math
import struct
import random
import os

OUTPUT_DIR = "app/src/main/res/raw"
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

def generate_wav(filename, duration, gen_func):
    path = os.path.join(OUTPUT_DIR, filename)
    with wave.open(path, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        framerate = 44100
        wav_file.setframerate(framerate)

        for i in range(int(framerate * duration)):
            t = i / float(framerate)
            sample = gen_func(t, duration)
            # Clamp
            sample = max(-32768, min(32767, int(sample)))
            wav_file.writeframes(struct.pack('<h', sample))
    print(f"Generated {path}")

# 1. Takeoff (rising pitch jet turbine)
def takeoff_gen(t, duration):
    # frequency rises from 100 to 400
    freq = 50 + (350 * (t / duration)**2)
    # add noise
    noise = random.uniform(-1.0, 1.0) * 0.3
    # tone
    tone = math.sin(2 * math.pi * freq * t)
    # volume envelope: fade in
    env = min(1.0, t / 1.0) * (1.0 - (t / duration)**4)
    return (tone * 0.7 + noise) * env * 20000

# 2. Flight Background (continuous rumbly hum)
def flight_bg_gen(t, duration):
    noise = random.uniform(-1.0, 1.0)
    # low pass filter approximation
    return noise * 5000 + math.sin(2 * math.pi * 120 * t) * 8000 + math.sin(2 * math.pi * 80 * t) * 8000

# 3. Enemy Flyby (doppler whoosh)
def flyby_gen(t, duration):
    # doppler effect: pitch drops rapidly in the middle
    center_t = duration / 2.0
    dist = t - center_t
    freq = 400 - math.atan(dist * 10) * 100
    noise = random.uniform(-1.0, 1.0) * 0.5
    tone = math.sin(2 * math.pi * freq * t)
    # amplitude bell curve
    amp = math.exp(-(dist**2) * 20)
    return (tone + noise) * amp * 30000

# 4. Gunshot (sharp burst of noise)
def gunshot_gen(t, duration):
    noise = random.uniform(-1.0, 1.0)
    # sharp exponential decay
    decay = math.exp(-t * 30)
    return noise * decay * 32000

# 5. Explosion (deep bass rumble and noise)
def explosion_gen(t, duration):
    noise = random.uniform(-1.0, 1.0)
    # low frequency rumble
    rumble = math.sin(2 * math.pi * (40 + random.uniform(-10, 10)) * t)
    decay = math.exp(-t * 2.5)
    return (noise * 0.6 + rumble * 0.8) * decay * 32700

generate_wav('takeoff.wav', 4.0, takeoff_gen)
generate_wav('flight_bg.wav', 2.0, flight_bg_gen) # Will be looped in Android
generate_wav('enemy_flyby.wav', 1.0, flyby_gen)
generate_wav('gunshot.wav', 0.3, gunshot_gen)
generate_wav('explosion.wav', 2.0, explosion_gen)
