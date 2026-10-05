"""A DIGITÁLIS_KULTÚRA.docx táblázataiból újragenerálja az app/src/main/assets/tasks.json fájlt.

Használat:  pip install python-docx
            python tools/docx_to_tasks.py DIGITÁLIS_KULTÚRA.docx app/src/main/assets/tasks.json

A docx négy táblázatot vár ebben a sorrendben:
közép/digitális kultúra, közép/informatika, emelt/digitális kultúra, emelt/informatika.
A feladatok azonosítója (év + időszak + témakör + szint) állandó, ezért a már megtett
kijelölések új feladatok hozzáadása után is megmaradnak.
"""
import json
import re
import sys

from docx import Document
from docx.oxml.ns import qn

src, dst = sys.argv[1], sys.argv[2]
d = Document(src)
ws = lambda s: re.sub(r'\s+', ' ', s.replace('\xa0', ' ')).strip()
TOP = {'év': 'Év', 'szöveg': 'Szöveg', 'prezentáció': 'Prezentáció, grafika',
       'prezentáció, grafika': 'Prezentáció, grafika', 'weblap': 'Weblap',
       'táblázat': 'Táblázat', 'adatbázis': 'Adatbázis', 'programozás': 'Programozás'}
KEY = {'Szöveg': 'szoveg', 'Prezentáció, grafika': 'prez', 'Weblap': 'web',
       'Táblázat': 'tabl', 'Adatbázis': 'adat', 'Programozás': 'prog'}
MON = {'május': 'maj', 'október': 'okt', 'február': 'feb'}
CFG = [('kozep', 'digkult'), ('kozep', 'informatika'), ('emelt', 'digkult'), ('emelt', 'informatika')]

merged, seq = {}, []
for (level, era), t in zip(CFG, d.tables):
    heads = [TOP[ws(c.text).lower()] for c in t.rows[0].cells]
    seen = set()
    for r in t.rows[1:]:
        cells = r.cells
        m = re.search(r'(\d{4})\s*(.*)', ws(cells[0].text))
        year, sess = int(m.group(1)), m.group(2)
        idegen = 'idegen' in sess
        month = re.sub(r'\(idegen\)', '', sess).strip()
        for ci in range(1, len(cells)):
            c = cells[ci]
            if c._tc in seen:
                continue
            seen.add(c._tc)
            links = []
            for h in c._tc.iter(qn('w:hyperlink')):
                rid = h.get(qn('r:id'))
                txt = ws(''.join(x.text or '' for x in h.iter(qn('w:t'))))
                if rid in c.part.rels:
                    links.append((txt, c.part.rels[rid].target_ref))
            if not links:
                continue
            pdf = links[0][1]
            files = []
            for n, u in links[1:]:
                if n and (n, u) not in [(f['name'], f['url']) for f in files]:
                    files.append(dict(name=n, url=u))
            title = ws(c.text)
            for f in sorted(files, key=lambda f: -len(f['name'])):
                title = title.replace(f['name'], '')
            nosrc = 'nincsen' in title
            title = re.sub(r'\(?nincsen (forrás|nyers)\)?', '', title)
            title = ws(title.replace(';', '').replace('.zip', '').replace('.txt', '')) or links[0][0]
            k = (level, era, year, month, idegen, pdf)
            if k in merged:  # több témakört érintő, összevont cella
                if heads[ci] not in merged[k]['topics']:
                    merged[k]['topics'].append(heads[ci])
                continue
            tid = f"{level}_{era}_{year}_{MON.get(month, month)}{'_i' if idegen else ''}_{KEY[heads[ci]]}"
            task = dict(id=tid, level=level, era=era, year=year, month=month, idegen=idegen,
                        topics=[heads[ci]], title=title, pdf=pdf, files=files, nosrc=nosrc)
            merged[k] = task
            seq.append(task)

ids = [t['id'] for t in seq]
assert len(ids) == len(set(ids)), 'duplikált azonosító'
with open(dst, 'w', encoding='utf-8') as fh:
    json.dump(seq, fh, ensure_ascii=False, separators=(',', ':'))
print(len(seq), 'feladat kiírva ->', dst)
