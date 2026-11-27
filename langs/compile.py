#!/usr/bin/python3

from os import listdir, makedirs, path
import json

def load_jsonc(f,lines=None):
    cleaned = ""
    for s in f:
        if lines != None:
            lines.append(s)
        if s.strip().startswith("//"):
            cleaned += "\n"
            continue
        if " // " in s:
            cleaned += s[:s.index(" // ")] + "\n"
        else:
            cleaned += s
    return json.loads(cleaned)

def escape(s: str):
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

en_lines = []
en_json = None
with open("en.jsonc", encoding="utf-8") as f:
    en_json = load_jsonc(f,en_lines)

print(len(en_json), "keys")

err=False
for s in en_json.keys():
    if " " in s:
        print("Invalid key:", s)
        err=True

if not err:
    with open("../src/LangConstants.java", "w") as f:
        f.write("// This is a generated file. Not intended for manual editing.\n")
        f.write("\n")
        f.write("public interface LangConstants {\n")
        i = 1
        for s in en_json.keys():
            f.write("\tint L" + s + " = " + str(i) + ";\n")
            i += 1
        f.write("\tint LLocaleStrings = " + str(len(en_json)) + ";\n")
        f.write("\tint Lmpgram = 0;\n")
        f.write("}\n")

    if not path.exists("../res/l/"):
        makedirs("../res/l")

    for n in listdir():
        if not n.endswith(".jsonc"):
            continue
        print(n)
        
        j = None
        lines = []
        with open(n, encoding="utf-8") as f:
            j = load_jsonc(f, lines)

        patch = "LocaleBase" in j

        with open("../res/l/" + n[:-6], mode='w', encoding="utf-8") as f:
            if patch:
                f.write("/l/")
                f.write(j["LocaleBase"])
                f.write("\n")

                i = 1
                for s in en_json.keys():
                    if s in j:
                        o = en_json[s]
                        t = j[s]
                        f.write(str(i))
                        f.write(":")
                        f.write(escape(t))
                        f.write("\n")
                    i = i + 1
            else:
                for s in en_json.keys():
                    if s in j:
                        if j[s] == None:
                            f.write(escape(en_json[s]))
                            #print("Missing key:", s)
                        else:
                            o = en_json[s]
                            t = j[s]
                            if n != "fa.jsonc" and n != "ar.jsonc" and n != "zh-CN.jsonc" and len(o.strip()) != 0 and len(t.strip()) != 0 and (o.strip().lower()[0] == o.strip()[0]) != (t.strip().lower()[0] == t.strip()[0]):
                                print('Warning: "{}": "{}"->"{}" does not match original case'.format(s, o, t))
                            f.write(escape(t))
                    else:
                        f.write(escape(en_json[s]))
                        print("Missing key:", s)
                    f.write("\n")
        
        if not patch and n != "en.jsonc":
            slavic = j["LocaleSlavicPlurals"] == "1"
            i = 0
            old_lines = lines
            lines = []
            for s in en_lines:
                t = s.strip()
                if len(t) == 0 or t == "{" or t == "}" or t.startswith("//"):
                    lines.append(s)
                    continue
                key = t.split(":")[0].split("\"")[1]
                if not key in j.keys():
                    lines.append(s[:-1] + " // Untranslated\n")
                else:
                    comment = s[s.rfind("\""):]
                    for p in old_lines:
                        if "\"" + key + "\": " in p:
                            comment = p[p.rfind("\""):]
                            break
                    if "Untranslated" in comment and (not key.endswith("_2") or not slavic):
                        lines.append(s[:-1] + " // Untranslated\n")
                    else:
                        lines.append(s[:s.index(":")] + ": \"" + escape(j[key]) + comment)
            
            with open(n, mode="w", encoding="utf-8") as f:
                for s in lines:
                    f.write(s)
        
        print()
