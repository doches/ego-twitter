import re
import sys

with sys.stdin as stream:
    oldtext = stream.read()

    newtext = re.sub(r'(?<!\n)\n(?![\n\t])', ' ', oldtext.replace('\r', ''))

    print(newtext)