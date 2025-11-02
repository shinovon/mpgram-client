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
    return s.replace("\\", "\\\\").replace("\n", "\\n")

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

        with open("../res/l/" + n[:-6], mode='w', encoding="utf-8") as f:
            for s in en_json.keys():
                if s in j:
                    if j[s] == None:
                        f.write(escape(en_json[s]))
                        #print("Missing key:", s)
                    else:
                        o = en_json[s]
                        t = j[s]
                        if n != "ar.jsonc" and len(o.strip()) != 0 and len(t.strip()) != 0 and (o.strip().lower()[0] == o.strip()[0]) != (t.strip().lower()[0] == t.strip()[0]):
                            print('Warning: "{}": "{}"->"{}" does not match original case'.format(s, o, t))
                        f.write(escape(t))
                else:
                    f.write(escape(en_json[s]))
                    print("Missing key:", s)
                f.write("\n")
        
        if len(lines) != len(en_lines):
            print("Filling")
            for k in en_json.keys():
                if k in j.keys():
                    continue
                i = 0
                for s in en_lines:
                    if s.strip().startswith("//") and s.strip() != lines[i].strip():
                        print(i, s.strip())
                        #if lines[i].strip().startswith("//"):
                        #    lines[i] = s
                        #else:
                        lines.insert(i, s)  
                    elif s.strip() == "" and lines[i].strip() != "":
                        lines.insert(i, s)
                    elif s.strip().split(":")[0] == "\"" + k + "\"":
                        print(i, s.strip())
                        if k[0] == '_' and k[-1] == '2':
                            lines.insert(i, s)
                        else:
                            lines.insert(i, s[:-1] + " // Untranslated\n")
                        break
                    # elif s.strip()[-1] == ',' and lines[i].strip()[-1] != ',':
                    #     if " // " in lines[i]:
                    #         d = lines[i].split(" // ")
                    #         if d[0][-1] == ',':
                    #             lines[i] = lines[i][:-1] + ",\n"
                    #     else:
                    #         lines[i] = lines[i][:-1] + ",\n"
                    i += 1
            
            with open(n, mode="w", encoding="utf-8") as f:
                for s in lines:
                    f.write(s)
        
        print()
