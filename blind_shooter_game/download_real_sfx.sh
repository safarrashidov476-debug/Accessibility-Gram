#!/bin/bash
OUTPUT_DIR="app/src/main/res/raw"
mkdir -p $OUTPUT_DIR

# Actual game audio repos
curl -L -o $OUTPUT_DIR/gunshot.wav "https://raw.githubusercontent.com/libgdx/libgdx/master/tests/gdx-tests-android/assets/data/shotgun.wav"
curl -L -o $OUTPUT_DIR/enemy_flyby.wav "https://raw.githubusercontent.com/libgdx/libgdx/master/tests/gdx-tests-android/assets/data/quadraphonic.wav"
# Use wind/hum for background
curl -L -o $OUTPUT_DIR/flight_bg.wav "https://raw.githubusercontent.com/libgdx/libgdx/master/tests/gdx-tests-android/assets/data/8.12.loop.wav"
curl -L -o $OUTPUT_DIR/takeoff.wav "https://raw.githubusercontent.com/libgdx/libgdx/master/tests/gdx-tests-android/assets/data/chirp.wav"
curl -L -o $OUTPUT_DIR/explosion.wav "https://raw.githubusercontent.com/libgdx/libgdx/master/tests/gdx-tests-android/assets/data/sell_buy_item.wav"
