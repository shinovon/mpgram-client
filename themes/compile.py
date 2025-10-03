#!/usr/bin/python3

from os import listdir, makedirs, path
import json

enum_colors = {
    "CHAT_BG": 0,
    "CHAT_FG": 1,
    "CHAT_HIGHLIGHT_BG": 2,
    "CHAT_PANEL_BG": 3,
    "CHAT_PANEL_FG": 4,
    "CHAT_PANEL_BORDER": 5,
    "CHAT_MENU_BG": 6,
    "CHAT_MENU_HIGHLIGHT_BG": 7,
    "CHAT_MENU_FG": 8,
    "CHAT_STATUS_FG": 9,
    "CHAT_STATUS_HIGHLIGHT_FG": 10,
    "CHAT_POINTER_HOLD": 11,
    "CHAT_INPUT_ICON": 12,
    "CHAT_SEND_ICON": 13,
    "CHAT_INPUT_BORDER": 14,
    
    "MESSAGE_BG": 20,
    "MESSAGE_OUT_BG": 21,
    "MESSAGE_FG": 22,
    "MESSAGE_LINK": 23,
    "MESSAGE_LINK_FOCUS": 24,
    "MESSAGE_SENDER": 25,
    "MESSAGE_ATTACHMENT_BORDER": 26,
    "MESSAGE_ATTACHMENT_TITLE": 27,
    "MESSAGE_ATTACHMENT_SUBTITLE": 28,
    "MESSAGE_ATTACHMENT_FOCUS_BG": 29,
    "MESSAGE_COMMENT_BORDER": 30,
    "MESSAGE_IMAGE": 31,
    "MESSAGE_FOCUS_BORDER": 32,
    "MESSAGE_TIME": 33,
    "MESSAGE_OUT_TIME": 34,
    "ACTION_BG": 35,
    "MESSAGE_OUT_READ": 36
}

enum_style = {
    "MESSAGE_FILL": 0,
    "MESSAGE_ROUND": 1,
    "MESSAGE_BORDER": 2
}

if not path.exists("../res/c/"):
    makedirs("../res/c")

for n in listdir():
    if not n.endswith(".json"):
        continue
    print(n)
    
    j = None
    with open(n, encoding="utf-8") as f:
        j = json.load(f)
    
    theme = [0]*40
    style = [0]*20
    name = j["name"]
    
    for key in enum_colors.keys():
        if not key in j:
            print("Missing key:", key);
            continue
        theme[enum_colors[key]] = int(j[key], 16);
    
    for key in enum_style.keys():
        if not key in j:
            print("Missing key:", key);
            continue
        style[enum_style[key]] = 1 if j[key] else 0;
    
    with open("../res/c/" + n[:-5], mode='wb') as f:
        name = bytes(name, "utf-8")
        f.write((len(name)).to_bytes(2, byteorder='big', signed=False))
        f.write(name)
        for i in theme:
            f.write((i).to_bytes(4, byteorder='big', signed=False))
        for i in style:
            f.write((i).to_bytes(1, byteorder='big', signed=False))
    
    print()