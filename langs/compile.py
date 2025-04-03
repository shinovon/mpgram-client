from os import listdir
import json

desc=[]
with open("en.json", encoding="utf-8") as f:
    desc = json.load(f).keys()

with open("../src/LangConstants.java", "w") as f:
    f.write("/* Auto-generated locale constants */\n")
    f.write("\n")
    f.write("public interface LangConstants {\n")
    i = 1
    for s in desc:
        f.write("\tstatic final int " + s + " = " + str(i) + ";\n")
        i = i + 1
    f.write("\tstatic final int mpgram = 0;\n")
    f.write("}\n")

for n in listdir():
    if not n.endswith(".json"):
        continue
    print(n)
    
    j = None
    with open(n, encoding="utf-8") as f:
        j = json.load(f)

    with open("../res/l/" + n[:-5], mode='w', encoding="utf-8") as f:
        for s in desc:
            if s in j:
                f.write(j[s].replace("\n", "\\\n"))
            else:
                print(s, "missing")
            f.write("\n")
