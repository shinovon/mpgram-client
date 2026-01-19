# MPGram

Telegram client for J2ME platform based on [MPGram Web API](https://github.com/shinovon/mpgram-web), which is based on [MadelineProto](https://github.com/danog/MadelineProto)

![img](/img/chat.jpg) ![img](/img/image_viewer.jpg) ![img](/img/music_player.jpg)

![img](/img/notification.jpg) ![img](/img/chat_info.jpg) ![img](/img/bot.jpg)

## Features

- Chat with automatic updates
- Music player with playlists
- Bot interactions
- Notifications (Pigler API, NokiaUI)
- Image viewer
- Basic administration (message deletion, member banning)
- Forum chats
- Folders
- File downloads
- Sticker sending
- File uploads
- Message search
- Contact list

## Device support

### Requirements

- MIDP 2.0
- Heap memory: at least 1 MB
- Screen size: at least 176x220 (for smaller screens use legacy UI)

Does not work on most Samsung phones.

### Platform-specific versions

- If you want to hide virtual keyboard on Samsung or LG touchscreen phones, use Samsung version.
- If installation fails with verification error, use BlackBerry version.
- If your device does not support CLDC 1.1 or JSR-75, use Lite version.

### Lite version limitations

- Legacy UI only
- No file uploads
- No notifications
- No chat or profile pictures
- No data compression
- No localization

## License notice

This project's license is messed up, as it contains modified parts of TUBE42 imagelib (LGPL) and jazzlib (GPLv2 or later).

If you want to fork this project under the MIT license, you have to exclude jazzlib (src/zip) from it.
