import os
import telebot
import yt_dlp
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Get bot token from environment variables
BOT_TOKEN = os.environ.get('BOT_TOKEN')

# Initialize bot, will fail gracefully if no token provided in env when running
if BOT_TOKEN:
    bot = telebot.TeleBot(BOT_TOKEN)
else:
    # Dummy token for syntax checking / dry run without real token
    bot = telebot.TeleBot("123456789:DUMMY_TOKEN")

@bot.message_handler(commands=['start', 'help'])
def send_welcome(message):
    bot.reply_to(message, "Salom! Men Youtube dan video yuklab beruvchi botman. Menga Youtube videosining linkini (URL) yuboring.")

@bot.message_handler(func=lambda message: True)
def handle_message(message):
    url = message.text.strip()

    if "youtube.com" not in url and "youtu.be" not in url:
        bot.reply_to(message, "Iltimos, haqiqiy Youtube havolasini yuboring.")
        return

    msg = bot.reply_to(message, "Video yuklanmoqda... Iltimos kuting.")

    try:
        # Configuration for yt-dlp to download video and audio combined, targeting ~720p or lower to fit in Telegram limits (50MB usually for bots without local API server)
        ydl_opts = {
            'format': 'bestvideo[ext=mp4][filesize<40M]+bestaudio[ext=m4a]/best[ext=mp4][filesize<50M]/best[filesize<50M]',
            'outtmpl': '%(id)s.%(ext)s',
            'noplaylist': True,
        }

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info_dict = ydl.extract_info(url, download=True)
            video_filename = ydl.prepare_filename(info_dict)

        bot.edit_message_text("Video yuklab olindi. Telegramga yuborilmoqda...", chat_id=message.chat.id, message_id=msg.message_id)

        with open(video_filename, 'rb') as video_file:
            bot.send_video(message.chat.id, video_file, caption=info_dict.get('title', 'Youtube Video'), reply_to_message_id=message.message_id)

        # Clean up
        if os.path.exists(video_filename):
            os.remove(video_filename)

        bot.delete_message(chat_id=message.chat.id, message_id=msg.message_id)

    except Exception as e:
        logger.error(f"Error processing video: {e}")
        bot.edit_message_text(f"Xatolik yuz berdi yoki video hajmi juda katta.", chat_id=message.chat.id, message_id=msg.message_id)

if __name__ == '__main__':
    if not BOT_TOKEN:
        logger.error("BOT_TOKEN environment variable not set. Please set it before running the bot.")
    else:
        logger.info("Bot is polling...")
        bot.infinity_polling()
