"""Import only the app's 24 dp, weight 400 Material Symbols Rounded vectors.

Google's China CDN is preferred; official global sources are fallbacks. No icon
font or runtime download is used. Existing resource names stay stable.
"""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import hashlib, json, urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
DRAWABLES = ROOT / 'shared/src/commonMain/composeResources/drawable'
ANDROID = 'http://schemas.android.com/apk/res/android'
SVG = '{http://www.w3.org/2000/svg}'
ET.register_namespace('android', ANDROID)
MIRRORED = {'arrow_back', 'arrow_forward', 'chevron_right', 'double_arrow', 'menu_open'}

def fetch(urls):
    errors = []
    for url in urls:
        try:
            with urllib.request.urlopen(url, timeout=20) as response:
                return response.read(), url
        except Exception as error:
            errors.append(type(error).__name__)
    raise RuntimeError('Official asset download failed: ' + ', '.join(errors))

def convert(name):
    symbol = name.removesuffix('_24px')
    filled = symbol.endswith('_filled')
    symbol = symbol.removesuffix('_filled')
    variant = 'fill1' if filled else 'default'
    file = symbol + ('_fill1' if filled else '') + '_24px.svg'
    data, url = fetch([
        f'https://fonts.gstatic.cn/s/i/short-term/release/materialsymbolsrounded/{symbol}/{variant}/24px.svg',
        f'https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/{symbol}/{variant}/24px.svg',
        f'https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/{symbol}/materialsymbolsrounded/{file}'
    ])
    svg = ET.fromstring(data)
    assert svg.tag == SVG + 'svg' and svg.attrib['viewBox'] == '0 -960 960 960', name
    paths = svg.findall(SVG + 'path')
    assert paths and len(list(svg)) == len(paths), name
    attributes = {f'{{{ANDROID}}}width': '24dp', f'{{{ANDROID}}}height': '24dp',
                  f'{{{ANDROID}}}viewportWidth': '960', f'{{{ANDROID}}}viewportHeight': '960'}
    if symbol in MIRRORED:
        attributes[f'{{{ANDROID}}}autoMirrored'] = 'true'
    vector = ET.Element('vector', attributes)
    group = ET.SubElement(vector, 'group', {f'{{{ANDROID}}}translateY': '960'})
    for path in paths:
        ET.SubElement(group, 'path', {f'{{{ANDROID}}}fillColor': '#FF000000', f'{{{ANDROID}}}pathData': path.attrib['d']})
    xml = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Material Symbols Rounded, Google; Apache-2.0. See tools/icons/NOTICE.md. -->\n'
    xml += ET.tostring(vector, encoding='unicode') + '\n'
    return name, xml, {'symbol': symbol, 'fill': int(filled), 'style': 'Rounded', 'weight': 400, 'optical_size': 24,
                       'source': url, 'svg_sha256': hashlib.sha256(data).hexdigest(), 'xml_sha256': hashlib.sha256(xml.encode()).hexdigest()}

def main():
    names = sorted({p.stem for p in DRAWABLES.glob('*_24px.xml')} | {'visibility_24px', 'visibility_off_24px'})
    # Prepare every vector before writing any, so partial downloads cannot mix styles.
    with ThreadPoolExecutor(max_workers=6) as executor:
        vectors = list(executor.map(convert, names))
    license_data, license_url = fetch(['https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE'])
    assert b'Apache License' in license_data and b'Version 2.0' in license_data
    for name, xml, _ in vectors:
        (DRAWABLES / (name + '.xml')).write_bytes(xml.encode('utf-8'))
    licenses = ROOT / 'shared/src/commonMain/composeResources/files/licenses'
    licenses.mkdir(exist_ok=True, parents=True)
    (licenses / 'Material-Symbols-Apache-2.0.txt').write_bytes(license_data)
    report = {'publisher': 'Google', 'license': 'Apache-2.0', 'license_source': license_url,
              'icons': {name: metadata for name, _, metadata in vectors}, 'xml_hash_normalization': 'UTF-8 LF', 'icon_font_bundled': False}
    (Path(__file__).parent / 'material-symbols.json').write_text(json.dumps(report, indent=2) + '\n', 'utf-8')
    print(json.dumps({'vectors': len(vectors), 'style': 'Rounded', 'all_weight_400': True, 'no_bundled_icon_font': True}))

if __name__ == '__main__':
    main()
